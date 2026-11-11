package com.lifea.chess.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GameFlowTest {

    @Test
    void rejectsIllegalMovesAndMovesAfterEnd() {
        Game g = Game.fromStart();
        assertThrows(IllegalArgumentException.class, () -> g.playUci("e2e5"));
        assertThrows(IllegalArgumentException.class, () -> g.playUci("e7e5"));
        g.resign(Color.WHITE);
        assertTrue(g.isOver());
        assertEquals(Color.BLACK, g.result().winner());
        assertThrows(IllegalStateException.class, () -> g.playUci("e2e4"));
        assertThrows(IllegalStateException.class, () -> g.resign(Color.BLACK));
        assertTrue(g.legalMoves().isEmpty());
    }

    @Test
    void drawByAgreementAndAbandon() {
        Game g = Game.fromStart();
        g.agreeDraw();
        assertEquals(GameResult.Reason.AGREEMENT, g.result().reason());
        Game h = Game.fromStart();
        h.abandon(Color.BLACK);
        assertEquals(Color.WHITE, h.result().winner());
        assertEquals(GameResult.Reason.ABANDONED, h.result().reason());
    }

    @Test
    void undoRestoresPositionResultAndRepetitions() {
        Game g = Game.fromStart();
        g.playSan("f3");
        g.playSan("e5");
        g.playSan("g4");
        g.playSan("Qh4#");
        assertTrue(g.isOver());
        MoveRecord undone = g.undo();
        assertEquals("Qh4#", undone.san());
        assertFalse(g.isOver());
        assertNull(g.result());
        assertEquals(3, g.history().size());
        assertEquals(Color.BLACK, g.sideToMove());
        assertEquals(1, g.repetitionCount());
        assertThrows(IllegalStateException.class, () -> {
            Game e = Game.fromStart();
            e.undo();
        });
    }

    @Test
    void legalMovesFromSquareAndCaptures() {
        Game g = Game.fromStart();
        List<Move> fromE2 = g.legalMovesFrom(Square.parse("e2"));
        assertEquals(2, fromE2.size());
        assertTrue(g.legalMovesFrom(Square.parse("e4")).isEmpty());
        g.playSan("e4");
        g.playSan("d5");
        g.playSan("exd5");
        assertEquals(List.of(Piece.BLACK_PAWN), g.capturedFrom(Color.BLACK));
        assertTrue(g.capturedFrom(Color.WHITE).isEmpty());
        assertEquals(38, g.position().material(Color.BLACK));
    }

    @Test
    void historyRecordsCheckFlagsAndFen() {
        Game g = Game.fromFen("4k3/8/8/8/8/8/8/R3K3 w Q - 0 1");
        MoveRecord r = g.playSan("Ra8+");
        assertTrue(r.check());
        assertFalse(r.checkmate());
        assertEquals(1, r.ply());
        assertEquals(1, r.moveNumber());
        assertEquals(Color.WHITE, r.mover());
        assertEquals(g.fen(), r.fenAfter());
        assertEquals("a1a8", r.uci());
    }

    @Test
    void startFenPreserved() {
        String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
        Game g = Game.fromFen(fen);
        g.playUci("e1g1");
        assertEquals(fen, g.startFen());
    }
}
