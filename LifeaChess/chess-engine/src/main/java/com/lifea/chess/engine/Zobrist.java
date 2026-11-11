package com.lifea.chess.engine;

import java.util.Random;

/** Fixed-seed Zobrist keys used for repetition detection. */
final class Zobrist {
    static final long[][] PIECE = new long[12][64];
    static final long[] CASTLING = new long[16];
    static final long[] EP_FILE = new long[8];
    static final long SIDE;

    static {
        Random rnd = new Random(0x5EEDC0FFEEL);
        for (int p = 0; p < 12; p++) {
            for (int s = 0; s < 64; s++) {
                PIECE[p][s] = rnd.nextLong();
            }
        }
        for (int i = 0; i < 16; i++) {
            CASTLING[i] = rnd.nextLong();
        }
        for (int i = 0; i < 8; i++) {
            EP_FILE[i] = rnd.nextLong();
        }
        SIDE = rnd.nextLong();
    }

    private Zobrist() {
    }
}
