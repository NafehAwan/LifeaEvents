package com.lifea.chess.storage;

import com.lifea.chess.match.PlayerStats;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Plain-JDBC persistence for players, ratings and match history. Works with
 * SQLite ({@code jdbc:sqlite:path}) out of the box and with MySQL/MariaDB when a
 * driver is on the classpath. All methods are synchronous and must be called off
 * the game thread; the plugin wraps them in an async executor. Thread-safe via
 * the connection monitor.
 */
public final class JdbcStore implements AutoCloseable {
    private final Connection connection;
    private final boolean sqlite;

    private JdbcStore(Connection connection, boolean sqlite) {
        this.connection = connection;
        this.sqlite = sqlite;
    }

    public static JdbcStore open(String jdbcUrl, String user, String password) throws SQLException {
        Connection c = (user == null || user.isBlank())
                ? DriverManager.getConnection(jdbcUrl)
                : DriverManager.getConnection(jdbcUrl, user, password);
        c.setAutoCommit(true);
        JdbcStore store = new JdbcStore(c, jdbcUrl.startsWith("jdbc:sqlite:"));
        store.createSchema();
        return store;
    }

    public static JdbcStore openSqlite(String path) throws SQLException {
        return open("jdbc:sqlite:" + path, null, null);
    }

    private void createSchema() throws SQLException {
        String autoText = sqlite ? "TEXT" : "VARCHAR(36)";
        String nameText = sqlite ? "TEXT" : "VARCHAR(64)";
        synchronized (connection) {
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS chess_players ("
                        + "uuid " + autoText + " PRIMARY KEY,"
                        + "name " + nameText + " NOT NULL,"
                        + "rating INT NOT NULL,"
                        + "peak INT NOT NULL,"
                        + "wins INT NOT NULL,"
                        + "losses INT NOT NULL,"
                        + "draws INT NOT NULL,"
                        + "streak INT NOT NULL,"
                        + "best_streak INT NOT NULL,"
                        + "updated_at BIGINT NOT NULL)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS chess_matches ("
                        + "id " + autoText + " PRIMARY KEY,"
                        + "white_uuid " + autoText + " NOT NULL,"
                        + "white_name " + nameText + " NOT NULL,"
                        + "black_uuid " + autoText + " NOT NULL,"
                        + "black_name " + nameText + " NOT NULL,"
                        + "time_control VARCHAR(32) NOT NULL,"
                        + "ranked INT NOT NULL,"
                        + "outcome VARCHAR(16) NOT NULL,"
                        + "reason VARCHAR(40) NOT NULL,"
                        + "movetext TEXT,"
                        + "final_fen VARCHAR(120),"
                        + "started_at BIGINT NOT NULL,"
                        + "ended_at BIGINT NOT NULL,"
                        + "white_delta INT NOT NULL,"
                        + "black_delta INT NOT NULL)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_chess_matches_white ON chess_matches(white_uuid)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_chess_matches_black ON chess_matches(black_uuid)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_chess_players_rating ON chess_players(rating)");
            }
        }
    }

    // ------------------------------------------------------------------ players

    public Optional<RatedPlayer> loadPlayer(UUID uuid) throws SQLException {
        synchronized (connection) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT name, rating, peak, wins, losses, draws, streak, best_streak FROM chess_players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new RatedPlayer(uuid, rs.getString(1), new PlayerStats(
                            rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8))));
                }
            }
        }
    }

    public Optional<RatedPlayer> findPlayerByName(String name) throws SQLException {
        synchronized (connection) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT uuid, name, rating, peak, wins, losses, draws, streak, best_streak FROM chess_players WHERE LOWER(name) = LOWER(?)")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(read(rs));
                }
            }
        }
    }

    public void savePlayer(RatedPlayer p, long nowMillis) throws SQLException {
        PlayerStats s = p.stats();
        synchronized (connection) {
            boolean exists;
            try (PreparedStatement q = connection.prepareStatement("SELECT 1 FROM chess_players WHERE uuid = ?")) {
                q.setString(1, p.uuid().toString());
                try (ResultSet rs = q.executeQuery()) {
                    exists = rs.next();
                }
            }
            String sql = exists
                    ? "UPDATE chess_players SET name=?, rating=?, peak=?, wins=?, losses=?, draws=?, streak=?, best_streak=?, updated_at=? WHERE uuid=?"
                    : "INSERT INTO chess_players (name, rating, peak, wins, losses, draws, streak, best_streak, updated_at, uuid) VALUES (?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, p.name());
                ps.setInt(2, s.rating());
                ps.setInt(3, s.peakRating());
                ps.setInt(4, s.wins());
                ps.setInt(5, s.losses());
                ps.setInt(6, s.draws());
                ps.setInt(7, s.streak());
                ps.setInt(8, s.bestStreak());
                ps.setLong(9, nowMillis);
                ps.setString(10, p.uuid().toString());
                ps.executeUpdate();
            }
        }
    }

    public List<RatedPlayer> topPlayers(int limit) throws SQLException {
        synchronized (connection) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT uuid, name, rating, peak, wins, losses, draws, streak, best_streak FROM chess_players "
                            + "WHERE wins + losses + draws > 0 ORDER BY rating DESC, wins DESC LIMIT ?")) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    List<RatedPlayer> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(read(rs));
                    }
                    return out;
                }
            }
        }
    }

    /** 1-based rank of a player on the leaderboard, or -1 when unrated. */
    public int rankOf(UUID uuid) throws SQLException {
        synchronized (connection) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM chess_players WHERE wins + losses + draws > 0 AND rating > "
                            + "(SELECT rating FROM chess_players WHERE uuid = ? AND wins + losses + draws > 0)")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    int above = rs.next() ? rs.getInt(1) : 0;
                    return loadPlayer(uuid).map(p -> p.stats().games() > 0 ? above + 1 : -1).orElse(-1);
                }
            }
        }
    }

    private static RatedPlayer read(ResultSet rs) throws SQLException {
        return new RatedPlayer(UUID.fromString(rs.getString(1)), rs.getString(2), new PlayerStats(
                rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8), rs.getInt(9)));
    }

    // ------------------------------------------------------------------ matches

    /** Inserts a finished match; returns false if that id was already recorded (idempotent). */
    public boolean recordMatch(StoredMatch m) throws SQLException {
        synchronized (connection) {
            try (PreparedStatement q = connection.prepareStatement("SELECT 1 FROM chess_matches WHERE id = ?")) {
                q.setString(1, m.id().toString());
                try (ResultSet rs = q.executeQuery()) {
                    if (rs.next()) {
                        return false;
                    }
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO chess_matches (id, white_uuid, white_name, black_uuid, black_name, time_control, ranked, "
                            + "outcome, reason, movetext, final_fen, started_at, ended_at, white_delta, black_delta) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, m.id().toString());
                ps.setString(2, m.whiteId().toString());
                ps.setString(3, m.whiteName());
                ps.setString(4, m.blackId().toString());
                ps.setString(5, m.blackName());
                ps.setString(6, m.timeControl());
                ps.setInt(7, m.ranked() ? 1 : 0);
                ps.setString(8, m.outcome());
                ps.setString(9, m.reason());
                ps.setString(10, m.movetext());
                ps.setString(11, m.finalFen());
                ps.setLong(12, m.startedAt());
                ps.setLong(13, m.endedAt());
                ps.setInt(14, m.whiteDelta());
                ps.setInt(15, m.blackDelta());
                ps.executeUpdate();
                return true;
            }
        }
    }

    public List<StoredMatch> recentMatches(UUID player, int limit) throws SQLException {
        synchronized (connection) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, white_uuid, white_name, black_uuid, black_name, time_control, ranked, outcome, reason, "
                            + "movetext, final_fen, started_at, ended_at, white_delta, black_delta FROM chess_matches "
                            + "WHERE white_uuid = ? OR black_uuid = ? ORDER BY ended_at DESC LIMIT ?")) {
                ps.setString(1, player.toString());
                ps.setString(2, player.toString());
                ps.setInt(3, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    List<StoredMatch> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(new StoredMatch(UUID.fromString(rs.getString(1)), UUID.fromString(rs.getString(2)),
                                rs.getString(3), UUID.fromString(rs.getString(4)), rs.getString(5), rs.getString(6),
                                rs.getInt(7) == 1, rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11),
                                rs.getLong(12), rs.getLong(13), rs.getInt(14), rs.getInt(15)));
                    }
                    return out;
                }
            }
        }
    }

    public int matchCount() throws SQLException {
        synchronized (connection) {
            try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM chess_matches")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public void close() throws SQLException {
        synchronized (connection) {
            connection.close();
        }
    }
}
