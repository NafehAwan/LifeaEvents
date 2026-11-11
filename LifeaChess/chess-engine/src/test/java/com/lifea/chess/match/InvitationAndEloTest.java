package com.lifea.chess.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lifea.chess.engine.GameResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvitationAndEloTest {
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID c = UUID.randomUUID();

    @Test
    void invitationLifecycle() {
        FakeTime t = new FakeTime();
        InvitationService svc = new InvitationService(60_000, t);
        assertEquals(InvitationService.Failure.SELF_INVITE, svc.send(a, "A", a, "A", TimeControl.BLITZ_3_0, false).failure());
        InvitationService.SendResult r = svc.send(a, "A", b, "B", TimeControl.BLITZ_3_0, true);
        assertTrue(r.ok());
        assertEquals(InvitationService.Failure.ALREADY_PENDING, svc.send(a, "A", b, "B", TimeControl.BLITZ_3_0, true).failure());
        assertTrue(svc.latestFor(b).isPresent());
        assertTrue(svc.latestFor(a).isEmpty());
        assertTrue(svc.deny(b).isPresent());
        assertEquals(0, svc.size());

        svc.send(a, "A", b, "B", TimeControl.BLITZ_3_0, true);
        svc.send(c, "C", b, "B", TimeControl.RAPID_10_0, false);
        assertEquals(2, svc.size());
        Invitation accepted = svc.accept(b).orElseThrow();
        assertEquals(c, accepted.from(), "newest invitation is accepted");
        assertEquals(0, svc.size(), "accepting clears everything involving either party");

        svc.send(a, "A", b, "B", TimeControl.BLITZ_3_0, true);
        assertTrue(svc.cancel(a).isPresent());
        assertEquals(0, svc.size());
    }

    @Test
    void invitationsExpire() {
        FakeTime t = new FakeTime();
        InvitationService svc = new InvitationService(30_000, t);
        Invitation inv = svc.send(a, "A", b, "B", TimeControl.BLITZ_3_0, false).invitation();
        assertFalse(inv.isExpired(t.getAsLong()));
        assertEquals(30_000, inv.remainingMillis(t.getAsLong()));
        t.advance(30_000);
        assertTrue(inv.isExpired(t.getAsLong()));
        assertEquals(1, svc.purgeExpired().size());
        assertTrue(svc.latestFor(b).isEmpty());
    }

    @Test
    void eloSymmetricAndProvisional() {
        EloCalculator elo = new EloCalculator(32, 40, 30);
        assertEquals(0.5, EloCalculator.expectedScore(1500, 1500), 1e-9);
        EloCalculator.Change even = elo.compute(1500, 100, 1500, 100, GameResult.Outcome.WHITE_WINS);
        assertEquals(16, even.whiteDelta());
        assertEquals(-16, even.blackDelta());
        EloCalculator.Change draw = elo.compute(1500, 100, 1500, 100, GameResult.Outcome.DRAW);
        assertEquals(0, draw.whiteDelta());
        assertEquals(0, draw.blackDelta());
        EloCalculator.Change upset = elo.compute(1200, 100, 1600, 100, GameResult.Outcome.WHITE_WINS);
        assertTrue(upset.whiteDelta() > 16, "beating a stronger player pays more");
        assertEquals(-upset.whiteDelta(), upset.blackDelta());
        EloCalculator.Change provisional = elo.compute(1500, 0, 1500, 100, GameResult.Outcome.WHITE_WINS);
        assertEquals(20, provisional.whiteDelta(), "provisional K=40");
        assertEquals(-16, provisional.blackDelta());
    }

    @Test
    void statsTrackPeakAndStreak() {
        PlayerStats s = PlayerStats.fresh(1000);
        s = s.afterWin(16).afterWin(15).afterLoss(-20).afterDraw(0).afterWin(10);
        assertEquals(1021, s.rating());
        assertEquals(1031, s.peakRating());
        assertEquals(3, s.wins());
        assertEquals(1, s.losses());
        assertEquals(1, s.draws());
        assertEquals(5, s.games());
        assertEquals(1, s.streak());
        assertEquals(2, s.bestStreak());
        assertEquals(0.6, s.winRate(), 1e-9);
    }
}
