package dev.cheat.client.util;

/**
 * Millisecond stopwatch, e.g. for delays and CPS limits.
 */
public final class TimerUtil {
    private long start = now();

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    public boolean hasElapsed(long millis) {
        return now() - start >= millis;
    }

    /**
     * Returns true and resets when {@code millis} have passed.
     */
    public boolean passedAndReset(long millis) {
        if (!hasElapsed(millis)) return false;
        reset();
        return true;
    }

    public long elapsed() {
        return now() - start;
    }

    public void reset() {
        start = now();
    }
}
