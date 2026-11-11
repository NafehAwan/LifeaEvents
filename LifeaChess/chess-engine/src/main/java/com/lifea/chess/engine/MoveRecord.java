package com.lifea.chess.engine;

/** One played move with its notation and effects. */
public record MoveRecord(int ply, Color mover, Move move, String san, Piece captured,
                         boolean check, boolean checkmate, String fenAfter) {

    /** Full-move number this ply belongs to (1-based). */
    public int moveNumber() {
        return (ply + 1) / 2;
    }

    public String uci() {
        return move.uci();
    }
}
