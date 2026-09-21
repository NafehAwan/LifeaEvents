package com.lifea.events.listeners;

import com.lifea.events.LifeaEvents;
import com.lifea.events.gui.SpectatorMenuGuiHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class SpectatorListener implements Listener {
   private final LifeaEvents plugin;

   public SpectatorListener(LifeaEvents plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onToggleSneak(PlayerToggleSneakEvent event) {
      if (event.isSneaking()) {
         this.plugin.getSpectatorManager().onSneak(event.getPlayer());
      }
   }

   @EventHandler
   public void onMenuClick(InventoryClickEvent event) {
      if (event.getInventory().getHolder() instanceof SpectatorMenuGuiHolder holder) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player spectator) {
            Player target = holder.get(event.getSlot());
            if (target != null) {
               this.plugin.getSpectatorManager().teleportTo(spectator, target);
            }
         }
      }
   }
}
