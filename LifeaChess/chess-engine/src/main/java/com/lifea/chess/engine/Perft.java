package com.lifea.chess.engine;

/** Move-path enumeration used to validate move generation. */
public final class Perft {
    private Perft() {
    }

    public static long perft(Position pos, int depth) {
        if (depth == 0) {
            return 1L;
        }
        long nodes = 0L;
        for (Move m : pos.legalMoves()) {
            if (depth == 1) {
                nodes++;
            } else {
                pos.make(m);
                nodes += perft(pos, depth - 1);
                pos.unmake();
            }
        }
        return nodes;
    }
}
