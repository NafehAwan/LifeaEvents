package com.lifea.chess.engine;

/**
 * A move: origin, destination and optional promotion piece. Special semantics
 * (capture, en passant, castling, double push) are derived from the position the
 * move is played in, so two moves are equal when from/to/promotion match.
 */
public record Move(int from, int to, PieceType promotion) {

    public Move {
        if (from < 0 || from > 63 || to < 0 || to > 63) {
            throw new IllegalArgumentException("Square out of range");
        }
        if (promotion != null && !promotion.isPromotionTarget()) {
            throw new IllegalArgumentException("Invalid promotion piece: " + promotion);
        }
    }

    public Move(int from, int to) {
        this(from, to, null);
    }

    public boolean isPromotion() {
        return promotion != null;
    }

    /** UCI string such as "e2e4" or "e7e8q". */
    public String uci() {
        return Uci.format(this);
    }

    @Override
    public String toString() {
        return uci();
    }
}
