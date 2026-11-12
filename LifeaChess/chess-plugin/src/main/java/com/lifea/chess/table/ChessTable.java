package com.lifea.chess.table;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * A physical chess table: two chairs (index 0 = White, 1 = Black) and the point
 * above which the big public board floats. Chairs face each other across the board.
 */
public final class ChessTable {
    public static final int WHITE_SEAT = 0;
    public static final int BLACK_SEAT = 1;

    private final String id;
    private String worldName;
    private final Location[] chairs = new Location[2];
    private Location board;

    public ChessTable(String id, String worldName) {
        this.id = id;
        this.worldName = worldName;
    }

    public String id() {
        return id;
    }

    public String worldName() {
        return worldName;
    }

    public World world() {
        return Bukkit.getWorld(worldName);
    }

    public Location chair(int index) {
        return chairs[index] == null ? null : chairs[index].clone();
    }

    public void setChair(int index, Location loc) {
        chairs[index] = loc.clone();
        worldName = loc.getWorld().getName();
    }

    public Location board() {
        return board == null ? null : board.clone();
    }

    public void setBoard(Location loc) {
        board = loc.clone();
        worldName = loc.getWorld().getName();
    }

    public boolean isComplete() {
        return chairs[0] != null && chairs[1] != null && board != null;
    }

    /** Where players are teleported to / distance is measured from. */
    public Location anchor() {
        if (board != null) {
            return board.clone();
        }
        return chairs[0] != null ? chairs[0].clone() : chairs[1] == null ? null : chairs[1].clone();
    }

    /**
     * Yaw the board should face: perpendicular to the chair-to-chair axis so both
     * players look at it edge-on from their sides, with White reading it upright.
     */
    public float boardYaw() {
        if (chairs[0] != null && chairs[1] != null) {
            double dx = chairs[1].getX() - chairs[0].getX();
            double dz = chairs[1].getZ() - chairs[0].getZ();
            return (float) Math.toDegrees(Math.atan2(-dx, dz));
        }
        return board != null ? board.getYaw() : 0f;
    }

    public void save(ConfigurationSection s) {
        s.set("world", worldName);
        saveLoc(s.createSection("chair-white"), chairs[0]);
        saveLoc(s.createSection("chair-black"), chairs[1]);
        saveLoc(s.createSection("board"), board);
    }

    public static ChessTable load(String id, ConfigurationSection s) {
        ChessTable t = new ChessTable(id, s.getString("world", "world"));
        t.chairs[0] = loadLoc(s.getConfigurationSection("chair-white"));
        t.chairs[1] = loadLoc(s.getConfigurationSection("chair-black"));
        t.board = loadLoc(s.getConfigurationSection("board"));
        return t;
    }

    private static void saveLoc(ConfigurationSection s, Location l) {
        if (l == null || l.getWorld() == null) {
            return;
        }
        s.set("world", l.getWorld().getName());
        s.set("x", l.getX());
        s.set("y", l.getY());
        s.set("z", l.getZ());
        s.set("yaw", l.getYaw());
        s.set("pitch", l.getPitch());
    }

    private static Location loadLoc(ConfigurationSection s) {
        if (s == null || !s.isSet("world")) {
            return null;
        }
        World w = Bukkit.getWorld(s.getString("world"));
        if (w == null) {
            return null;
        }
        return new Location(w, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
    }
}
