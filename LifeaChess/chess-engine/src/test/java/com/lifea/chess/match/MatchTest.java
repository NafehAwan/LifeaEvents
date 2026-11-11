package com.lifea.chess.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lifea.chess.engine.Color;
import com.lifea.chess.engine.GameResult;
import com.lifea.chess.engine.San;
import com.lifea.chess.engine.Uci;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchTest {
    private final UUID white = UUID.randomUUID();
    private final UUID black = UUID.randomUUID();
    private final UUID stranger = UUID.randomUUID();
    private final FakeTime time = new FakeTime();

    private Match newMatch(TimeControl tc) {
        Match m = new Match(white, "Alice", black, "Bob", tc, true, DisconnectPolicy.forfeitAfterSeconds(30), time);
        m.start();
        return m;
    }

    private Match.MoveOutcome san(Match m, UUID player, String san) {
        return m.playMove(player, San.parse(m.game().position(), san));
    }

    @Test
    void turnOrderAndIdentityAreEnforced() {
        Match m = newMatch(TimeControl.BLITZ_5_0);
        assertEquals(Match.MoveFailure.NOT_YOUR_TURN, m.playMove(black, Uci.parse("e7e5")).failure());
        assertEquals(Match.MoveFailure.NOT_A_PLAYER, m.playMove(stranger, Uci.parse("e2e4")).failure());
        assertEquals(Match.MoveFailure.ILLEGAL_MOVE, m.playMove(white, Uci.parse("e2e5")).failure());
        assertTrue(san(m, white, "e4").ok());
        assertEquals(Color.BLACK, m.clock().runningSide());
        assertTrue(san(m, black, "e5").ok());
    }

    @Test
    void checkmateFinishesTheMatch() {
        Match m = newMatch(TimeControl.BLITZ_5_0);
        san(m, white, "f3");
        san(m, black, "e5");
        san(m, white, "g4");
        Match.MoveOutcome mate = san(m, black, "Qh4#");
        assertTrue(mate.ok());
        assertTrue(mate.record().checkmate());
        assertTrue(m.isFinished());
        assertEquals(GameResult.Reason.CHECKMATE, m.result().reason());
        assertEquals(Color.BLACK, m.result().winner());
        assertEquals(Match.MoveFailure.NOT_ACTIVE, m.playMove(white, Uci.parse("a2a3")).failure());
        assertFalse(m.clock().isRunning());
    }

    @Test
    void timeoutWinsUnlessInsufficientMaterial() {
        Match m = newMatch(TimeControl.BULLET_1_0);
        san(m, white, "e4");
        time.advance(60_001);
        assertEquals(Color.BLACK, m.checkFlag());
        assertEquals(GameResult.Reason.TIMEOUT, m.result().reason());
        assertEquals(Color.WHITE, m.result().winner());
        assertEquals(Match.MoveFailure.NOT_ACTIVE, m.playMove(black, Uci.parse("e7e5")).failure());
    }

    @Test
    void resignAndDrawOffers() {
        Match m = newMatch(TimeControl.BLITZ_3_2);
        assertEquals(Match.DrawAction.OFFERED, m.offerOrAcceptDraw(white));
        assertEquals(Color.WHITE, m.drawOfferedBy());
        assertEquals(Match.DrawAction.REJECTED, m.offerOrAcceptDraw(white), "cannot accept own offer");
        assertEquals(Match.DrawAction.DECLINED, m.declineDraw(black));
        assertNull(m.drawOfferedBy());

        assertEquals(Match.DrawAction.OFFERED, m.offerOrAcceptDraw(black));
        san(m, white, "e4");
        assertNull(m.drawOfferedBy(), "moving declines the pending offer");

        assertEquals(Match.DrawAction.OFFERED, m.offerOrAcceptDraw(black));
        assertEquals(Match.DrawAction.ACCEPTED, m.offerOrAcceptDraw(white));
        assertEquals(GameResult.Reason.AGREEMENT, m.result().reason());

        Match r = newMatch(TimeControl.BLITZ_3_2);
        assertFalse(r.resign(stranger));
        assertTrue(r.resign(black));
        assertEquals(Color.WHITE, r.result().winner());
        assertEquals(GameResult.Reason.RESIGNATION, r.result().reason());
    }

    @Test
    void disconnectPausesThenForfeits() {
        Match m = newMatch(TimeControl.BLITZ_5_0);
        san(m, white, "e4");
        m.markDisconnected(black);
        assertEquals(Match.State.PAUSED, m.state());
        assertFalse(m.clock().isRunning());
        long blackBefore = m.clock().remaining(Color.BLACK);
        time.advance(10_000);
        assertEquals(blackBefore, m.clock().remaining(Color.BLACK), "clock paused during grace");
        assertFalse(m.checkDisconnectTimeout());
        m.markReconnected(black);
        assertEquals(Match.State.ACTIVE, m.state());
        assertEquals(Color.BLACK, m.clock().runningSide());

        m.markDisconnected(black);
        time.advance(31_000);
        assertTrue(m.checkDisconnectTimeout());
        assertTrue(m.isFinished());
        assertEquals(GameResult.Reason.ABANDONED, m.result().reason());
        assertEquals(Color.WHITE, m.result().winner());
    }

    @Test
    void spectatorsAndMetadata() {
        Match m = newMatch(TimeControl.RAPID_10_0);
        assertFalse(m.addSpectator(white), "players are not spectators");
        assertTrue(m.addSpectator(stranger));
        assertEquals(1, m.spectators().size());
        assertEquals(Match.MoveFailure.NOT_A_PLAYER, m.playMove(stranger, Uci.parse("e2e4")).failure());
        assertEquals(Color.WHITE, m.colorOf(white));
        assertEquals(black, m.opponentOf(white));
        assertEquals("Bob", m.nameOf(Color.BLACK));
        time.advance(5_000);
        assertEquals(5_000, m.duration());
        m.abort();
        assertTrue(m.isFinished());
        assertEquals(GameResult.Reason.AGREEMENT, m.result().reason());
    }
}
