package com.lifea.chess.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Perft against published node counts (chessprogramming.org). */
class PerftTest {

    private static final String KIWIPETE =
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
    private static final String POSITION_3 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1";
    private static final String POSITION_4 =
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1";
    private static final String POSITION_5 =
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";

    @ParameterizedTest(name = "start depth {0} = {1}")
    @CsvSource({"1, 20", "2, 400", "3, 8902", "4, 197281"})
    void startPosition(int depth, long expected) {
        assertEquals(expected, Perft.perft(Position.initial(), depth));
    }

    @ParameterizedTest(name = "kiwipete depth {0} = {1}")
    @CsvSource({"1, 48", "2, 2039", "3, 97862"})
    void kiwipete(int depth, long expected) {
        assertEquals(expected, Perft.perft(Fen.parse(KIWIPETE), depth));
    }

    @ParameterizedTest(name = "position3 depth {0} = {1}")
    @CsvSource({"1, 14", "2, 191", "3, 2812", "4, 43238"})
    void position3(int depth, long expected) {
        assertEquals(expected, Perft.perft(Fen.parse(POSITION_3), depth));
    }

    @ParameterizedTest(name = "position4 depth {0} = {1}")
    @CsvSource({"1, 6", "2, 264", "3, 9467"})
    void position4(int depth, long expected) {
        assertEquals(expected, Perft.perft(Fen.parse(POSITION_4), depth));
    }

    @ParameterizedTest(name = "position5 depth {0} = {1}")
    @CsvSource({"1, 44", "2, 1486", "3, 62379"})
    void position5(int depth, long expected) {
        assertEquals(expected, Perft.perft(Fen.parse(POSITION_5), depth));
    }

    @Test
    void makeUnmakeRoundTripRestoresFen() {
        Position p = Fen.parse(KIWIPETE);
        String before = Fen.format(p);
        for (Move m : p.legalMoves()) {
            p.make(m);
            for (Move m2 : p.legalMoves()) {
                p.make(m2);
                p.unmake();
            }
            p.unmake();
            assertEquals(before, Fen.format(p), "after " + m);
        }
    }
}
