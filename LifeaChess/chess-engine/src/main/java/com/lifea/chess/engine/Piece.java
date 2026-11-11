package com.lifea.chess.engine;

/** A coloured piece. Twelve constants; {@link #index()} is stable for table lookups. */
public enum Piece {
    WHITE_KING(Color.WHITE, PieceType.KING),
    WHITE_QUEEN(Color.WHITE, PieceType.QUEEN),
    WHITE_ROOK(Color.WHITE, PieceType.ROOK),
    WHITE_BISHOP(Color.WHITE, PieceType.BISHOP),
    WHITE_KNIGHT(Color.WHITE, PieceType.KNIGHT),
    WHITE_PAWN(Color.WHITE, PieceType.PAWN),
    BLACK_KING(Color.BLACK, PieceType.KING),
    BLACK_QUEEN(Color.BLACK, PieceType.QUEEN),
    BLACK_ROOK(Color.BLACK, PieceType.ROOK),
    BLACK_BISHOP(Color.BLACK, PieceType.BISHOP),
    BLACK_KNIGHT(Color.BLACK, PieceType.KNIGHT),
    BLACK_PAWN(Color.BLACK, PieceType.PAWN);

    private final Color color;
    private final PieceType type;

    Piece(Color color, PieceType type) {
        this.color = color;
        this.type = type;
    }

    public Color color() {
        return color;
    }

    public PieceType type() {
        return type;
    }

    public int index() {
        return ordinal();
    }

    public boolean is(Color c, PieceType t) {
        return color == c && type == t;
    }

    /** FEN character: upper case for white, lower case for black. */
    public char fenChar() {
        return color == Color.WHITE ? type.letter() : Character.toLowerCase(type.letter());
    }

    /** Unicode figurine (white: ♔♕♖♗♘♙, black: ♚♛♜♝♞♟). */
    public char glyph() {
        return switch (this) {
            case WHITE_KING -> '♔';
            case WHITE_QUEEN -> '♕';
            case WHITE_ROOK -> '♖';
            case WHITE_BISHOP -> '♗';
            case WHITE_KNIGHT -> '♘';
            case WHITE_PAWN -> '♙';
            case BLACK_KING -> '♚';
            case BLACK_QUEEN -> '♛';
            case BLACK_ROOK -> '♜';
            case BLACK_BISHOP -> '♝';
            case BLACK_KNIGHT -> '♞';
            case BLACK_PAWN -> '♟';
        };
    }

    public static Piece of(Color color, PieceType type) {
        return values()[(color == Color.WHITE ? 0 : 6) + type.ordinal()];
    }

    public static Piece fromFenChar(char c) {
        Color color = Character.isUpperCase(c) ? Color.WHITE : Color.BLACK;
        return of(color, PieceType.fromLetter(c));
    }
}
