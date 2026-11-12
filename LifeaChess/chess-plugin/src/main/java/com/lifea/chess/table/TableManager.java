package com.lifea.chess.table;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Loads, saves and looks up chess tables (tables.yml). */
public final class TableManager {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, ChessTable> tables = new LinkedHashMap<>();
    private final Set<String> busy = new HashSet<>();

    public TableManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "tables.yml");
    }

    public void load() {
        tables.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("tables");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s != null) {
                tables.put(id.toLowerCase(Locale.ROOT), ChessTable.load(id.toLowerCase(Locale.ROOT), s));
            }
        }
        plugin.getLogger().info("Loaded " + tables.size() + " chess table(s).");
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        ConfigurationSection root = yml.createSection("tables");
        for (ChessTable t : tables.values()) {
            t.save(root.createSection(t.id()));
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save tables.yml: " + e.getMessage());
        }
    }

    public ChessTable get(String id) {
        return id == null ? null : tables.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<ChessTable> all() {
        return Collections.unmodifiableCollection(tables.values());
    }

    public List<String> ids() {
        return new ArrayList<>(tables.keySet());
    }

    public boolean exists(String id) {
        return get(id) != null;
    }

    public ChessTable create(String id, Location at) {
        String key = id.toLowerCase(Locale.ROOT);
        ChessTable t = new ChessTable(key, at.getWorld().getName());
        t.setBoard(at);
        tables.put(key, t);
        save();
        return t;
    }

    public boolean remove(String id) {
        ChessTable removed = tables.remove(id.toLowerCase(Locale.ROOT));
        if (removed != null) {
            busy.remove(removed.id());
            save();
        }
        return removed != null;
    }

    public boolean isBusy(ChessTable t) {
        return busy.contains(t.id());
    }

    public void setBusy(ChessTable t, boolean value) {
        if (value) {
            busy.add(t.id());
        } else {
            busy.remove(t.id());
        }
    }

    /** Nearest complete table in the same world within {@code maxDistance} blocks, or null. */
    public ChessTable nearest(Location from, double maxDistance) {
        ChessTable best = null;
        double bestDist = maxDistance * maxDistance;
        for (ChessTable t : tables.values()) {
            Location a = t.anchor();
            if (a == null || !t.isComplete() || a.getWorld() == null || !a.getWorld().equals(from.getWorld())) {
                continue;
            }
            double d = a.distanceSquared(from);
            if (d <= bestDist) {
                best = t;
                bestDist = d;
            }
        }
        return best;
    }

    /** A complete table nobody is using, preferring the one closest to {@code near}. */
    public ChessTable freeTable(Location near) {
        ChessTable best = null;
        double bestDist = Double.MAX_VALUE;
        for (ChessTable t : tables.values()) {
            if (!t.isComplete() || isBusy(t)) {
                continue;
            }
            Location a = t.anchor();
            double d = near != null && a != null && a.getWorld() != null && a.getWorld().equals(near.getWorld())
                    ? a.distanceSquared(near) : Double.MAX_VALUE / 2;
            if (best == null || d < bestDist) {
                best = t;
                bestDist = d;
            }
        }
        return best;
    }

    /** The chair whose block is {@code block}, if any. */
    public SeatRef seatAtBlock(Location block) {
        for (ChessTable t : tables.values()) {
            for (int i = 0; i < 2; i++) {
                Location c = t.chair(i);
                if (c != null && c.getWorld() != null && c.getWorld().equals(block.getWorld())
                        && c.getBlockX() == block.getBlockX() && c.getBlockY() == block.getBlockY()
                        && c.getBlockZ() == block.getBlockZ()) {
                    return new SeatRef(t, i);
                }
            }
        }
        return null;
    }
}
