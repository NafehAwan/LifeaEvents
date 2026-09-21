package com.lifea.events.minigames;

import com.lifea.events.LifeaEvents;
import com.lifea.events.arena.Arena;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitTask;

public abstract class Minigame implements Listener {
   protected final LifeaEvents plugin;
   protected final Arena arena;
   protected final List<Player> alive = new ArrayList<>();
   protected final List<Player> spectators = new ArrayList<>();
   protected BukkitTask tickTask;
   protected boolean running = false;

   protected Minigame(LifeaEvents plugin, Arena arena) {
      this.plugin = plugin;
      this.arena = arena;
   }

   public abstract MinigameType getType();

   public abstract void start(List<Player> var1);

   public abstract void eliminate(Player var1, boolean var2);

   public abstract void end();

   public List<Player> getAlive() {
      return this.alive;
   }

   public List<Player> getSpectators() {
      return this.spectators;
   }

   public Map<String, String> scoreboardExtras() {
      return Map.of();
   }

   public boolean isRunning() {
      return this.running;
   }

   protected void register() {
      this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
   }

   protected void unregister() {
      HandlerList.unregisterAll(this);
   }

   protected void makeSpectator(Player player) {
      this.alive.remove(player);
      if (!this.spectators.contains(player)) {
         this.spectators.add(player);
      }

      player.setGameMode(GameMode.SPECTATOR);
      Location loc = this.spectatorLocation();
      if (loc != null) {
         player.teleport(loc);
      }

      this.plugin.getMessagesManager().send(player, "elimination.personal", "minigame", this.getType().displayName());
      this.plugin.getMessagesManager().send(player, "elimination.spectate-hint");
   }

   protected Location spectatorLocation() {
      return this.arena.getPreLobbySpawn();
   }

   protected void eliminateOnDeath(PlayerDeathEvent event) {
      Player player = event.getEntity();
      if (this.alive.contains(player)) {
         event.getDrops().clear();
         event.setDroppedExp(0);
         this.eliminate(player, true);
      }
   }

   @EventHandler
   public void onParticipantRespawn(PlayerRespawnEvent event) {
      Player player = event.getPlayer();
      if (this.spectators.contains(player)) {
         Location loc = this.spectatorLocation();
         if (loc != null) {
            event.setRespawnLocation(loc);
         }

         this.plugin.getServer().getScheduler().runTask(this.plugin, () -> player.setGameMode(GameMode.SPECTATOR));
      }
   }

   protected void stopTicking() {
      if (this.tickTask != null) {
         this.tickTask.cancel();
         this.tickTask = null;
      }
   }

   protected void announceWinner(Player winner) {
      if (winner != null) {
         this.plugin.getMessagesManager().broadcast("winner.announce", "minigame", this.getType().displayName(), "winner", winner.getName());
      } else {
         this.plugin.getMessagesManager().broadcast("winner.announce-none", "minigame", this.getType().displayName());
      }
   }

   protected void restoreAll() {
      for (Player p : new ArrayList<>(this.alive)) {
         this.restoreOne(p);
      }

      for (Player p : new ArrayList<>(this.spectators)) {
         this.restoreOne(p);
      }

      this.alive.clear();
      this.spectators.clear();
   }

   protected void restoreOne(Player p) {
      if (p != null) {
         p.setGameMode(GameMode.SURVIVAL);
         p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
         if (this.plugin.getPlayerStateManager().hasSnapshot(p)) {
            this.plugin.getPlayerStateManager().restore(p);
         }
      }
   }

   protected void finish(Player winner) {
      this.running = false;
      this.stopTicking();
      this.announceWinner(winner);
      this.restoreAll();
      this.unregister();
      this.plugin.getEventManager().onMinigameEnded();
   }
}
