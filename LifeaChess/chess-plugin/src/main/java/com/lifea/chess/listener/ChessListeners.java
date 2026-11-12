package com.lifea.chess.listener;

import com.lifea.chess.LifeaChessPlugin;
import com.lifea.chess.table.SeatRef;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

/** Bukkit events: seating via chair clicks, standing up, connection handling and menu clicks. */
public final class ChessListeners implements Listener {
    private final LifeaChessPlugin plugin;

    public ChessListeners(LifeaChessPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.stats().preload(e.getPlayer());
        plugin.matches().onJoin(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.matches().onQuit(e.getPlayer());
        plugin.stats().evict(e.getPlayer().getUniqueId());
    }

    /** Right-clicking a chair block sits the player down. */
    @EventHandler(ignoreCancelled = true)
    public void onChairClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) {
            return;
        }
        if (e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }
        Block block = e.getClickedBlock();
        SeatRef seat = plugin.tables().seatAtBlock(block.getLocation());
        if (seat == null) {
            return;
        }
        e.setCancelled(true);
        Player player = e.getPlayer();
        if (player.getLocation().distanceSquared(block.getLocation().add(0.5, 0.5, 0.5)) > 16) {
            return;
        }
        plugin.matches().sitAt(player, seat.table(), seat.index());
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) {
            return;
        }
        Player p = e.getPlayer();
        if (plugin.seats().isSeated(p)) {
            SeatRef seat = plugin.seats().stand(p);
            plugin.matches().onStoodUp(p, seat);
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent e) {
        if (!(e.getEntity() instanceof Player p) || !plugin.seats().isSeatMount(e.getDismounted())) {
            return;
        }
        if (plugin.seats().isSeated(p)) {
            SeatRef seat = plugin.seats().stand(p);
            plugin.matches().onStoodUp(p, seat);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (plugin.seats().isSeated(p)) {
            SeatRef seat = plugin.seats().stand(p);
            plugin.matches().onStoodUp(p, seat);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        if (plugin.seats().isSeated(p)) {
            SeatRef seat = plugin.seats().stand(p);
            plugin.matches().onStoodUp(p, seat);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSeatInteract(PlayerInteractAtEntityEvent e) {
        if (plugin.seats().isSeatMount(e.getRightClicked())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSeatManipulate(PlayerArmorStandManipulateEvent e) {
        if (plugin.seats().isSeatMount(e.getRightClicked())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSeatDamage(EntityDamageEvent e) {
        if (plugin.seats().isSeatMount(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMenuClick(InventoryClickEvent e) {
        plugin.menus().handleClick(e);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMenuDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() != null
                && e.getInventory().getHolder().getClass().getName().startsWith("com.lifea.chess.ui.MenuService")) {
            e.setCancelled(true);
        }
    }
}
