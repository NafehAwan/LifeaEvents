package com.lifea.chess.config;

import com.lifea.chess.match.DisconnectPolicy;
import com.lifea.chess.match.TimeControl;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Typed view of config.yml. Rebuilt on reload. */
public final class ChessConfig {

    public enum DatabaseType { SQLITE, JDBC }

    public enum PieceStyle { GLYPH, PIXEL }

    public final DatabaseType databaseType;
    public final String sqliteFile;
    public final String jdbcUrl;
    public final String dbUser;
    public final String dbPassword;

    public final int startingRating;
    public final int kFactor;
    public final int provisionalK;
    public final int provisionalGames;
    public final long sameOpponentCooldownMillis;

    public final long inviteExpiryMillis;
    public final DisconnectPolicy disconnectPolicy;

    public final TimeControl defaultTimeControl;
    public final List<TimeControl> presets;
    public final boolean allowCustom;
    public final int customMaxMinutes;
    public final int customMaxIncrement;

    public final boolean rankedEnabled;
    public final boolean casualEnabled;
    public final boolean spectatorsEnabled;

    public final double boardHeight;
    public final double squareSize;
    public final boolean autoOpenBoard;
    public final boolean showHowToPanels;
    public final PieceStyle pieceStyle;

    public final Theme theme;
    public final boolean sounds;
    public final boolean animations;
    public final Set<String> disabledWorlds;
    public final boolean debug;

    private ChessConfig(FileConfiguration c) {
        databaseType = "jdbc".equalsIgnoreCase(c.getString("database.type", "sqlite")) ? DatabaseType.JDBC : DatabaseType.SQLITE;
        sqliteFile = c.getString("database.sqlite-file", "lifeachess.db");
        jdbcUrl = c.getString("database.jdbc-url", "");
        dbUser = c.getString("database.username", "");
        dbPassword = c.getString("database.password", "");

        startingRating = c.getInt("ratings.starting", 1000);
        kFactor = c.getInt("ratings.k-factor", 32);
        provisionalK = c.getInt("ratings.provisional-k", 40);
        provisionalGames = c.getInt("ratings.provisional-games", 30);
        sameOpponentCooldownMillis = Math.max(0, c.getInt("ratings.same-opponent-cooldown-minutes", 10)) * 60_000L;

        inviteExpiryMillis = Math.max(5, c.getInt("invitations.expiry-seconds", 60)) * 1000L;
        DisconnectPolicy.Action action = "ADJOURN".equalsIgnoreCase(c.getString("disconnect.action", "FORFEIT"))
                ? DisconnectPolicy.Action.ADJOURN : DisconnectPolicy.Action.FORFEIT;
        disconnectPolicy = new DisconnectPolicy(action, Math.max(0, c.getInt("disconnect.grace-seconds", 60)) * 1000L);

        List<TimeControl> list = new ArrayList<>();
        for (String s : c.getStringList("time-controls.presets")) {
            try {
                list.add(TimeControl.parse(s));
            } catch (RuntimeException ignored) {
                // skip malformed entries
            }
        }
        presets = list.isEmpty() ? TimeControl.PRESETS : List.copyOf(list);
        TimeControl def;
        try {
            def = TimeControl.parse(c.getString("time-controls.default", "10+0"));
        } catch (RuntimeException e) {
            def = TimeControl.RAPID_10_0;
        }
        defaultTimeControl = def;
        allowCustom = c.getBoolean("time-controls.allow-custom", true);
        customMaxMinutes = c.getInt("time-controls.custom-max-minutes", 180);
        customMaxIncrement = c.getInt("time-controls.custom-max-increment-seconds", 60);

        rankedEnabled = c.getBoolean("modes.ranked", true);
        casualEnabled = c.getBoolean("modes.casual", true);
        spectatorsEnabled = c.getBoolean("spectators.enabled", true);

        boardHeight = c.getDouble("table.board-height", 1.4);
        squareSize = c.getDouble("table.square-size", 0.3);
        autoOpenBoard = c.getBoolean("table.auto-open-board", true);
        showHowToPanels = c.getBoolean("table.show-how-to-panels", true);
        pieceStyle = "PIXEL".equalsIgnoreCase(c.getString("screen.piece-style", "GLYPH")) ? PieceStyle.PIXEL : PieceStyle.GLYPH;

        theme = Theme.from(c.getConfigurationSection("theme"));
        sounds = c.getBoolean("sounds.enabled", true);
        animations = c.getBoolean("animations.enabled", true);
        Set<String> worlds = new HashSet<>();
        for (String w : c.getStringList("disabled-worlds")) {
            worlds.add(w.toLowerCase(Locale.ROOT));
        }
        disabledWorlds = Set.copyOf(worlds);
        debug = c.getBoolean("debug", false);
    }

    public static ChessConfig load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        return new ChessConfig(plugin.getConfig());
    }

    public boolean isWorldDisabled(String worldName) {
        return disabledWorlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    /** Validates a custom time control against the configured limits. */
    public boolean customAllowed(TimeControl tc) {
        if (!allowCustom) {
            return presets.contains(tc);
        }
        return tc.initialMillis() <= customMaxMinutes * 60_000L && tc.incrementMillis() <= customMaxIncrement * 1_000L;
    }
}
