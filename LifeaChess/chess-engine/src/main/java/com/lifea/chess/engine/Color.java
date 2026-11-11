package com.lifea.chess.engine;

/** The two sides. */
public enum Color {
    WHITE, BLACK;

    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    /** Direction pawns of this colour advance along the rank axis (+1 for white, -1 for black). */
    public int pawnDirection() {
        return this == WHITE ? 1 : -1;
    }

    public int backRank() {
        return this == WHITE ? 0 : 7;
    }

    public int pawnStartRank() {
        return this == WHITE ? 1 : 6;
    }

    public int promotionRank() {
        return this == WHITE ? 7 : 0;
    }
}
