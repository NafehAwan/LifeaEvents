package com.lifea.chess.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Seats players on chairs by mounting them on an invisible, non-persistent
 * armour stand. One occupant per chair; everything is cleaned up on stand-up,
 * quit, death, world change and plugin disable.
 */
public final class SeatService {
    private final JavaPlugin plugin;
    private final NamespacedKey seatKey;
    private final Map<UUID, Seat> byPlayer = new HashMap<>();
    private final Map<String, Seat[]> byTable = new HashMap<>();

    private record Seat(SeatRef ref, ArmorStand mount, UUID player) {
    }

    public SeatService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.seatKey = new NamespacedKey(plugin, "seat");
    }

    public boolean sit(Player player, ChessTable table, int index) {
        Location chair = table.chair(index);
        if (chair == null || chair.getWorld() == null || occupant(table, index) != null) {
            return false;
        }
        stand(player);
        Location mountLoc = chair.clone().add(0, -0.45, 0);
        mountLoc.setYaw(faceBoardYaw(table, chair));
        mountLoc.setPitch(0f);
        ArmorStand mount = chair.getWorld().spawn(mountLoc, ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setGravity(false);
            as.setMarker(true);
            as.setSmall(true);
            as.setInvulnerable(true);
            as.setPersistent(false);
            as.setCollidable(false);
            as.setSilent(true);
            as.setBasePlate(false);
            as.getPersistentDataContainer().set(seatKey, PersistentDataType.BYTE, (byte) 1);
        });
        player.teleport(mountLoc);
        if (!mount.addPassenger(player)) {
            mount.remove();
            return false;
        }
        Seat seat = new Seat(new SeatRef(table, index), mount, player.getUniqueId());
        byPlayer.put(player.getUniqueId(), seat);
        byTable.computeIfAbsent(table.id(), k -> new Seat[2])[index] = seat;
        return true;
    }

    private static float faceBoardYaw(ChessTable table, Location chair) {
        Location b = table.board() != null ? table.board() : table.anchor();
        if (b == null) {
            return chair.getYaw();
        }
        double dx = b.getX() - chair.getX();
        double dz = b.getZ() - chair.getZ();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** Removes the player's seat (if seated) and returns it, or null. */
    public SeatRef stand(Player player) {
        Seat seat = byPlayer.remove(player.getUniqueId());
        if (seat == null) {
            return null;
        }
        Seat[] slots = byTable.get(seat.ref().table().id());
        if (slots != null) {
            slots[seat.ref().index()] = null;
        }
        if (seat.mount() != null && seat.mount().isValid()) {
            seat.mount().removePassenger(player);
            seat.mount().remove();
        }
        return seat.ref();
    }

    public SeatRef seatOf(Player player) {
        Seat s = byPlayer.get(player.getUniqueId());
        return s == null ? null : s.ref();
    }

    public boolean isSeated(Player player) {
        return byPlayer.containsKey(player.getUniqueId());
    }

    public Player occupant(ChessTable table, int index) {
        Seat[] slots = byTable.get(table.id());
        if (slots == null || slots[index] == null) {
            return null;
        }
        Player p = plugin.getServer().getPlayer(slots[index].player());
        if (p == null || !p.isOnline()) {
            slots[index] = null;
            return null;
        }
        return p;
    }

    public boolean isSeatMount(Entity e) {
        return e instanceof ArmorStand && e.getPersistentDataContainer().has(seatKey, PersistentDataType.BYTE);
    }

    public void standAt(ChessTable table) {
        for (int i = 0; i < 2; i++) {
            Player p = occupant(table, i);
            if (p != null) {
                stand(p);
            }
        }
    }

    public void standAll() {
        for (UUID id : new ArrayList<>(byPlayer.keySet())) {
            Player p = plugin.getServer().getPlayer(id);
            if (p != null) {
                stand(p);
            } else {
                Seat s = byPlayer.remove(id);
                if (s != null && s.mount() != null) {
                    s.mount().remove();
                }
            }
        }
        // Sweep any orphaned mounts left from a crash.
        for (org.bukkit.World w : plugin.getServer().getWorlds()) {
            for (ArmorStand as : w.getEntitiesByClass(ArmorStand.class)) {
                if (isSeatMount(as)) {
                    as.remove();
                }
            }
        }
    }
}
