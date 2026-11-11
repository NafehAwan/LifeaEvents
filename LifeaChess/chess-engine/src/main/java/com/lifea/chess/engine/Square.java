package com.lifea.chess.engine;

/**
 * Square helpers. Squares are ints 0..63 with {@code index = rank * 8 + file};
 * a1 = 0, h1 = 7, a8 = 56, h8 = 63. {@link #NONE} (-1) means "no square".
 */
public final class Square {
    public static final int NONE = -1;

    private Square() {
    }

    public static int of(int file, int rank) {
        return rank * 8 + file;
    }

    public static int file(int sq) {
        return sq & 7;
    }

    public static int rank(int sq) {
        return sq >> 3;
    }

    public static boolean valid(int file, int rank) {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }

    /** True when the square is light-coloured (a1 is dark). */
    public static boolean isLight(int sq) {
        return ((file(sq) + rank(sq)) & 1) == 1;
    }

    /** Parses algebraic notation such as "e4". */
    public static int parse(String s) {
        if (s == null || s.length() != 2) {
            throw new IllegalArgumentException("Bad square: " + s);
        }
        int file = s.charAt(0) - 'a';
        int rank = s.charAt(1) - '1';
        if (!valid(file, rank)) {
            throw new IllegalArgumentException("Bad square: " + s);
        }
        return of(file, rank);
    }

    public static String name(int sq) {
        if (sq == NONE) {
            return "-";
        }
        return "" + (char) ('a' + file(sq)) + (char) ('1' + rank(sq));
    }

    public static char fileChar(int sq) {
        return (char) ('a' + file(sq));
    }

    public static char rankChar(int sq) {
        return (char) ('1' + rank(sq));
    }
}
