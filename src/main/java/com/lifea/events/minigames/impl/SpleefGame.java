package com.lifea.events.minigames.impl;

import com.lifea.events.LifeaEvents;
import com.lifea.events.arena.Arena;
import com.lifea.events.minigames.Minigame;
import com.lifea.events.minigames.MinigameType;
import com.lifea.events.util.ItemBuilder;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class SpleefGame extends Minigame {
   private final int matchLength;
   private int secondsLeft;

   public SpleefGame(LifeaEvents plugin, Arena arena) {
      super(plugin, arena);
      this.matchLength = plugin.getConfigManager().getInt("minigames.spleef.match-time-seconds", 300);
   }

   @Override
   public MinigameType getType() {
      return MinigameType.SPLEEF;
   }

   @Override
   public void start(List<Player> players) {
      this.alive.addAll(players);
      this.running = true;
      this.register();
      Location spawn = this.arena.getSpawn("MAIN_SPAWN");

      for (Player p : players) {
         p.getInventory().clear();
         p.getInventory().addItem(new ItemStack[]{this.shovel()});
         if (spawn != null) {
            p.teleport(spawn);
         }
      }

      this.plugin.getMessagesManager().broadcast("spleef.begun", "minigame", this.getType().displayName(), "count", String.valueOf(this.alive.size()));
      this.secondsLeft = this.matchLength;
      this.tickTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 20L, 20L);
   }

   private ItemStack shovel() {
      return new ItemBuilder(Material.DIAMOND_SHOVEL).name("<red><b>Spleef Shovel</b></red>").enchant(Enchantment.EFFICIENCY, 5).unbreakable(true).build();
   }

   private void tick() {
      if (this.running) {
         if (this.secondsLeft <= 0) {
            this.stopTicking();
            this.plugin.getMessagesManager().broadcast("spleef.draw", "minigame", this.getType().displayName());
            this.finish(null);
         } else {
            this.secondsLeft--;
         }
      }
   }

   @EventHandler
   public void onBreak(BlockBreakEvent event) {
      if (this.running) {
         Player player = event.getPlayer();
         if (this.alive.contains(player)) {
            Material type = event.getBlock().getType();
            if (type != Material.SNOW_BLOCK && type != Material.SNOW) {
               event.setCancelled(true);
            } else {
               event.setDropItems(false);
            }
         }
      }
   }

   @EventHandler
   public void onFall(PlayerMoveEvent event) {
      if (this.running) {
         Player player = event.getPlayer();
         if (this.alive.contains(player)) {
            Location spawn = this.arena.getSpawn("MAIN_SPAWN");
            if (spawn != null && player.getWorld() == spawn.getWorld()) {
               if (player.getLocation().getY() < spawn.getY() - 10.0) {
                  this.eliminate(player, true);
               }
            }
         }
      }
   }

   @EventHandler
   public void onDeath(PlayerDeathEvent event) {
      this.eliminateOnDeath(event);
   }

   @Override
   public Map<String, String> scoreboardExtras() {
      int m = this.secondsLeft / 60;
      int s = this.secondsLeft % 60;
      return Map.of("time", String.format("%d:%02d", m, s));
   }

   @Override
   public void eliminate(Player player, boolean broadcastMessage) {
      if (this.alive.contains(player)) {
         this.makeSpectator(player);
         if (broadcastMessage) {
            this.plugin.getMessagesManager().broadcast("spleef.eliminated", "player", player.getName(), "minigame", this.getType().displayName());
         }

         if (this.alive.size() <= 1 && this.running) {
            this.stopTicking();
            this.finish(this.alive.isEmpty() ? null : this.alive.get(0));
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
