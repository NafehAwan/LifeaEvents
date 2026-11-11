package com.lifea.chess.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NotationTest {

    @Test
    void fenRoundTrip() {
        for (String fen : new String[] {
                Fen.START,
                "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
                "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3",
                "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"}) {
            assertEquals(fen, Fen.format(Fen.parse(fen)));
        }
    }

    @Test
    void fenRejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> Fen.parse("not a fen"));
        assertThrows(IllegalArgumentException.class, () -> Fen.parse("8/8/8/8/8/8/8/8 w - - 0 1"));
        assertThrows(IllegalArgumentException.class, () -> Fen.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1"));
    }

    @Test
    void sanBasics() {
        Position p = Position.initial();
        assertEquals("e4", San.format(p, Uci.parse("e2e4")));
        assertEquals("Nf3", San.format(p, Uci.parse("g1f3")));
        Game g = Game.fromStart();
        g.playSan("e4");
        g.playSan("d5");
        assertEquals("exd5", g.play(Uci.parse("e4d5")).san());
    }

    @Test
    void sanCastlingPromotionAndChecks() {
        Position p = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        assertEquals("O-O", San.format(p, Uci.parse("e1g1")));
        assertEquals("O-O-O", San.format(p, Uci.parse("e1c1")));

        Position promo = Fen.parse("3r4/4P3/8/8/8/8/8/4K2k w - - 0 1");
        assertEquals("exd8=Q", San.format(promo, Uci.parse("e7d8q")));
        assertEquals("e8=N", San.format(promo, Uci.parse("e7e8n")));

        Position mate = Fen.parse("k7/8/1K6/8/8/8/8/7R w - - 0 1");
        assertEquals("Rh8#", San.format(mate, Uci.parse("h1h8")));
    }

    @Test
    void sanDisambiguation() {
        Position files = Fen.parse("4k3/8/8/8/8/1N3N2/8/4K3 w - - 0 1");
        assertEquals("Nbd2", San.format(files, Uci.parse("b3d2")));
        assertEquals("Nfd2", San.format(files, Uci.parse("f3d2")));

        Position ranks = Fen.parse("4k3/8/8/8/8/N7/8/N3K3 w - - 0 1");
        assertEquals("N1c2", San.format(ranks, Uci.parse("a1c2")));
        assertEquals("N3c2", San.format(ranks, Uci.parse("a3c2")));

        Position both = Fen.parse("1k6/8/8/8/4Q2Q/8/8/K6Q w - - 0 1");
        assertEquals("Qh4e1", San.format(both, Uci.parse("h4e1")));
        assertEquals("Qee1", San.format(both, Uci.parse("e4e1")));
        assertEquals("Q1e1", San.format(both, Uci.parse("h1e1")));
    }

    @Test
    void sanParsingAcceptsVariants() {
        Position p = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        assertEquals(Uci.parse("e1g1"), San.parse(p, "0-0"));
        assertEquals(Uci.parse("e1c1"), San.parse(p, "O-O-O"));
        Position promo = Fen.parse("3r4/4P3/8/8/8/8/8/4K2k w - - 0 1");
        assertEquals(Uci.parse("e7d8q"), San.parse(promo, "exd8=Q"));
        assertEquals(Uci.parse("e7d8q"), San.parse(promo, "exd8=Q+!"));
        assertThrows(IllegalArgumentException.class, () -> San.parse(promo, "e8=K"));
        assertThrows(IllegalArgumentException.class, () -> San.parse(Position.initial(), "Nf6"));
    }

    @Test
    void uciRoundTrip() {
        assertEquals("e2e4", Uci.parse("e2e4").uci());
        assertEquals("e7e8q", new Move(Square.parse("e7"), Square.parse("e8"), PieceType.QUEEN).uci());
        assertEquals(PieceType.KNIGHT, Uci.parse("a7b8n").promotion());
        assertThrows(IllegalArgumentException.class, () -> Uci.parse("e2"));
        assertThrows(IllegalArgumentException.class, () -> Uci.parse("z9z9"));
    }

    @Test
    void movetext() {
        Game g = Game.fromStart();
        g.playSan("e4");
        g.playSan("e5");
        g.playSan("Nf3");
        assertEquals("1. e4 e5 2. Nf3", g.movetext());
        g.resign(Color.BLACK);
        assertEquals("1. e4 e5 2. Nf3 1-0", g.movetext());
    }

    @Test
    void squaresAndGlyphs() {
        assertEquals(0, Square.parse("a1"));
        assertEquals(63, Square.parse("h8"));
        assertEquals("e4", Square.name(Square.parse("e4")));
        assertEquals(false, Square.isLight(Square.parse("a1")));
        assertEquals(true, Square.isLight(Square.parse("h1")));
        assertEquals('♔', Piece.WHITE_KING.glyph());
        assertEquals('♟', Piece.BLACK_PAWN.glyph());
        assertEquals(Piece.BLACK_QUEEN, Piece.fromFenChar('q'));
    }
}
