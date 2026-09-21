package com.lifea.events.managers;

import com.lifea.events.LifeaEvents;
import com.lifea.events.gui.SpectatorMenuGui;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public class SpectatorManager {
   private static final long DOUBLE_TAP_WINDOW_MS = 400L;
   private final LifeaEvents plugin;
   private final Map<UUID, Long> lastShiftMillis = new HashMap<>();

   public SpectatorManager(LifeaEvents plugin) {
      this.plugin = plugin;
   }

   public void onSneak(Player player) {
      if (this.plugin.getEventManager().isSpectating(player)) {
         long now = System.currentTimeMillis();
         long last = this.lastShiftMillis.getOrDefault(player.getUniqueId(), 0L);
         this.lastShiftMillis.put(player.getUniqueId(), now);
         if (now - last <= 400L) {
            this.open(player);
         }
      }
   }

   public boolean canSpectate(Player player) {
      return this.plugin.getEventManager().isSpectating(player);
   }

   public void open(Player player) {
      if (!this.canSpectate(player)) {
         this.plugin.getMessagesManager().send(player, "spectate.not-spectating");
      } else {
         SpectatorMenuGui.open(this.plugin, player);
      }
   }

   public void teleportTo(Player spectator, Player target) {
      if (target != null && target.isOnline()) {
         spectator.teleport(target.getLocation());
         this.plugin.getMessagesManager().send(spectator, "spectate.teleported", "player", target.getName());
      } else {
         this.plugin.getMessagesManager().send(spectator, "spectate.target-unavailable");
      }
   }
}
