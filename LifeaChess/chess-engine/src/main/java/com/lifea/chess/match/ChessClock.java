package com.lifea.chess.match;

import com.lifea.chess.engine.Color;
import java.util.function.LongSupplier;

/**
 * Two-sided Fischer clock driven by wall-clock timestamps rather than ticks, so
 * it stays accurate regardless of server lag. Only the running side's time
 * decreases; the increment is credited when that side completes a move.
 */
public final class ChessClock {
    private final LongSupplier now;
    private final long incrementMillis;
    private final long[] remaining = new long[2];
    private Color running;
    private long runningSince;

    public ChessClock(TimeControl control) {
        this(control, System::currentTimeMillis);
    }

    public ChessClock(TimeControl control, LongSupplier nowMillis) {
        this.now = nowMillis;
        this.incrementMillis = control.incrementMillis();
        this.remaining[0] = control.initialMillis();
        this.remaining[1] = control.initialMillis();
    }

    private static int idx(Color c) {
        return c == Color.WHITE ? 0 : 1;
    }

    /** Starts (or restarts) the clock for {@code side}. */
    public synchronized void start(Color side) {
        settle();
        running = side;
        runningSince = now.getAsLong();
    }

    /** Freezes both clocks, banking elapsed time. */
    public synchronized void pause() {
        settle();
        running = null;
    }

    public synchronized boolean isRunning() {
        return running != null;
    }

    public synchronized Color runningSide() {
        return running;
    }

    /**
     * The side to move completed a move: bank its elapsed time, add the increment
     * (only if it had not already flagged) and start the opponent's clock.
     */
    public synchronized void press(Color mover) {
        settle();
        if (remaining[idx(mover)] > 0) {
            remaining[idx(mover)] += incrementMillis;
        }
        running = mover.opposite();
        runningSince = now.getAsLong();
    }

    /** Milliseconds left for {@code side}, never negative. */
    public synchronized long remaining(Color side) {
        long value = remaining[idx(side)];
        if (running == side) {
            value -= now.getAsLong() - runningSince;
        }
        return Math.max(0L, value);
    }

    public synchronized boolean isFlagged(Color side) {
        return remaining(side) <= 0L;
    }

    /** The side whose flag has fallen, or null. */
    public synchronized Color flagged() {
        if (isFlagged(Color.WHITE)) {
            return Color.WHITE;
        }
        if (isFlagged(Color.BLACK)) {
            return Color.BLACK;
        }
        return null;
    }

    /** Adds time to one side (admin/compensation). */
    public synchronized void add(Color side, long millis) {
        remaining[idx(side)] += millis;
    }

    private void settle() {
        if (running != null) {
            long t = now.getAsLong();
            remaining[idx(running)] -= t - runningSince;
            runningSince = t;
        }
    }

    /** "m:ss" or "ss.t" under ten seconds, e.g. "4:07" or "7.3". */
    public static String format(long millis) {
        long clamped = Math.max(0L, millis);
        long totalSeconds = clamped / 1000L;
        if (totalSeconds < 10L) {
            return totalSeconds + "." + (clamped % 1000L) / 100L;
        }
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }
}
