package com.lifea.events.listeners;

import com.lifea.events.LifeaEvents;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {
   private final LifeaEvents plugin;

   public PlayerConnectionListener(LifeaEvents plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.plugin.getEventManager().handleDisconnect(event.getPlayer());
   }
}
