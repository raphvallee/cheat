package dev.cheat.client.util;

/** Millisecond stopwatch, e.g. for delays and CPS limits. */
public final class TimerUtil {
    private long start = System.currentTimeMillis();

    public boolean hasElapsed(long millis) {
        return System.currentTimeMillis() - start >= millis;
    }

    /** Returns true and resets when {@code millis} have passed. */
    public boolean passedAndReset(long millis) {
        if (!hasElapsed(millis)) return false;
        reset();
        return true;
    }

    public long elapsed() {
        return System.currentTimeMillis() - start;
    }

    public void reset() {
        start = System.currentTimeMillis();
    }
}
