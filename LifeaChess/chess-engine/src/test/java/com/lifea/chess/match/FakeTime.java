package com.lifea.chess.match;

import java.util.function.LongSupplier;

/** Controllable clock for deterministic tests. */
final class FakeTime implements LongSupplier {
    long millis = 1_000_000L;

    @Override
    public long getAsLong() {
        return millis;
    }

    void advance(long ms) {
        millis += ms;
    }
}
