package com.lifea.events.listeners;

import com.lifea.events.LifeaEvents;
import com.lifea.events.managers.EventManager;
import com.lifea.events.minigames.MinigameType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class ProtectionListener implements Listener {
   private final LifeaEvents plugin;

   public ProtectionListener(LifeaEvents plugin) {
      this.plugin = plugin;
   }

   private boolean inPreLobby(Player p) {
      return this.plugin.getEventManager().isInPreLobby(p);
   }

   private boolean inActiveEvent(Player p) {
      return this.plugin.getEventManager().isInActiveEvent(p);
   }

   @EventHandler
   public void onDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player player) {
         if (this.inPreLobby(player)) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onDamageByEntity(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof Player victim && this.inPreLobby(victim)) {
         event.setCancelled(true);
         return;
      }

      if (event.getDamager() instanceof Player damager && this.inPreLobby(damager)) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onDrop(PlayerDropItemEvent event) {
      if (this.inPreLobby(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      if (this.inPreLobby(player)) {
         event.setCancelled(true);
      } else {
         if (this.inActiveEvent(player)) {
            EventManager em = this.plugin.getEventManager();
            boolean spleefSnow = em.getSelectedType() == MinigameType.SPLEEF
               && (event.getBlock().getType() == Material.SNOW_BLOCK || event.getBlock().getType() == Material.SNOW);
            if (!spleefSnow) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onPlace(BlockPlaceEvent event) {
      Player player = event.getPlayer();
      if (this.inPreLobby(player) || this.inActiveEvent(player)) {
         event.setCancelled(true);
      }
   }
}
