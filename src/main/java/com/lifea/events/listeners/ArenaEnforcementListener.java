package com.lifea.events.listeners;

import com.lifea.events.LifeaEvents;
import com.lifea.events.arena.Arena;
import com.lifea.events.minigames.GameState;
import com.lifea.events.minigames.MinigameType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class ArenaEnforcementListener implements Listener {
   private final LifeaEvents plugin;

   public ArenaEnforcementListener(LifeaEvents plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onMove(PlayerMoveEvent event) {
      if (this.plugin.getEventManager().getState() == GameState.ACTIVE) {
         Player player = event.getPlayer();
         if (this.plugin.getEventManager().isInActiveEvent(player)) {
            MinigameType type = this.plugin.getEventManager().getSelectedType();
            if (type != null && type != MinigameType.MACE_ROULETTE && type != MinigameType.SPLEEF && type.requiresBoundingBox()) {
               Arena arena = this.plugin.getArenaManager().getArena(type);
               BoundingBox box = arena.getBoundingBox();
               if (box != null) {
                  Location to = event.getTo();
                  if (!box.contains(to.toVector())) {
                     Vector clamped = new Vector(
                        Math.max(box.getMinX(), Math.min(box.getMaxX(), to.getX())),
                        Math.max(box.getMinY(), Math.min(box.getMaxY(), to.getY())),
                        Math.max(box.getMinZ(), Math.min(box.getMaxZ(), to.getZ()))
                     );
                     Location clampedLoc = to.clone();
                     clampedLoc.setX(clamped.getX());
                     clampedLoc.setY(clamped.getY());
                     clampedLoc.setZ(clamped.getZ());
                     event.setTo(clampedLoc);
                  }
               }
            }
         }
      }
   }
}
