package com.lifea.chess.engine;

/** Final outcome of a game and why it ended. */
public record GameResult(Outcome outcome, Reason reason) {

    public enum Outcome {
        WHITE_WINS, BLACK_WINS, DRAW;

        public static Outcome winFor(Color c) {
            return c == Color.WHITE ? WHITE_WINS : BLACK_WINS;
        }
    }

    public enum Reason {
        CHECKMATE,
        STALEMATE,
        RESIGNATION,
        TIMEOUT,
        TIMEOUT_INSUFFICIENT_MATERIAL,
        AGREEMENT,
        THREEFOLD_REPETITION,
        FIVEFOLD_REPETITION,
        FIFTY_MOVE_RULE,
        SEVENTY_FIVE_MOVE_RULE,
        DEAD_POSITION,
        ABANDONED
    }

    /** The winning colour, or null for a draw. */
    public Color winner() {
        return switch (outcome) {
            case WHITE_WINS -> Color.WHITE;
            case BLACK_WINS -> Color.BLACK;
            case DRAW -> null;
        };
    }

    public boolean isDraw() {
        return outcome == Outcome.DRAW;
    }

    /** PGN result token. */
    public String token() {
        return switch (outcome) {
            case WHITE_WINS -> "1-0";
            case BLACK_WINS -> "0-1";
            case DRAW -> "1/2-1/2";
        };
    }
}
