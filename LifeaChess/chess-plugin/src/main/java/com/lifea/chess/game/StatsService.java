package com.lifea.chess.game;

import com.lifea.chess.LifeaChessPlugin;
import com.lifea.chess.config.ChessConfig;
import com.lifea.chess.engine.GameResult;
import com.lifea.chess.match.EloCalculator;
import com.lifea.chess.match.Match;
import com.lifea.chess.match.PlayerStats;
import com.lifea.chess.storage.JdbcStore;
import com.lifea.chess.storage.RatedPlayer;
import com.lifea.chess.storage.StoredMatch;
import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Ratings and history. All database work runs on a single background thread;
 * callbacks are delivered on the main thread. Results are applied exactly once
 * per match.
 */
public final class StatsService {
    private final LifeaChessPlugin plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "LifeaChess-DB");
        t.setDaemon(true);
        return t;
    });
    private final Map<UUID, RatedPlayer> cache = new ConcurrentHashMap<>();
    /** Last ranked game timestamp per unordered player pair, for the anti-farming cooldown. */
    private final Map<String, Long> lastRankedBetween = new HashMap<>();
    private JdbcStore store;
    private EloCalculator elo;

    public StatsService(LifeaChessPlugin plugin) {
        this.plugin = plugin;
    }

    public void open() throws SQLException {
        ChessConfig c = plugin.config();
        elo = new EloCalculator(c.kFactor, c.provisionalK, c.provisionalGames);
        if (c.databaseType == ChessConfig.DatabaseType.JDBC && !c.jdbcUrl.isBlank()) {
            store = JdbcStore.open(c.jdbcUrl, c.dbUser, c.dbPassword);
            plugin.getLogger().info("Connected to database via JDBC.");
        } else {
            File f = new File(plugin.getDataFolder(), c.sqliteFile);
            plugin.getDataFolder().mkdirs();
            store = JdbcStore.openSqlite(f.getAbsolutePath());
            plugin.getLogger().info("Using SQLite database " + f.getName());
        }
    }

    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (store != null) {
            try {
                store.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("Error closing database: " + e.getMessage());
            }
        }
    }

    private void sync(Runnable r) {
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, r);
        }
    }

    private RatedPlayer fresh(UUID id, String name) {
        return new RatedPlayer(id, name, PlayerStats.fresh(plugin.config().startingRating));
    }

    /** Loads (or creates) the player's row into the cache. */
    public void preload(Player player) {
        UUID id = player.getUniqueId();
        String name = player.getName();
        executor.execute(() -> {
            try {
                RatedPlayer p = store.loadPlayer(id).orElse(null);
                if (p == null) {
                    p = fresh(id, name);
                    store.savePlayer(p, System.currentTimeMillis());
                } else if (!p.name().equals(name)) {
                    p = new RatedPlayer(id, name, p.stats());
                    store.savePlayer(p, System.currentTimeMillis());
                }
                cache.put(id, p);
            } catch (SQLException e) {
                plugin.getLogger().warning("Could not load stats for " + name + ": " + e.getMessage());
            }
        });
    }

    public void evict(UUID id) {
        cache.remove(id);
    }

    /** Cached stats (may be null for a moment right after login). */
    public RatedPlayer cached(UUID id) {
        return cache.get(id);
    }

    public RatedPlayer cachedOrFresh(UUID id, String name) {
        RatedPlayer p = cache.get(id);
        return p != null ? p : fresh(id, name);
    }

    public record Applied(RatedPlayer white, RatedPlayer black, int whiteDelta, int blackDelta, boolean rated, boolean farmBlocked) {
    }

    /**
     * Applies a finished match: Elo (if ranked and not farm-blocked), stats, and
     * history. Idempotent per match id. The callback runs on the main thread.
     */
    public void applyResult(ActiveMatch am, Consumer<Applied> callback) {
        if (am.resultProcessed()) {
            return;
        }
        Match m = am.match();
        GameResult result = m.result();
        if (result == null) {
            return;
        }
        boolean ranked = m.ranked();
        boolean farmBlocked = false;
        if (ranked) {
            long cooldown = plugin.config().sameOpponentCooldownMillis;
            String key = pairKey(m.whiteId(), m.blackId());
            Long last = lastRankedBetween.get(key);
            long now = System.currentTimeMillis();
            if (cooldown > 0 && last != null && now - last < cooldown) {
                farmBlocked = true;
            }
            lastRankedBetween.put(key, now);
        }
        boolean rated = ranked && !farmBlocked;
        final boolean fRated = rated;
        final boolean fFarm = farmBlocked;
        am.markProcessed(rated, 0, 0);
        String movetext = m.game().movetext();
        String fen = m.game().fen();
        executor.execute(() -> {
            try {
                RatedPlayer white = store.loadPlayer(m.whiteId()).orElse(fresh(m.whiteId(), m.whiteName()));
                RatedPlayer black = store.loadPlayer(m.blackId()).orElse(fresh(m.blackId(), m.blackName()));
                int wd = 0;
                int bd = 0;
                if (fRated) {
                    EloCalculator.Change ch = elo.compute(white.stats().rating(), white.stats().games(),
                            black.stats().rating(), black.stats().games(), result.outcome());
                    wd = ch.whiteDelta();
                    bd = ch.blackDelta();
                }
                PlayerStats ws = white.stats();
                PlayerStats bs = black.stats();
                switch (result.outcome()) {
                    case WHITE_WINS -> {
                        ws = ws.afterWin(wd);
                        bs = bs.afterLoss(bd);
                    }
                    case BLACK_WINS -> {
                        ws = ws.afterLoss(wd);
                        bs = bs.afterWin(bd);
                    }
                    case DRAW -> {
                        ws = ws.afterDraw(wd);
                        bs = bs.afterDraw(bd);
                    }
                }
                RatedPlayer nw = new RatedPlayer(white.uuid(), m.whiteName(), ws);
                RatedPlayer nb = new RatedPlayer(black.uuid(), m.blackName(), bs);
                long now = System.currentTimeMillis();
                boolean inserted = store.recordMatch(new StoredMatch(m.id(), m.whiteId(), m.whiteName(), m.blackId(), m.blackName(),
                        m.timeControl().notation(), ranked, result.outcome().name(), result.reason().name(),
                        movetext, fen, m.startedAt(), m.endedAt(), wd, bd));
                if (inserted) {
                    store.savePlayer(nw, now);
                    store.savePlayer(nb, now);
                    cache.put(nw.uuid(), nw);
                    cache.put(nb.uuid(), nb);
                }
                final int fwd = wd;
                final int fbd = bd;
                sync(() -> {
                    am.markProcessed(fRated, fwd, fbd);
                    callback.accept(new Applied(nw, nb, fwd, fbd, fRated, fFarm));
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not record match " + m.id() + ": " + e.getMessage());
            }
        });
    }

    private static String pairKey(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
    }

    /** Looks a player up by name (online or offline) with their leaderboard rank. */
    public void lookup(String name, BiConsumer<Optional<RatedPlayer>, Integer> callback) {
        executor.execute(() -> {
            try {
                Optional<RatedPlayer> p = store.findPlayerByName(name);
                int rank = p.isPresent() ? store.rankOf(p.get().uuid()) : -1;
                sync(() -> callback.accept(p, rank));
            } catch (SQLException e) {
                plugin.getLogger().warning("Stats lookup failed: " + e.getMessage());
                sync(() -> callback.accept(Optional.empty(), -1));
            }
        });
    }

    public void lookup(UUID id, BiConsumer<Optional<RatedPlayer>, Integer> callback) {
        executor.execute(() -> {
            try {
                Optional<RatedPlayer> p = store.loadPlayer(id);
                int rank = p.isPresent() ? store.rankOf(id) : -1;
                sync(() -> callback.accept(p, rank));
            } catch (SQLException e) {
                sync(() -> callback.accept(Optional.empty(), -1));
            }
        });
    }

    public void top(int limit, Consumer<List<RatedPlayer>> callback) {
        executor.execute(() -> {
            try {
                List<RatedPlayer> list = store.topPlayers(limit);
                sync(() -> callback.accept(list));
            } catch (SQLException e) {
                plugin.getLogger().warning("Leaderboard query failed: " + e.getMessage());
                sync(() -> callback.accept(List.of()));
            }
        });
    }

    public void history(UUID id, int limit, Consumer<List<StoredMatch>> callback) {
        executor.execute(() -> {
            try {
                List<StoredMatch> list = store.recentMatches(id, limit);
                sync(() -> callback.accept(list));
            } catch (SQLException e) {
                sync(() -> callback.accept(List.of()));
            }
        });
    }
}
