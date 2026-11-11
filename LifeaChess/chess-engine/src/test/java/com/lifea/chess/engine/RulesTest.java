package com.lifea.chess.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RulesTest {

    private static Move mv(String uci) {
        return Uci.parse(uci);
    }

    // ---------------------------------------------------------------- castling

    @Test
    void castlingBothSidesWhenClear() {
        Position p = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        List<Move> legal = p.legalMoves();
        assertTrue(legal.contains(mv("e1g1")));
        assertTrue(legal.contains(mv("e1c1")));
        p.make(mv("e1g1"));
        assertEquals(Piece.WHITE_ROOK, p.pieceAt(Square.parse("f1")));
        assertNull(p.pieceAt(Square.parse("h1")));
        assertEquals(0, p.castlingRights() & (Position.CASTLE_WK | Position.CASTLE_WQ));
    }

    @Test
    void cannotCastleWhileInCheck() {
        Position p = Fen.parse("4k3/8/8/8/8/8/4r3/R3K2R w KQ - 0 1");
        assertTrue(p.inCheck());
        List<Move> legal = p.legalMoves();
        assertFalse(legal.contains(mv("e1g1")));
        assertFalse(legal.contains(mv("e1c1")));
    }

    @Test
    void cannotCastleThroughOrOntoAttackedSquare() {
        // Black rook on f8-file attacks f1 (through), rook on c-file attacks c1 (onto) is covered by d-file test below.
        Position p = Fen.parse("4kr2/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        List<Move> legal = p.legalMoves();
        assertFalse(legal.contains(mv("e1g1")), "f1 attacked: no kingside castling");
        assertTrue(legal.contains(mv("e1c1")), "queenside unaffected");

        Position q = Fen.parse("3rk3/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        assertFalse(q.legalMoves().contains(mv("e1c1")), "d1 attacked: no queenside castling");
        assertTrue(q.legalMoves().contains(mv("e1g1")));
    }

    @Test
    void queensideCastlingAllowedWhenOnlyB1Attacked() {
        // The king does not cross b1, so an attack on b1 does not prevent O-O-O.
        Position p = Fen.parse("1r2k3/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        assertTrue(p.legalMoves().contains(mv("e1c1")));
    }

    @Test
    void castlingRightsLostWhenKingOrRookMoves() {
        Position p = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        p.make(mv("h1h2"));
        assertFalse(p.canCastle(Position.CASTLE_WK));
        assertTrue(p.canCastle(Position.CASTLE_WQ));
        p.make(mv("e8d8"));
        assertFalse(p.canCastle(Position.CASTLE_BK));
        assertFalse(p.canCastle(Position.CASTLE_BQ));
    }

    @Test
    void capturingRookOnOriginSquareRemovesRight() {
        Position p = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        p.make(mv("a1a8"));
        assertFalse(p.canCastle(Position.CASTLE_BQ));
        assertTrue(p.canCastle(Position.CASTLE_BK));
    }

    @Test
    void fenWithStaleRightsIsSanitised() {
        Position p = Fen.parse("4k3/8/8/8/8/8/8/4K2R w KQkq - 0 1");
        assertEquals(Position.CASTLE_WK, p.castlingRights());
    }

    // ---------------------------------------------------------------- en passant

    @Test
    void enPassantOnlyImmediatelyAfterDoubleStep() {
        Game g = Game.fromStart();
        g.playUci("e2e4");
        g.playUci("a7a6");
        g.playUci("e4e5");
        g.playUci("d7d5");
        assertEquals(Square.parse("d6"), g.position().epSquare());
        assertTrue(g.legalMoves().contains(mv("e5d6")));
        g.playUci("h2h3");
        g.playUci("a6a5");
        assertFalse(g.legalMoves().contains(mv("e5d6")), "ep right expired");
    }

    @Test
    void enPassantRemovesCapturedPawn() {
        Position p = Fen.parse("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2");
        Move ep = mv("e5d6");
        assertTrue(p.isEnPassant(ep));
        assertTrue(p.isCapture(ep));
        assertEquals(Piece.BLACK_PAWN, p.capturedBy(ep));
        p.make(ep);
        assertNull(p.pieceAt(Square.parse("d5")));
        assertEquals(Piece.WHITE_PAWN, p.pieceAt(Square.parse("d6")));
        p.unmake();
        assertEquals(Piece.BLACK_PAWN, p.pieceAt(Square.parse("d5")));
    }

    @Test
    void enPassantIllegalWhenItExposesKingAlongRank() {
        // Ka5, Pb5 vs pc5 (just double-stepped), rh5: bxc6 would uncover the rook.
        Position p = Fen.parse("8/8/8/KPp4r/8/8/8/4k3 w - c6 0 1");
        assertFalse(p.legalMoves().contains(mv("b5c6")));
    }

    // ---------------------------------------------------------------- promotion

    @Test
    void promotionGeneratesAllFourPiecesAndCapturePromotion() {
        Position p = Fen.parse("3r4/4P3/8/8/8/8/8/4K2k w - - 0 1");
        List<Move> legal = p.legalMoves();
        for (PieceType t : List.of(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
            assertTrue(legal.contains(new Move(Square.parse("e7"), Square.parse("e8"), t)));
            assertTrue(legal.contains(new Move(Square.parse("e7"), Square.parse("d8"), t)));
        }
        assertFalse(legal.contains(new Move(Square.parse("e7"), Square.parse("e8"))), "plain pawn move to last rank is not legal");
        p.make(new Move(Square.parse("e7"), Square.parse("d8"), PieceType.KNIGHT));
        assertEquals(Piece.WHITE_KNIGHT, p.pieceAt(Square.parse("d8")));
        p.unmake();
        assertEquals(Piece.WHITE_PAWN, p.pieceAt(Square.parse("e7")));
        assertEquals(Piece.BLACK_ROOK, p.pieceAt(Square.parse("d8")));
    }

    // ---------------------------------------------------------------- pins & checks

    @Test
    void pinnedPieceCannotLeaveThePinLine() {
        Position p = Fen.parse("4k3/8/8/8/8/8/4N3/4K2r w - - 0 1");
        // Knight e2 is not pinned (rook on h1 attacks along rank 1, not through e2) — king must respond.
        assertTrue(p.inCheck());
        Position q = Fen.parse("4k3/4r3/8/8/8/8/4N3/4K3 w - - 0 1");
        for (Move m : q.legalMoves()) {
            assertNotEquals(Square.parse("e2"), m.from(), "pinned knight must not move: " + m);
        }
    }

    @Test
    void doubleCheckOnlyKingMoves() {
        // Nf6 gives check, discovering the bishop on b2 — two attackers.
        Position p = Fen.parse("4k3/8/5N2/8/8/8/1B6/4K3 b - - 0 1");
        assertTrue(p.inCheck());
        for (Move m : p.legalMoves()) {
            assertEquals(PieceType.KING, p.pieceAt(m.from()).type());
        }
    }

    @Test
    void foolsMateIsCheckmate() {
        Game g = Game.fromStart();
        g.playSan("f3");
        g.playSan("e5");
        g.playSan("g4");
        MoveRecord last = g.playSan("Qh4#");
        assertTrue(last.checkmate());
        assertEquals("Qh4#", last.san());
        assertTrue(g.isOver());
        assertEquals(GameResult.Outcome.BLACK_WINS, g.result().outcome());
        assertEquals(GameResult.Reason.CHECKMATE, g.result().reason());
        assertEquals(Color.BLACK, g.result().winner());
        assertThrows(IllegalStateException.class, () -> g.playUci("a2a3"));
    }

    @Test
    void stalemateIsDraw() {
        Game g = Game.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
        assertTrue(g.isOver());
        assertEquals(GameResult.Reason.STALEMATE, g.result().reason());
        assertTrue(g.result().isDraw());
    }

    // ---------------------------------------------------------------- repetition

    @Test
    void threefoldIsClaimableAndFivefoldIsAutomatic() {
        Game g = Game.fromStart();
        assertEquals(1, g.repetitionCount());
        for (int cycle = 1; cycle <= 2; cycle++) {
            shuffleKnights(g);
        }
        assertEquals(3, g.repetitionCount());
        assertTrue(g.canClaimThreefold());
        assertFalse(g.isOver());
        shuffleKnights(g);
        assertEquals(4, g.repetitionCount());
        assertFalse(g.isOver());
        shuffleKnights(g);
        assertTrue(g.isOver());
        assertEquals(GameResult.Reason.FIVEFOLD_REPETITION, g.result().reason());
    }

    private static void shuffleKnights(Game g) {
        g.playUci("g1f3");
        g.playUci("g8f6");
        g.playUci("f3g1");
        g.playUci("f6g8");
    }

    @Test
    void repetitionKeyIncludesEpOnlyWhenCaptureIsPossible() {
        Position withCapture = Fen.parse("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3");
        Position sameNoEp = Fen.parse("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq - 0 3");
        assertNotEquals(withCapture.repetitionKey(), sameNoEp.repetitionKey());

        Position noCapturer = Fen.parse("rnbqkbnr/ppp1pppp/8/3p4/8/8/PPPPPPPP/RNBQKBNR w KQkq d6 0 2");
        Position noCapturerNoEp = Fen.parse("rnbqkbnr/ppp1pppp/8/3p4/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 2");
        assertEquals(noCapturer.repetitionKey(), noCapturerNoEp.repetitionKey());
    }

    // ---------------------------------------------------------------- move clocks

    @Test
    void fiftyMoveRuleIsClaimable() {
        Game g = Game.fromFen("4k3/8/8/8/8/8/8/R3K3 w Q - 99 80");
        assertFalse(g.canClaimFiftyMoves());
        g.playUci("a1a2");
        assertEquals(100, g.position().halfmoveClock());
        assertTrue(g.canClaimFiftyMoves());
        assertFalse(g.isOver());
        assertTrue(g.claimDraw());
        assertEquals(GameResult.Reason.FIFTY_MOVE_RULE, g.result().reason());
    }

    @Test
    void seventyFiveMoveRuleIsAutomaticButCheckmateWins() {
        Game g = Game.fromFen("4k3/8/8/8/8/8/8/R3K3 w Q - 149 80");
        g.playUci("a1a2");
        assertTrue(g.isOver());
        assertEquals(GameResult.Reason.SEVENTY_FIVE_MOVE_RULE, g.result().reason());

        // Mate on the 150th half-move still counts as mate.
        Game m = Game.fromFen("k7/8/1K6/8/8/8/8/7R w - - 149 80");
        m.playUci("h1h8");
        assertEquals(GameResult.Reason.CHECKMATE, m.result().reason());
    }

    @Test
    void pawnMoveAndCaptureResetHalfmoveClock() {
        Position p = Fen.parse("4k3/8/8/8/8/8/P7/R3K3 w Q - 40 30");
        p.make(mv("a2a3"));
        assertEquals(0, p.halfmoveClock());
        p.unmake();
        p.make(mv("a1b1"));
        assertEquals(41, p.halfmoveClock());
    }

    // ---------------------------------------------------------------- material

    @Test
    void deadPositions() {
        assertTrue(Fen.parse("4k3/8/8/8/8/8/8/4K3 w - - 0 1").isDeadPosition());
        assertTrue(Fen.parse("4k3/8/8/8/8/8/8/2B1K3 w - - 0 1").isDeadPosition());
        assertTrue(Fen.parse("4k3/8/8/8/8/8/8/1N2K3 w - - 0 1").isDeadPosition());
        assertTrue(Fen.parse("4kb2/8/8/8/8/8/8/2B1K3 w - - 0 1").isDeadPosition(), "bishops on same colour");
        assertFalse(Fen.parse("2b1k3/8/8/8/8/8/8/2B1K3 w - - 0 1").isDeadPosition(), "opposite-colour bishops");
        assertFalse(Fen.parse("4k3/8/8/8/8/8/8/1NN1K3 w - - 0 1").isDeadPosition());
        assertFalse(Fen.parse("4k3/8/8/8/8/8/P7/4K3 w - - 0 1").isDeadPosition());
        Game g = Game.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        assertEquals(GameResult.Reason.DEAD_POSITION, g.result().reason());
    }

    @Test
    void timeoutRespectsInsufficientMaterial() {
        Game lone = Game.fromFen("4k3/8/8/8/8/8/8/R3K3 b Q - 0 1");
        lone.timeout(Color.WHITE);
        assertEquals(GameResult.Reason.TIMEOUT_INSUFFICIENT_MATERIAL, lone.result().reason());
        assertTrue(lone.result().isDraw());

        Game win = Game.fromFen("4k3/8/8/8/8/8/8/R3K3 b Q - 0 1");
        win.timeout(Color.BLACK);
        assertEquals(GameResult.Reason.TIMEOUT, win.result().reason());
        assertEquals(Color.WHITE, win.result().winner());

        Game twoKnights = Game.fromFen("4k3/8/8/8/8/8/8/1NN1K3 b - - 0 1");
        twoKnights.timeout(Color.BLACK);
        assertEquals(Color.WHITE, twoKnights.result().winner(), "two knights can mate in principle");
    }

    @Test
    void materialCount() {
        Position p = Position.initial();
        assertEquals(39, p.material(Color.WHITE));
        assertEquals(39, p.material(Color.BLACK));
    }
}
