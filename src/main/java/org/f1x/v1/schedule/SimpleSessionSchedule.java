/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.f1x.v1.schedule;

import org.f1x.util.AsciiUtils;
import org.f1x.util.parse.TimeOfDayParser;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Simple implementation of SessionSchedule that can define daily or weekly schedule during with specific start/end time of day.
 *
 * This implementation is NOT thread-safe.
 */
public class SimpleSessionSchedule implements SessionSchedule {
    protected final TimeEndPoint start;
    protected final TimeEndPoint end;
    private final boolean isDailySchedule;
    private final boolean isEndTimeBeforeStartTimeOfDay;

    /**
     * @param startDayOfWeek Session Start day of week (can be <code>-1</code> for sessions that happen every day). Week starts with sunday and has code 1. For example, <code>Calendar.MONDAY</code>.
     * @param endDayOfWeek  Session End day of week (must be <code>-1</code> if startDayOfWeek is <code>-1</code>). Week starts with sunday and has code 1. For example, <code>Calendar.FRIDAY</code>.
     * @param startTimeOfDay session start time of day (in HH:MM:SS format)
     * @param endTimeOfDay session end time of day (in HH:MM:SS format)
     * @param isDailySchedule <code>true</code> for daily FIX sessions, <code>false</code> for multi-day sessions. Makes sense only when startDayOfWeek and endDayOfWeek are set.
     * @param tz optional time zone (otherwise local TZ is assumed)
     */
    public SimpleSessionSchedule (int startDayOfWeek, int endDayOfWeek, String startTimeOfDay, String endTimeOfDay, boolean isDailySchedule, TimeZone tz) {
        if (startDayOfWeek != -1 && endDayOfWeek == -1 || startDayOfWeek == -1 && endDayOfWeek != -1)
            throw new IllegalArgumentException("Start and End day of weeks must be specified together");

        if ( ! isDailySchedule && startDayOfWeek == -1)
            throw new IllegalArgumentException("Weekly schedule must have Start and End day of week defined");

        if (tz == null)
            tz = TimeZone.getDefault();
        this.start = new TimeEndPoint(startDayOfWeek, startTimeOfDay, tz);
        this.end = new TimeEndPoint(endDayOfWeek, endTimeOfDay, tz);
        this.isEndTimeBeforeStartTimeOfDay = start.calcTimeInSeconds() > end.calcTimeInSeconds();
        this.isDailySchedule = isDailySchedule;
    }

    @Override
    public SessionTimes getCurrentSessionTimes(long currentTime) {

        setMostRecentSessionBefore(currentTime);

        if (currentTime < end.getTimeInMillis()) {
            return new SessionTimes(start.getTimeInMillis(), end.getTimeInMillis());
        } else {
            // Session is over
            setNextSessionAfter(currentTime);
        }
        return new SessionTimes(start.getTimeInMillis(), end.getTimeInMillis());
    }

    /** Sets start/end times of current session if it includes given timestamp or the most recent session if given timestamp is outside of this schedule */
    protected void setMostRecentSessionBefore(long timestamp) { //assert Thread.holdsLock(this); // using {start, end}
        start.setAndAdjust(timestamp);
        if (start.dayOfWeek != -1) {
            start.calendar.set(Calendar.DAY_OF_WEEK, start.dayOfWeek);
            if (start.after(timestamp))
                start.calendar.add(Calendar.WEEK_OF_YEAR, -1);

            if (isDailySchedule) {
                int correctedDayOfWeek = (isEndTimeBeforeStartTimeOfDay) ? end.dayOfWeek - 1 : end.dayOfWeek;
                while (start.notAfter(timestamp) && start.getDayOfWeek () < correctedDayOfWeek) {
                    start.calendar.add(Calendar.DAY_OF_YEAR, 1);
                }
                if (start.after(timestamp))
                    start.calendar.add(Calendar.DAY_OF_YEAR, -1);
            }
        } else if (start.after(timestamp)) {
            start.calendar.add(Calendar.DAY_OF_YEAR, -1);
        }

        adjustIntervalEnd();
    }

    /** Sets start/end times to the next session (called ensures that given timestamp is outside of this schedule) */
    protected void setNextSessionAfter(long timestamp) { //assert Thread.holdsLock(this); // using {start, end}
        start.setAndAdjust(timestamp);

        if (start.dayOfWeek != -1) {
            if (isDailySchedule) {
                if (start.before(timestamp))
                    start.calendar.add(Calendar.DAY_OF_YEAR, 1);
                int correctedDayOfWeek = (isEndTimeBeforeStartTimeOfDay) ? end.dayOfWeek - 1 : end.dayOfWeek;
                if (start.getDayOfWeek() > correctedDayOfWeek) {
                    start.calendar.set(Calendar.DAY_OF_WEEK, start.dayOfWeek);
                    start.calendar.add(Calendar.WEEK_OF_YEAR, 1);
                }

            } else {
                start.calendar.set(Calendar.DAY_OF_WEEK, start.dayOfWeek);
                if (start.before(timestamp))
                    start.calendar.add(Calendar.WEEK_OF_YEAR, 1);
            }

        } else if (start.before(timestamp)) {
            start.calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        adjustIntervalEnd();
    }

    /** Adjust End of interval based on Start point */
    private void adjustIntervalEnd() {
        end.setAndAdjust(start.getTimeInMillis());
        if (end.dayOfWeek != -1) {

            if (isDailySchedule) {
                if (end.notAfter(start))
                    end.calendar.add(Calendar.DAY_OF_WEEK, 1);
            } else {
                end.calendar.set(Calendar.DAY_OF_WEEK, end.dayOfWeek);
                if (end.notAfter(start)) {
                    end.calendar.add(Calendar.WEEK_OF_MONTH, 1);
                }
            }
        } else if (end.notAfter(start)) {
            end.calendar.add(Calendar.DAY_OF_WEEK, 1);
        }
    }

//    protected void setNextSessionAfterOld(long timestamp) { assert Thread.holdsLock(this); // using {start, end}
//        end.setAndAdjust(timestamp);
//        if (end.dayOfWeek != -1) {
//            end.calendar.set(Calendar.DAY_OF_WEEK, end.dayOfWeek);
//            if (end.before(timestamp))
//                end.calendar.add(Calendar.WEEK_OF_YEAR, 1);
//
//            if (isDailySchedule) {
//                //int correctedDayOfWeek = (isEndTimeBeforeStartTimeOfDay) ? start.dayOfWeek + 1 : start.dayOfWeek; //TODO
//                //while (end.getTimeInMillis() >= timestamp && end.getDayOfWeek () < correctedDayOfWeek) {
//                while (end.notBefore(timestamp) && end.getDayOfWeek () < end.dayOfWeek) {
//                    end.calendar.add(Calendar.DAY_OF_YEAR, -1);
//                }
//                if (end.before(timestamp))
//                    end.calendar.add(Calendar.DAY_OF_YEAR, 1);
//            }
//        } else if (end.before(timestamp)) {
//            end.calendar.add(Calendar.DAY_OF_YEAR, 1);
//        }
//
//        start.setAndAdjust(end.getTimeInMillis());
//        if (end.dayOfWeek != -1) {
//
//            if (isDailySchedule) {
//                if (start.notBefore(end))
//                    start.calendar.add(Calendar.DAY_OF_WEEK, -1);
//            } else {
//                start.calendar.set(Calendar.DAY_OF_WEEK, start.dayOfWeek);
//                if (start.notBefore(end)) {
//                    start.calendar.add(Calendar.WEEK_OF_MONTH, -1);
//                }
//            }
//        } else if (start.notBefore(end)) {
//            start.calendar.add(Calendar.DAY_OF_WEEK, -1);
//        }
//    }


    @Override
    public String toString() {
        return start + " " + end + ((isDailySchedule)?" Daily":" Weekly") ;
    }

    protected static class TimeEndPoint {
        private final Calendar calendar;
        private final int dayOfWeek;
        private final int hour;
        private final int minute;
        private final int second;

        private TimeEndPoint(int dayOfWeek, String timeOfDay, TimeZone tz) {
            this.calendar = Calendar.getInstance(tz);
            this.dayOfWeek = dayOfWeek;

            int [] parsed = TimeOfDayParser.parseTimeOfDay(AsciiUtils.getBytes(timeOfDay));
            this.hour = parsed[0];
            this.minute = parsed[1];
            this.second = parsed[2];
        }

        private int calcTimeInSeconds () {
            return (hour * 3600) + (minute * 60) + second;
        }

        private void setAndAdjust(long timestamp) {
            calendar.setTimeInMillis(timestamp);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, second);
            calendar.set(Calendar.MILLISECOND, 0);
        }

        protected long getTimeInMillis() {
            return calendar.getTimeInMillis();
        }

        private boolean before(long timestamp) {
            return getTimeInMillis() < timestamp;
        }

        private boolean after(long timestamp) {
            return getTimeInMillis() > timestamp;
        }

        private boolean notAfter(long timestamp) {
            return getTimeInMillis() <= timestamp;
        }

        private boolean notBefore(long timestamp) {
            return getTimeInMillis() >= timestamp;
        }

        private boolean before(TimeEndPoint timestamp) {
            return before(timestamp.getTimeInMillis());
        }

        private boolean after(TimeEndPoint timestamp) {
            return after(timestamp.getTimeInMillis());
        }

        private boolean notAfter(TimeEndPoint timestamp) {
            return notAfter(timestamp.getTimeInMillis());
        }

        private boolean notBefore(TimeEndPoint timestamp) {
            return notBefore(timestamp.getTimeInMillis());
        }

        private int getDayOfWeek () {
            return calendar.get(Calendar.DAY_OF_WEEK);
        }

        @Override
        public String toString() {
            return String.format("%02d:%02d:%02d (%d)", hour, minute, second, dayOfWeek);
        }

    }
}
