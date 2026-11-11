package com.lifea.chess.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lifea.chess.engine.Color;
import org.junit.jupiter.api.Test;

class ChessClockTest {

    @Test
    void onlyRunningSideLosesTime() {
        FakeTime t = new FakeTime();
        ChessClock c = new ChessClock(TimeControl.BLITZ_3_0, t);
        c.start(Color.WHITE);
        t.advance(5_000);
        assertEquals(175_000, c.remaining(Color.WHITE));
        assertEquals(180_000, c.remaining(Color.BLACK));
    }

    @Test
    void pressAddsIncrementAndSwitchesSides() {
        FakeTime t = new FakeTime();
        ChessClock c = new ChessClock(TimeControl.BLITZ_3_2, t);
        c.start(Color.WHITE);
        t.advance(10_000);
        c.press(Color.WHITE);
        assertEquals(172_000, c.remaining(Color.WHITE), "180 - 10 + 2 increment");
        assertEquals(Color.BLACK, c.runningSide());
        t.advance(3_000);
        assertEquals(172_000, c.remaining(Color.WHITE));
        assertEquals(177_000, c.remaining(Color.BLACK));
    }

    @Test
    void flagDetectionAndNoIncrementAfterFlag() {
        FakeTime t = new FakeTime();
        ChessClock c = new ChessClock(TimeControl.BULLET_2_1, t);
        c.start(Color.BLACK);
        assertNull(c.flagged());
        t.advance(120_001);
        assertTrue(c.isFlagged(Color.BLACK));
        assertEquals(Color.BLACK, c.flagged());
        assertEquals(0, c.remaining(Color.BLACK));
        c.press(Color.BLACK);
        assertEquals(0, c.remaining(Color.BLACK), "no increment once flagged");
    }

    @Test
    void pauseAndResume() {
        FakeTime t = new FakeTime();
        ChessClock c = new ChessClock(TimeControl.RAPID_10_0, t);
        c.start(Color.WHITE);
        t.advance(4_000);
        c.pause();
        assertFalse(c.isRunning());
        t.advance(60_000);
        assertEquals(596_000, c.remaining(Color.WHITE));
        c.start(Color.WHITE);
        t.advance(1_000);
        assertEquals(595_000, c.remaining(Color.WHITE));
    }

    @Test
    void formatting() {
        assertEquals("3:00", ChessClock.format(180_000));
        assertEquals("0:10", ChessClock.format(10_000));
        assertEquals("9.9", ChessClock.format(9_999));
        assertEquals("0.0", ChessClock.format(-5));
        assertEquals("12:05", ChessClock.format(725_000));
    }

    @Test
    void timeControlPresetsAndParsing() {
        assertEquals("3+2", TimeControl.BLITZ_3_2.notation());
        assertEquals(TimeControl.Category.BULLET, TimeControl.BULLET_1_0.category());
        assertEquals(TimeControl.Category.BLITZ, TimeControl.BLITZ_5_0.category());
        assertEquals(TimeControl.Category.RAPID, TimeControl.RAPID_10_5.category());
        assertEquals(TimeControl.Category.CLASSICAL, TimeControl.CLASSICAL_30_0.category());
        assertEquals(TimeControl.BLITZ_3_2, TimeControl.parse("3+2"));
        TimeControl custom = TimeControl.parse("15+10");
        assertEquals(900_000, custom.initialMillis());
        assertEquals(10_000, custom.incrementMillis());
        assertEquals("Custom", custom.name());
    }
}
