package com.lifea.events.minigames.impl;

import com.lifea.events.LifeaEvents;
import com.lifea.events.arena.Arena;
import com.lifea.events.minigames.Minigame;
import com.lifea.events.minigames.MinigameType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.BoundingBox;

public class RedLightGreenLightGame extends Minigame {
   private static final int INITIAL_RED_SECONDS = 3;
   private static final int PHASE_SECONDS = 8;
   private boolean greenLight = false;
   private int secondsLeft;
   private int secondsUntilToggle;
   private final int matchLength;
   private final List<Player> winners = new ArrayList<>();

   public RedLightGreenLightGame(LifeaEvents plugin, Arena arena) {
      super(plugin, arena);
      this.matchLength = plugin.getConfigManager().getInt("minigames.red_light_green_light.match-time-seconds", 180);
   }

   @Override
   public MinigameType getType() {
      return MinigameType.RED_LIGHT_GREEN_LIGHT;
   }

   @Override
   public void start(List<Player> players) {
      this.alive.addAll(players);
      this.running = true;
      this.register();
      this.spreadAlongStartLine(players);
      this.plugin
         .getMessagesManager()
         .broadcast("red_light_green_light.begun", "minigame", this.getType().displayName(), "count", String.valueOf(this.alive.size()));
      this.setRedLight();
      this.secondsLeft = this.matchLength;
      this.secondsUntilToggle = INITIAL_RED_SECONDS;
      this.tickTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 20L, 20L);
   }

   private void spreadAlongStartLine(List<Player> players) {
      Location c1 = this.arena.getSpawn("START_LINE_CORNER_1");
      Location c2 = this.arena.getSpawn("START_LINE_CORNER_2");

      for (int i = 0; i < players.size(); i++) {
         Player p = players.get(i);
         p.getInventory().clear();
         if (c1 != null && c2 != null) {
            double t = players.size() == 1 ? 0.5 : (double)i / (double)(players.size() - 1);
            double x = c1.getX() + (c2.getX() - c1.getX()) * t;
            double y = Math.max(c1.getY(), c2.getY());
            double z = c1.getZ() + (c2.getZ() - c1.getZ()) * t;
            Location spot = new Location(c1.getWorld(), x, y, z, c1.getYaw(), c1.getPitch());
            p.teleport(spot);
         }
      }
   }

   private void tick() {
      if (this.running) {
         if (this.secondsLeft <= 0) {
            this.concludeByTimer();
         } else {
            this.secondsLeft--;
            if (--this.secondsUntilToggle <= 0) {
               if (this.greenLight) {
                  this.setRedLight();
               } else {
                  this.setGreenLight();
               }

               this.secondsUntilToggle = PHASE_SECONDS;
            }
         }
      }
   }

   private void setGreenLight() {
      this.greenLight = true;

      for (Player p : this.alive) {
         this.plugin.getMessagesManager().title(p, "red_light_green_light.green-light-title", "red_light_green_light.green-light-subtitle", 5, 20, 5);
         p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
      }
   }

   private void setRedLight() {
      this.greenLight = false;

      for (Player p : this.alive) {
         this.plugin.getMessagesManager().title(p, "red_light_green_light.red-light-title", "red_light_green_light.red-light-subtitle", 5, 20, 5);
         p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.5F);
      }
   }

   @EventHandler
   public void onMove(PlayerMoveEvent event) {
      if (this.running) {
         Player player = event.getPlayer();
         if (this.alive.contains(player)) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
               this.checkFinishLine(player, event.getTo());
               if (!this.greenLight) {
                  player.getWorld().strikeLightningEffect(player.getLocation());
                  this.eliminate(player, true);
               }
            }
         }
      }
   }

   private void checkFinishLine(Player player, Location to) {
      Location c1 = this.arena.getSpawn("FINISH_LINE_CORNER_1");
      Location c2 = this.arena.getSpawn("FINISH_LINE_CORNER_2");
      if (c1 != null && c2 != null && c1.getWorld() != null) {
         BoundingBox box = BoundingBox.of(c1, c2).expand(0.0, 2.0, 0.0);
         if (box.contains(to.toVector())) {
            this.declareWinner(player);
         }
      }
   }

   private void declareWinner(Player player) {
      if (!this.winners.contains(player)) {
         this.winners.add(player);
         this.alive.remove(player);
         this.plugin.getMessagesManager().broadcast("red_light_green_light.reached-finish", "player", player.getName());
         if (this.winners.size() == 1) {
            this.stopTicking();
            this.finish(player);
         }
      }
   }

   private void concludeByTimer() {
      this.stopTicking();
      if (this.winners.isEmpty()) {
         this.plugin.getMessagesManager().broadcast("red_light_green_light.timer-no-winner", "minigame", this.getType().displayName());
         this.finish(null);
      } else {
         this.finish(this.winners.get(0));
      }
   }

   @Override
   public Map<String, String> scoreboardExtras() {
      int m = this.secondsLeft / 60;
      int s = this.secondsLeft % 60;
      return Map.of("phase", this.greenLight ? "Green Light" : "Red Light", "time", String.format("%d:%02d", m, s));
   }

   @Override
   public void eliminate(Player player, boolean broadcastMessage) {
      if (this.alive.contains(player)) {
         this.makeSpectator(player);
         if (broadcastMessage) {
            this.plugin.getMessagesManager().broadcast("red_light_green_light.eliminated", "player", player.getName());
         }

         if (this.alive.isEmpty() && this.running && this.winners.isEmpty()) {
            this.stopTicking();
            this.plugin.getMessagesManager().broadcast("red_light_green_light.all-eliminated", "minigame", this.getType().displayName());
            this.finish(null);
         }
      }
   }

   @Override
   public void end() {
      this.stopTicking();
      this.restoreAll();
      this.unregister();
      this.running = false;
   }
}
