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

import org.f1x.util.StoredTimeSource;
import org.f1x.util.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

public class Test_SimpleSessionSchedule {

    private static final String LONG_TIME_AGO = "20101212-00:00:00.000"; // some random time in the past (exact moment is not important)
    private StoredTimeSource timeSource = new StoredTimeSource() {
        {
            setLocalTime(LONG_TIME_AGO);
        }
    };

    private SimpleSessionSchedule schedule;


    @Test(expected = IllegalArgumentException.class)
    public void testEveryDayMostRecentSessionBefore_Weekly () {
        makeSchedule(-1, -1, "11:00:00", "22:00:00", false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOneSidedWeekdayStart () {
        makeSchedule(Calendar.MONDAY, -1, "11:00:00", "22:00:00", true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOneSidedWeekdayEnd () {
        makeSchedule(-1, Calendar.FRIDAY, "11:00:00", "22:00:00", true);
    }    

    /** Test "every day same hours" schedule */
    @Test
    public void testEveryDayMostRecentSessionBefore () {
        schedule = makeSchedule(-1, -1, "09:00:00", "23:00:00", true);

        assertMostRecentSession ("20140121-08:59:00.000", "20140120-09:00:00.000", "20140120-23:00:00.000"); // shortly before session start
        assertMostRecentSession ("20140121-09:01:00.000", "20140121-09:00:00.000", "20140121-23:00:00.000"); // shortly after session start
        assertMostRecentSession ("20140121-22:59:00.000", "20140121-09:00:00.000", "20140121-23:00:00.000"); // shortly before session close
        assertMostRecentSession ("20140121-23:01:00.000", "20140121-09:00:00.000", "20140121-23:00:00.000"); // shortly after session close

        assertMostRecentSession ("20140121-00:00:00.000", "20140120-09:00:00.000", "20140120-23:00:00.000"); // midnight
        assertMostRecentSession ("20140121-12:00:00.000", "20140121-09:00:00.000", "20140121-23:00:00.000"); // noon
    }

    /** Test "every day same hours" schedule where start time is later than end time */
    @Test
    public void testEveryDayMostRecentSessionBefore_Inverted () {
        schedule = makeSchedule(-1, -1, "23:00:00", "22:00:00", true);  // starts 23:00 and continues until 22:00 next day

        assertMostRecentSession ("20140121-22:59:00.000", "20140120-23:00:00.000", "20140121-22:00:00.000"); // shortly before session start
        assertMostRecentSession ("20140121-23:01:00.000", "20140121-23:00:00.000", "20140122-22:00:00.000"); // shortly after session start
        assertMostRecentSession ("20140122-21:59:00.000", "20140121-23:00:00.000", "20140122-22:00:00.000"); // shortly before session close
        assertMostRecentSession ("20140122-22:01:00.000", "20140121-23:00:00.000", "20140122-22:00:00.000"); // shortly after session close

        assertMostRecentSession ("20140121-00:00:00.000", "20140120-23:00:00.000", "20140121-22:00:00.000"); // midnight
        assertMostRecentSession ("20140121-12:00:00.000", "20140120-23:00:00.000", "20140121-22:00:00.000"); // noon
    }

    /** Test "Monday to Friday hours" schedule */
    @Test
    public void testMondayFridayMostRecentSessionBefore () {
        schedule = makeSchedule(Calendar.MONDAY, Calendar.FRIDAY, "09:00:00", "23:00:00", true);

        assertMostRecentSession ("20140121-08:59:00.000", "20140120-09:00:00.000", "20140120-23:00:00.000"); // shortly before session start
        assertMostRecentSession ("20140121-09:01:00.000", "20140121-09:00:00.000", "20140121-23:00:00.000"); // shortly after session start
        assertMostRecentSession ("20140121-22:59:00.000", "20140121-09:00:00.000", "20140121-23:00:00.000"); // shortly before session close
        assertMostRecentSession ("20140121-23:01:00.000", "20140121-09:00:00.000", "20140121-23:00:00.000"); // shortly after session close

        assertMostRecentSession ("20140121-00:00:00.000", "20140120-09:00:00.000", "20140120-23:00:00.000"); // midnight
        assertMostRecentSession ("20140121-12:00:00.000", "20140121-09:00:00.000", "20140121-23:00:00.000"); // noon

        // Special cases:
        assertMostRecentSession ("20140120-08:59:00.000", "20140117-09:00:00.000", "20140117-23:00:00.000"); // shortly before session open on Monday get us Friday session
    }
        
    /** Test "Monday to Friday hours" schedule where start time is later than end time */
    @Test
    public void testMondayFridayMostRecentSessionBefore_Inverted () {
        schedule = makeSchedule(Calendar.MONDAY, Calendar.FRIDAY, "23:00:00", "22:00:00", true);  // starts 23:00 and continues until 22:00 next day (THIS IS ACTUALLY 4 DAYS PER WEEK, NOT 5!)

        assertMostRecentSession ("20140121-22:59:00.000", "20140120-23:00:00.000", "20140121-22:00:00.000"); // shortly before session start
        assertMostRecentSession ("20140121-23:01:00.000", "20140121-23:00:00.000", "20140122-22:00:00.000"); // shortly after session start
        assertMostRecentSession ("20140122-21:59:00.000", "20140121-23:00:00.000", "20140122-22:00:00.000"); // shortly before session close
        assertMostRecentSession ("20140122-22:01:00.000", "20140121-23:00:00.000", "20140122-22:00:00.000"); // shortly after session close

        assertMostRecentSession ("20140121-00:00:00.000", "20140120-23:00:00.000", "20140121-22:00:00.000"); // midnight
        assertMostRecentSession ("20140121-12:00:00.000", "20140120-23:00:00.000", "20140121-22:00:00.000"); // noon

        // Special cases:
        assertMostRecentSession ("20140120-21:59:00.000", "20140116-23:00:00.000", "20140117-22:00:00.000"); // shortly before session open on Monday returns Thursday-Friday session
        assertMostRecentSession ("20140121-21:59:00.000", "20140120-23:00:00.000", "20140121-22:00:00.000"); // shortly before session open on Tuesday returns Monday-Tuesday session
        assertMostRecentSession ("20140122-21:59:00.000", "20140121-23:00:00.000", "20140122-22:00:00.000"); // shortly before session open on Wed returns Tues-Wed session
        assertMostRecentSession ("20140123-21:59:00.000", "20140122-23:00:00.000", "20140123-22:00:00.000"); // shortly before session open on Thurs returns Wed-Thurs session
        assertMostRecentSession ("20140124-21:59:00.000", "20140123-23:00:00.000", "20140124-22:00:00.000"); // shortly before session open on Friday returns Thurs-Fri session
        assertMostRecentSession ("20140125-21:59:00.000", "20140123-23:00:00.000", "20140124-22:00:00.000"); // shortly before 22:00 on Saturday     returns Thurs-Fri session AGAIN!

        assertMostRecentSession ("20140120-12:00:00.000", "20140116-23:00:00.000", "20140117-22:00:00.000");
        assertMostRecentSession ("20140120-21:59:00.000", "20140116-23:00:00.000", "20140117-22:00:00.000");
        assertMostRecentSession ("20140120-21:59:00.000", "20140116-23:00:00.000", "20140117-22:00:00.000");
    }

    /** Test weekly "Monday to Friday" schedule */
    @Test
    public void testMondayFridayWeeklyMostRecentSessionBefore () {
        schedule = makeSchedule(Calendar.MONDAY, Calendar.FRIDAY, "09:00:00", "23:00:00", false);

        assertMostRecentSession ("20140120-08:59:00.000", "20140113-09:00:00.000", "20140117-23:00:00.000"); // shortly before session start
        assertMostRecentSession ("20140120-09:01:00.000", "20140120-09:00:00.000", "20140124-23:00:00.000"); // shortly after session start
        assertMostRecentSession ("20140124-22:59:00.000", "20140120-09:00:00.000", "20140124-23:00:00.000"); // shortly before session close
        assertMostRecentSession ("20140124-23:01:00.000", "20140120-09:00:00.000", "20140124-23:00:00.000"); // shortly after session close

        assertMostRecentSession ("20140120-00:00:00.000", "20140113-09:00:00.000", "20140117-23:00:00.000"); // midnight Jan 20
        assertMostRecentSession ("20140120-12:00:00.000", "20140120-09:00:00.000", "20140124-23:00:00.000"); // noon  Jan 20
        assertMostRecentSession ("20140121-00:00:00.000", "20140120-09:00:00.000", "20140124-23:00:00.000"); // midnight Jan 21
        assertMostRecentSession ("20140121-12:00:00.000", "20140120-09:00:00.000", "20140124-23:00:00.000"); // noon Jan 21
    }

    /** Test weekly "Monday to Friday" schedule where start time is later than end time */
    @Test
    public void testMondayFridayWeeklyMostRecentSessionBefore_Inverted () {
        schedule = makeSchedule(Calendar.MONDAY, Calendar.FRIDAY, "23:00:00", "22:00:00", false);  // starts Monday 23:00 and continues until Friday 22:00

        assertMostRecentSession ("20140120-22:59:00.000", "20140113-23:00:00.000", "20140117-22:00:00.000"); // shortly before session start
        assertMostRecentSession ("20140120-23:01:00.000", "20140120-23:00:00.000", "20140124-22:00:00.000"); // shortly after session start
        assertMostRecentSession ("20140124-21:59:00.000", "20140120-23:00:00.000", "20140124-22:00:00.000"); // shortly before session close
        assertMostRecentSession ("20140124-22:01:00.000", "20140120-23:00:00.000", "20140124-22:00:00.000"); // shortly after session close

        assertMostRecentSession ("20140120-00:00:00.000", "20140113-23:00:00.000", "20140117-22:00:00.000"); // midnight Jan 20
        assertMostRecentSession ("20140120-12:00:00.000", "20140113-23:00:00.000", "20140117-22:00:00.000"); // noon Jan 20
        assertMostRecentSession ("20140121-00:00:00.000", "20140120-23:00:00.000", "20140124-22:00:00.000"); // midnight Jan 21
        assertMostRecentSession ("20140121-12:00:00.000", "20140120-23:00:00.000", "20140124-22:00:00.000"); // noon Jan 21
    }

    /// NextSession


    /** Test "every day same hours" schedule */
    @Test
    public void testEveryDayNextSessionAfter () {
        schedule = makeSchedule(-1, -1, "09:00:00", "23:00:00", true);

        assertNextSession ("20140121-08:59:00.000", "20140121-09:00:00.000", "20140121-23:00:00.000"); // shortly before session start
        assertNextSession ("20140121-09:01:00.000", "20140122-09:00:00.000", "20140122-23:00:00.000"); // shortly after session start  => next session instead of current
        assertNextSession ("20140121-22:59:00.000", "20140122-09:00:00.000", "20140122-23:00:00.000"); // shortly before session close => next session instead of current
        assertNextSession ("20140121-23:01:00.000", "20140122-09:00:00.000", "20140122-23:00:00.000"); // shortly after session close

        assertNextSession ("20140121-00:00:00.000", "20140121-09:00:00.000", "20140121-23:00:00.000"); // midnight
        assertNextSession ("20140121-12:00:00.000", "20140122-09:00:00.000", "20140122-23:00:00.000"); // noon => next session instead of current
    }

    /** Test "every day same hours" schedule where start time is later than end time */
    @Test
    public void testEveryDayNextSessionAfter_Inverted () {
        schedule = makeSchedule(-1, -1, "23:00:00", "22:00:00", true);  // starts 23:00 and continues until 22:00 next day

        assertNextSession ("20140121-22:59:00.000", "20140121-23:00:00.000", "20140122-22:00:00.000"); // shortly before session start
        assertNextSession ("20140121-23:01:00.000", "20140122-23:00:00.000", "20140123-22:00:00.000"); // shortly after session start  => next session instead of current
        assertNextSession ("20140122-21:59:00.000", "20140122-23:00:00.000", "20140123-22:00:00.000"); // shortly before session close => next session instead of current
        assertNextSession ("20140122-22:01:00.000", "20140122-23:00:00.000", "20140123-22:00:00.000"); // shortly after session close

        assertNextSession ("20140121-00:00:00.000", "20140121-23:00:00.000", "20140122-22:00:00.000"); // midnight
        assertNextSession ("20140121-12:00:00.000", "20140121-23:00:00.000", "20140122-22:00:00.000"); // noon
    }

    /** Test "Monday to Friday hours" schedule */
    @Test
    public void testMondayFridayNextSessionAfter () {
        schedule = makeSchedule(Calendar.MONDAY, Calendar.FRIDAY, "09:00:00", "23:00:00", true);
        assertNextSession ("20140121-08:59:00.000", "20140121-09:00:00.000", "20140121-23:00:00.000"); // shortly before session start
        assertNextSession ("20140121-09:01:00.000", "20140122-09:00:00.000", "20140122-23:00:00.000"); // shortly after session start  => next session instead of current
        assertNextSession ("20140121-22:59:00.000", "20140122-09:00:00.000", "20140122-23:00:00.000"); // shortly before session close => next session instead of current
        assertNextSession ("20140121-23:01:00.000", "20140122-09:00:00.000", "20140122-23:00:00.000"); // shortly after session close

        assertNextSession ("20140121-00:00:00.000", "20140121-09:00:00.000", "20140121-23:00:00.000"); // midnight
        assertNextSession ("20140121-12:00:00.000", "20140122-09:00:00.000", "20140122-23:00:00.000"); // noon  => next session instead of current

        // Special cases:
        assertNextSession ("20140124-23:00:00.000", "20140127-09:00:00.000", "20140127-23:00:00.000"); // session close on Friday get us next Monday session
    }

    /** Test "Monday to Friday hours" schedule where start time is later than end time */
    @Test
    public void testMondayFridayNextSessionAfter_Inverted () {
        schedule = makeSchedule(Calendar.MONDAY, Calendar.FRIDAY, "23:00:00", "22:00:00", true);  // starts 23:00 and continues until 22:00 next day (THIS IS ACTUALLY 4 DAYS PER WEEK, NOT 5!)
        assertNextSession ("20140121-22:59:00.000", "20140121-23:00:00.000", "20140122-22:00:00.000"); // shortly before session start
        assertNextSession ("20140121-23:01:00.000", "20140122-23:00:00.000", "20140123-22:00:00.000"); // shortly after session start
        assertNextSession ("20140122-21:59:00.000", "20140122-23:00:00.000", "20140123-22:00:00.000"); // shortly before session close
        assertNextSession ("20140122-22:01:00.000", "20140122-23:00:00.000", "20140123-22:00:00.000"); // shortly after session close

        assertNextSession ("20140121-00:00:00.000", "20140121-23:00:00.000", "20140122-22:00:00.000"); // midnight
        assertNextSession ("20140121-12:00:00.000", "20140121-23:00:00.000", "20140122-22:00:00.000"); // noon

        // Special cases:
        assertNextSession ("20140120-21:59:00.000", "20140120-23:00:00.000", "20140121-22:00:00.000"); // shortly before session open on Monday returns Mon-Tues session (1)
        assertNextSession ("20140121-21:59:00.000", "20140121-23:00:00.000", "20140122-22:00:00.000"); // shortly before session open on Tuesday returns Tues-Wed session  (2)
        assertNextSession ("20140122-21:59:00.000", "20140122-23:00:00.000", "20140123-22:00:00.000"); // shortly before session open on Wed returns Wed-Thurs session (3)
        assertNextSession ("20140123-21:59:00.000", "20140123-23:00:00.000", "20140124-22:00:00.000"); // shortly before session open on Thurs returns Thurs-Frid session (4)
        assertNextSession ("20140124-21:59:00.000", "20140127-23:00:00.000", "20140128-22:00:00.000"); // shortly before session open on Friday returns Mon-Tues session
    }

    /** Test weekly "Monday to Friday" schedule */
    @Test
    public void testMondayFridayWeeklyNextSessionAfter () {
        schedule = makeSchedule(Calendar.MONDAY, Calendar.FRIDAY, "09:00:00", "23:00:00", false);

        assertNextSession ("20140120-08:59:00.000", "20140120-09:00:00.000", "20140124-23:00:00.000"); // shortly before session start on Monday
        assertNextSession ("20140120-09:01:00.000", "20140127-09:00:00.000", "20140131-23:00:00.000"); // shortly after session start on Monday
        assertNextSession ("20140124-22:59:00.000", "20140127-09:00:00.000", "20140131-23:00:00.000"); // shortly before session close on Friday
        assertNextSession ("20140124-23:01:00.000", "20140127-09:00:00.000", "20140131-23:00:00.000"); // shortly after session close on Friday

        assertNextSession ("20140120-00:00:00.000", "20140120-09:00:00.000", "20140124-23:00:00.000"); // midnight Jan 20
        assertNextSession ("20140120-12:00:00.000", "20140127-09:00:00.000", "20140131-23:00:00.000"); // noon  Jan 20
    }

    /** Test weekly "Monday to Friday" schedule where start time is later than end time */
    @Test
    public void testMondayFridayWeeklyNextSessionAfter_Inverted () {
        schedule = makeSchedule(Calendar.MONDAY, Calendar.FRIDAY, "23:00:00", "22:00:00", false);  // starts Monday 23:00 and continues until Friday 22:00

        assertNextSession ("20140120-22:59:00.000", "20140120-23:00:00.000", "20140124-22:00:00.000"); // shortly before session start on Monday
        assertNextSession ("20140120-23:01:00.000", "20140127-23:00:00.000", "20140131-22:00:00.000"); // shortly after session start on Monday
        assertNextSession ("20140124-21:59:00.000", "20140127-23:00:00.000", "20140131-22:00:00.000"); // shortly before session close on Friday
        assertNextSession ("20140124-22:01:00.000", "20140127-23:00:00.000", "20140131-22:00:00.000"); // shortly after session close on Friday

        assertNextSession ("20140120-00:00:00.000", "20140120-23:00:00.000", "20140124-22:00:00.000"); // midnight Jan 20
        assertNextSession ("20140120-12:00:00.000", "20140120-23:00:00.000", "20140124-22:00:00.000"); // noon Jan 20
        assertNextSession ("20140121-00:00:00.000", "20140127-23:00:00.000", "20140131-22:00:00.000"); // midnight Jan 20
        assertNextSession ("20140121-12:00:00.000", "20140127-23:00:00.000", "20140131-22:00:00.000"); // noon Jan 20
    }
    
    @Test
    public void testWaitTime() throws InterruptedException {
        schedule = makeSchedule(Calendar.MONDAY, Calendar.FRIDAY, "09:00:00", "23:00:00", true);

        assertSessionWaitAndEndTime("20140120-09:01:00.000", "20140120-23:00:00.000", 0); // Monday shortly after open
        assertSessionWaitAndEndTime("20140121-08:59:00.000", "20140121-23:00:00.000", 1*60000); // Tuesday shortly before open
        assertSessionWaitAndEndTime("20140121-09:01:00.000", "20140121-23:00:00.000", 0); // Tuesday shortly after open
        assertSessionWaitAndEndTime("20140117-22:59:00.000", "20140117-23:00:00.000", 0); // Friday before close
        assertSessionWaitAndEndTime("20140117-23:01:00.000", "20140120-23:00:00.000", (1+24+24+9)*3600000 - 1*60000); // Friday after close
    }


    /** Test weekly "Monday to Friday" schedule where start time is later than end time */
    @Test
    public void testFridayBeforeEnd () throws InterruptedException {
        schedule = makeSchedule(Calendar.SUNDAY, Calendar.FRIDAY, "17:30:00", "16:59:00", true);  // Sun-Fri daily from 17:30 till 16:59 next day

        assertNextSession ("20140314-13:30:00.000", "20140316-17:30:00.000", "20140317-16:59:00.000"); // Friday midday
        assertSessionWaitAndEndTime("20140314-13:30:00.000", "20140314-16:59:00.000", 0); // Friday before close
    }

    /// Helpers

    private void assertMostRecentSession (String currentTime, String expectedStart, String expectedEnd) {
        long now = TestUtils.parseLocalTimestamp(currentTime);
        synchronized (schedule) { // make assert happy
            schedule.setMostRecentSessionBefore(now);
        }

        long actualStart = schedule.start.getTimeInMillis();
        long actualEnd = schedule.end.getTimeInMillis();

        Assert.assertEquals("Start of most recent session for " + new Date(now), expectedStart,  TestUtils.formatLocalTimestamp(actualStart));
        Assert.assertEquals("End of most recent session for " + new Date(now), expectedEnd, TestUtils.formatLocalTimestamp(actualEnd));
    }

    private void assertNextSession (String currentTime, String expectedStart, String expectedEnd) {
        long now = TestUtils.parseLocalTimestamp(currentTime);
        synchronized (schedule) { // make assert happy
            schedule.setNextSessionAfter(now);
        }

        long actualStart = schedule.start.getTimeInMillis();
        long actualEnd = schedule.end.getTimeInMillis();

        Assert.assertEquals("Start of next session for " + new Date(now), expectedStart,  TestUtils.formatLocalTimestamp(actualStart));
        Assert.assertEquals("End of next session for " + new Date(now), expectedEnd, TestUtils.formatLocalTimestamp(actualEnd));
    }

    private void assertSessionWaitAndEndTime(String currentTime, String expectedSessionEndTime, long delayBeforeStart) throws InterruptedException {
        timeSource.setLocalTime(currentTime);
        SessionTimes sessionTimes = schedule.waitForSessionStart();

        long now = timeSource.currentTimeMillis();
        Assert.assertEquals("Session start time", delayBeforeStart, Math.max(0, sessionTimes.getStart() - now));


        String actualSessionEndTime = TestUtils.formatLocalTimestamp(sessionTimes.getEnd());
        Assert.assertEquals("Session end time", expectedSessionEndTime, actualSessionEndTime);
    }


//    private void assertNewSession (SessionSchedule ss, String lastConnectionTimestamp, String currentTime) throws InterruptedException {
//        timeSource.setLocalTime(currentTime);
//        boolean newSession = ss.waitForSessionStart(TestUtils.parseLocalTimestamp(lastConnectionTimestamp));
//        Assert.assertTrue("same session", newSession);
//    }
//
//    private void assertOldSession (SessionSchedule ss, String lastConnectionTimestamp, String currentTime) throws InterruptedException {
//        timeSource.setLocalTime(currentTime);
//        boolean newSession = ss.waitForSessionStart(TestUtils.parseLocalTimestamp(lastConnectionTimestamp));
//        Assert.assertFalse("old session", newSession);
//    }


    private SimpleSessionSchedule makeSchedule (int startDayOfWeek, int endDayOfWeek, String startTimeOfDay, String endTimeOfDay, boolean isDailySchedule) {
        return new SimpleSessionSchedule(startDayOfWeek, endDayOfWeek, startTimeOfDay, endTimeOfDay, isDailySchedule, null, timeSource);
    }

    private static String formatDuration (long millis) {
        int hours = (int) millis / 3600000;
        int minutes = (int) (millis % 3600000) / 60000;
        int seconds = (int) (millis % 60000) / 1000;
        int msec = (int) millis % 1000;
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, msec);
    }

}
