package com.lifea.chess.engine;

/** UCI long algebraic move notation ("e2e4", "e7e8q"). */
public final class Uci {
    private Uci() {
    }

    public static String format(Move m) {
        StringBuilder sb = new StringBuilder(5);
        sb.append(Square.name(m.from())).append(Square.name(m.to()));
        if (m.isPromotion()) {
            sb.append(Character.toLowerCase(m.promotion().letter()));
        }
        return sb.toString();
    }

    /** Parses a UCI move string. Does not check legality. */
    public static Move parse(String s) {
        if (s == null || (s.length() != 4 && s.length() != 5)) {
            throw new IllegalArgumentException("Bad UCI move: " + s);
        }
        int from = Square.parse(s.substring(0, 2));
        int to = Square.parse(s.substring(2, 4));
        PieceType promo = s.length() == 5 ? PieceType.fromLetter(s.charAt(4)) : null;
        return new Move(from, to, promo);
    }
}
