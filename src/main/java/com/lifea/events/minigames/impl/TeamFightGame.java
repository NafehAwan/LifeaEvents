package com.lifea.events.minigames.impl;

import com.lifea.events.LifeaEvents;
import com.lifea.events.arena.Arena;
import com.lifea.events.minigames.Minigame;
import com.lifea.events.minigames.MinigameType;
import com.lifea.events.util.ItemBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class TeamFightGame extends Minigame {
   private final Map<UUID, TeamFightGame.Team> teamOf = new HashMap<>();
   private int redScore = 0;
   private int blueScore = 0;
   private int secondsLeft;
   private final int matchLength;
   private final int targetScore;

   public TeamFightGame(LifeaEvents plugin, Arena arena) {
      super(plugin, arena);
      this.matchLength = plugin.getConfigManager().getInt("minigames.team_fight.match-time-seconds", 300);
      this.targetScore = plugin.getConfigManager().getInt("minigames.team_fight.target-score", 25);
   }

   @Override
   public MinigameType getType() {
      return MinigameType.TEAM_FIGHT;
   }

   @Override
   public void start(List<Player> players) {
      this.alive.addAll(players);
      this.running = true;
      this.register();
      List<Player> shuffled = new ArrayList<>(players);
      Collections.shuffle(shuffled);

      for (int i = 0; i < shuffled.size(); i++) {
         this.teamOf.put(shuffled.get(i).getUniqueId(), i % 2 == 0 ? TeamFightGame.Team.RED : TeamFightGame.Team.BLUE);
      }

      for (Player p : players) {
         this.gearUp(p);
      }

      this.secondsLeft = this.matchLength;
      this.plugin.getMessagesManager().broadcast("team_fight.begun", "minigame", this.getType().displayName(), "target", String.valueOf(this.targetScore));
      this.tickTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 20L, 20L);
   }

   private void gearUp(Player p) {
      p.getInventory().clear();
      TeamFightGame.Team team = this.teamOf.get(p.getUniqueId());
      DyeColor dye = team == TeamFightGame.Team.RED ? DyeColor.RED : DyeColor.BLUE;
      Color color = team == TeamFightGame.Team.RED ? Color.RED : Color.BLUE;
      p.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
      p.getInventory().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
      p.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
      ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
      LeatherArmorMeta meta = (LeatherArmorMeta)boots.getItemMeta();
      meta.setColor(color);
      boots.setItemMeta(meta);
      p.getInventory().setBoots(boots);
      p.getInventory()
         .addItem(
            new ItemStack[]{
               new ItemBuilder(Material.WOODEN_SWORD)
                  .name(
                     "<" + (team == TeamFightGame.Team.RED ? "red" : "blue") + "><b>Team Blade</b></" + (team == TeamFightGame.Team.RED ? "red" : "blue") + ">"
                  )
                  .enchant(Enchantment.SHARPNESS, 1)
                  .build()
            }
         );
      p.getInventory().addItem(new ItemStack[]{new ItemStack(Material.GOLDEN_APPLE, 1)});
      Location spawn = this.arena.getSpawn(team == TeamFightGame.Team.RED ? "RED_SPAWN" : "BLUE_SPAWN");
      if (spawn != null) {
         p.teleport(spawn);
      }

      p.setHealth(p.getMaxHealth());
      p.setFoodLevel(20);
   }

   private void tick() {
      if (this.running) {
         if (this.redScore >= this.targetScore || this.blueScore >= this.targetScore) {
            this.concludeByScore();
         } else if (this.secondsLeft <= 0) {
            this.concludeByTimer();
         } else {
            this.secondsLeft--;
         }
      }
   }

   private void concludeByScore() {
      TeamFightGame.Team winner = this.redScore >= this.targetScore ? TeamFightGame.Team.RED : TeamFightGame.Team.BLUE;
      this.stopTicking();
      this.plugin
         .getMessagesManager()
         .broadcast(
            "team_fight.team-wins-score",
            "team",
            winner == TeamFightGame.Team.RED ? "Red" : "Blue",
            "minigame",
            this.getType().displayName(),
            "red",
            String.valueOf(this.redScore),
            "blue",
            String.valueOf(this.blueScore)
         );
      this.finish(null);
   }

   private void concludeByTimer() {
      this.stopTicking();
      if (this.redScore > this.blueScore) {
         this.plugin.getMessagesManager().broadcast("team_fight.team-wins-timer", "team", "Red");
      } else if (this.blueScore > this.redScore) {
         this.plugin.getMessagesManager().broadcast("team_fight.team-wins-timer", "team", "Blue");
      } else {
         this.plugin
            .getMessagesManager()
            .broadcast(
               "team_fight.draw-timer", "minigame", this.getType().displayName(), "red", String.valueOf(this.redScore), "blue", String.valueOf(this.blueScore)
            );
      }

      this.finish(null);
   }

   @EventHandler
   public void onFriendlyFire(EntityDamageByEntityEvent event) {
      if (this.running && event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player victim) {
         if (this.alive.contains(attacker) && this.alive.contains(victim) && !attacker.equals(victim)) {
            TeamFightGame.Team attackerTeam = this.teamOf.get(attacker.getUniqueId());
            TeamFightGame.Team victimTeam = this.teamOf.get(victim.getUniqueId());
            if (attackerTeam != null && attackerTeam == victimTeam) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onDeath(PlayerDeathEvent event) {
      Player victim = event.getEntity();
      if (this.alive.contains(victim)) {
         event.getDrops().clear();
         event.setDroppedExp(0);
         Player killer = victim.getKiller();
         if (killer != null && this.alive.contains(killer)) {
            TeamFightGame.Team killerTeam = this.teamOf.get(killer.getUniqueId());
            TeamFightGame.Team victimTeam = this.teamOf.get(victim.getUniqueId());
            if (killerTeam != null && killerTeam != victimTeam) {
               if (killerTeam == TeamFightGame.Team.RED) {
                  this.redScore++;
               } else if (killerTeam == TeamFightGame.Team.BLUE) {
                  this.blueScore++;
               }

               killer.getInventory().addItem(new ItemStack[]{new ItemStack(Material.GOLDEN_APPLE, 1)});
            }
         }
      }
   }

   @EventHandler
   public void onRespawn(PlayerRespawnEvent event) {
      Player player = event.getPlayer();
      if (this.alive.contains(player)) {
         Location spawn = this.arena.getSpawn(this.teamOf.get(player.getUniqueId()) == TeamFightGame.Team.RED ? "RED_SPAWN" : "BLUE_SPAWN");
         if (spawn != null) {
            event.setRespawnLocation(spawn);
         }

         Bukkit.getScheduler().runTask(this.plugin, () -> this.gearUp(player));
      }
   }

   @Override
   public Map<String, String> scoreboardExtras() {
      int m = this.secondsLeft / 60;
      int s = this.secondsLeft % 60;
      return Map.of("red", String.valueOf(this.redScore), "blue", String.valueOf(this.blueScore), "time", String.format("%d:%02d", m, s));
   }

   @Override
   public void eliminate(Player player, boolean broadcastMessage) {
      this.alive.remove(player);
      this.teamOf.remove(player.getUniqueId());
      if (broadcastMessage) {
         this.plugin.getMessagesManager().broadcast("team_fight.left-event", "player", player.getName(), "minigame", this.getType().displayName());
      }

      if (this.alive.isEmpty() && this.running) {
         this.stopTicking();
         this.finish(null);
      }
   }

   @Override
   public void end() {
      this.stopTicking();
      this.restoreAll();
      this.unregister();
      this.running = false;
   }

   public static enum Team {
      RED,
      BLUE;
   }
}
