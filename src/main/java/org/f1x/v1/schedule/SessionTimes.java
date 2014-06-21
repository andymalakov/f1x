package org.f1x.v1.schedule;

import java.util.Date;

/** Data structure to hold session start/end times. <code>End</code> time is guaranteed to be not earlier than <code>Start</code> time. */
public class SessionTimes {
    private long start, end;

    public SessionTimes() {
    }

    public SessionTimes(long start, long end) {
        this.start = start;
        this.end = end;
    }

    /**
     * @return current or next session start time.
     * If current time is within the session time, it returns start time of the current session (in the past).
     * If current time is off the session, this will be set to the start time of the next session (in the future).
     * Returned value uses Java timestamp format (number of milliseconds since 1/1/1971 UTC).
     */
    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    /**
     * @return session end time (always in the future)
     * Returned value uses Java timestamp format (number of milliseconds since 1/1/1971 UTC).
     */
    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "SessionTimes{" + new Date(start) + " ... " + new Date(end) + '}';
    }
}
