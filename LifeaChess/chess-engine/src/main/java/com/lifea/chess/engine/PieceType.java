package com.lifea.chess.engine;

/** Piece kinds with their SAN letter and conventional material value. */
public enum PieceType {
    KING('K', 0),
    QUEEN('Q', 9),
    ROOK('R', 5),
    BISHOP('B', 3),
    KNIGHT('N', 3),
    PAWN('P', 1);

    private final char letter;
    private final int value;

    PieceType(char letter, int value) {
        this.letter = letter;
        this.value = value;
    }

    /** Upper-case SAN/FEN letter. */
    public char letter() {
        return letter;
    }

    /** Material value in pawns (king = 0). */
    public int value() {
        return value;
    }

    public boolean isPromotionTarget() {
        return this == QUEEN || this == ROOK || this == BISHOP || this == KNIGHT;
    }

    public static PieceType fromLetter(char c) {
        for (PieceType t : values()) {
            if (t.letter == Character.toUpperCase(c)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown piece letter: " + c);
    }
}
