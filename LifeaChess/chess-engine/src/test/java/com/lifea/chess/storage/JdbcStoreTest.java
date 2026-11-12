package com.lifea.chess.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lifea.chess.match.PlayerStats;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JdbcStoreTest {

    @Test
    void playersRoundTripAndLeaderboard() throws Exception {
        try (JdbcStore store = JdbcStore.open("jdbc:sqlite::memory:", null, null)) {
            UUID a = UUID.randomUUID();
            UUID b = UUID.randomUUID();
            assertTrue(store.loadPlayer(a).isEmpty());
            store.savePlayer(new RatedPlayer(a, "Alice", PlayerStats.fresh(1000).afterWin(16)), 1L);
            store.savePlayer(new RatedPlayer(b, "Bob", PlayerStats.fresh(1000).afterLoss(-16)), 1L);
            store.savePlayer(new RatedPlayer(UUID.randomUUID(), "Nobody", PlayerStats.fresh(1000)), 1L);

            RatedPlayer alice = store.loadPlayer(a).orElseThrow();
            assertEquals("Alice", alice.name());
            assertEquals(1016, alice.stats().rating());
            assertEquals(1, alice.stats().wins());

            store.savePlayer(new RatedPlayer(a, "Alice2", alice.stats().afterWin(10)), 2L);
            assertEquals("Alice2", store.loadPlayer(a).orElseThrow().name());
            assertEquals(1026, store.loadPlayer(a).orElseThrow().stats().rating());
            assertTrue(store.findPlayerByName("alice2").isPresent());

            List<RatedPlayer> top = store.topPlayers(10);
            assertEquals(2, top.size(), "unrated players are excluded");
            assertEquals(a, top.get(0).uuid());
            assertEquals(1, store.rankOf(a));
            assertEquals(2, store.rankOf(b));
            assertEquals(-1, store.rankOf(UUID.randomUUID()));
        }
    }

    @Test
    void matchesAreIdempotentAndQueryable() throws Exception {
        try (JdbcStore store = JdbcStore.open("jdbc:sqlite::memory:", null, null)) {
            UUID a = UUID.randomUUID();
            UUID b = UUID.randomUUID();
            UUID id = UUID.randomUUID();
            StoredMatch m = new StoredMatch(id, a, "Alice", b, "Bob", "3+2", true, "WHITE_WINS", "CHECKMATE",
                    "1. f3 e5 2. g4 Qh4# 0-1", "fen", 10L, 20L, 16, -16);
            assertTrue(store.recordMatch(m));
            assertFalse(store.recordMatch(m), "same id must not be recorded twice");
            assertEquals(1, store.matchCount());
            List<StoredMatch> recent = store.recentMatches(b, 5);
            assertEquals(1, recent.size());
            assertEquals("CHECKMATE", recent.get(0).reason());
            assertTrue(recent.get(0).ranked());
            assertTrue(store.recentMatches(UUID.randomUUID(), 5).isEmpty());
        }
    }
}
