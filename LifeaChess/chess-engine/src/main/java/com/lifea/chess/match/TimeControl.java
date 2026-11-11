package com.lifea.chess.match;

import java.util.List;

/**
 * A chess time control: starting time plus Fischer increment per completed move.
 * Notation "3+2" means 3 minutes with a 2-second increment.
 */
public record TimeControl(String name, long initialMillis, long incrementMillis) {

    public enum Category { BULLET, BLITZ, RAPID, CLASSICAL }

    public static final TimeControl BULLET_1_0 = minutes("Bullet", 1, 0);
    public static final TimeControl BULLET_2_1 = minutes("Bullet", 2, 1);
    public static final TimeControl BLITZ_3_0 = minutes("Blitz", 3, 0);
    public static final TimeControl BLITZ_3_2 = minutes("Blitz", 3, 2);
    public static final TimeControl BLITZ_5_0 = minutes("Blitz", 5, 0);
    public static final TimeControl RAPID_10_0 = minutes("Rapid", 10, 0);
    public static final TimeControl RAPID_10_5 = minutes("Rapid", 10, 5);
    public static final TimeControl CLASSICAL_30_0 = minutes("Classical", 30, 0);

    public static final List<TimeControl> PRESETS = List.of(
            BULLET_1_0, BULLET_2_1, BLITZ_3_0, BLITZ_3_2, BLITZ_5_0, RAPID_10_0, RAPID_10_5, CLASSICAL_30_0);

    public TimeControl {
        if (initialMillis <= 0) {
            throw new IllegalArgumentException("Initial time must be positive");
        }
        if (incrementMillis < 0) {
            throw new IllegalArgumentException("Increment cannot be negative");
        }
        if (name == null || name.isBlank()) {
            name = "Custom";
        }
    }

    public static TimeControl minutes(String name, int minutes, int incrementSeconds) {
        return new TimeControl(name, minutes * 60_000L, incrementSeconds * 1_000L);
    }

    public static TimeControl custom(int minutes, int incrementSeconds) {
        return minutes("Custom", minutes, incrementSeconds);
    }

    /** Parses "3+2", "10+0", "1+0". */
    public static TimeControl parse(String text) {
        String[] parts = text.trim().split("\\+");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Expected minutes+increment, e.g. 3+2: " + text);
        }
        int minutes = Integer.parseInt(parts[0].trim());
        int inc = Integer.parseInt(parts[1].trim());
        for (TimeControl preset : PRESETS) {
            if (preset.initialMillis == minutes * 60_000L && preset.incrementMillis == inc * 1_000L) {
                return preset;
            }
        }
        return custom(minutes, inc);
    }

    /** "3+2" style label. */
    public String notation() {
        return (initialMillis / 60_000L) + "+" + (incrementMillis / 1_000L);
    }

    /** Estimated game length (initial + 40 moves of increment) decides the category. */
    public Category category() {
        long estimate = initialMillis + 40L * incrementMillis;
        if (estimate < 3L * 60_000L) {
            return Category.BULLET;
        }
        if (estimate < 10L * 60_000L) {
            return Category.BLITZ;
        }
        if (estimate < 30L * 60_000L) {
            return Category.RAPID;
        }
        return Category.CLASSICAL;
    }

    @Override
    public String toString() {
        return name + " " + notation();
    }
}
