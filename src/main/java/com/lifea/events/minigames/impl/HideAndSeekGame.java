package com.lifea.events.minigames.impl;

import com.lifea.events.LifeaEvents;
import com.lifea.events.arena.Arena;
import com.lifea.events.minigames.Minigame;
import com.lifea.events.minigames.MinigameType;
import com.lifea.events.util.ItemBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HideAndSeekGame extends Minigame {
   private final Set<UUID> seekers = new HashSet<>();
   private boolean hidingPhase = true;
   private int secondsLeft;
   private final int hidingPhaseSeconds;
   private final int matchLength;

   public HideAndSeekGame(LifeaEvents plugin, Arena arena) {
      super(plugin, arena);
      this.hidingPhaseSeconds = plugin.getConfigManager().getInt("minigames.hide_and_seek.hiding-phase-seconds", 20);
      this.matchLength = plugin.getConfigManager().getInt("minigames.hide_and_seek.match-time-seconds", 300);
   }

   @Override
   public MinigameType getType() {
      return MinigameType.HIDE_AND_SEEK;
   }

   @Override
   public void start(List<Player> players) {
      this.alive.addAll(players);
      this.running = true;
      this.register();
      int seekerCount = Math.min(Math.max(1, this.plugin.getConfigManager().getInt("minigames.hide_and_seek.seekers", 1)), Math.max(1, players.size() - 1));
      List<Player> shuffled = new ArrayList<>(players);
      Collections.shuffle(shuffled);

      for (int i = 0; i < seekerCount; i++) {
         this.seekers.add(shuffled.get(i).getUniqueId());
      }

      Location seekersSpawn = this.arena.getSpawn("SEEKERS_SPAWN");
      Location hidersSpawn = this.arena.getSpawn("HIDERS_SPAWN");

      for (Player p : players) {
         boolean isSeeker = this.seekers.contains(p.getUniqueId());
         p.teleport(isSeeker ? seekersSpawn : hidersSpawn);
         p.getInventory().clear();
         if (isSeeker) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, this.hidingPhaseSeconds * 20, 1, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, this.hidingPhaseSeconds * 20, 4, false, false));
         }
      }

      this.plugin
         .getMessagesManager()
         .broadcast("hide_and_seek.begun", "minigame", this.getType().displayName(), "seconds", String.valueOf(this.hidingPhaseSeconds));
      this.hidingPhase = true;
      this.secondsLeft = this.hidingPhaseSeconds;
      this.tickTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 20L, 20L);
   }

   private void tick() {
      if (this.running) {
         if (this.secondsLeft <= 0) {
            if (this.hidingPhase) {
               this.beginSeekingPhase();
            } else {
               this.concludeByTimer();
            }
         } else {
            this.secondsLeft--;
         }
      }
   }

   private void beginSeekingPhase() {
      this.hidingPhase = false;
      this.secondsLeft = this.matchLength;

      for (Player p : this.alive) {
         if (this.seekers.contains(p.getUniqueId())) {
            p.removePotionEffect(PotionEffectType.BLINDNESS);
            p.removePotionEffect(PotionEffectType.SLOWNESS);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, this.matchLength * 20, 0, false, false));
            p.getInventory().setHelmet(this.seekerHelmet());
         }
      }

      this.plugin.getMessagesManager().broadcast("hide_and_seek.seeking-phase-begun");

      for (Player px : this.alive) {
         this.plugin.getMessagesManager().title(px, "hide_and_seek.seeking-title", "hide_and_seek.seeking-subtitle", 5, 20, 5);
      }
   }

   private ItemStack seekerHelmet() {
      ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
      LeatherArmorMeta meta = (LeatherArmorMeta)helmet.getItemMeta();
      meta.setColor(Color.BLACK);
      helmet.setItemMeta(meta);
      return new ItemBuilder(helmet).name("<red><b>Seeker</b></red>").build();
   }

   @EventHandler
   public void onFrozenMove(PlayerMoveEvent event) {
      if (this.running && this.hidingPhase) {
         Player player = event.getPlayer();
         if (this.seekers.contains(player.getUniqueId())) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX()
               || event.getFrom().getBlockY() != event.getTo().getBlockY()
               || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onTag(EntityDamageByEntityEvent event) {
      if (this.running && !this.hidingPhase) {
         if (event.getDamager() instanceof Player seeker) {
            if (event.getEntity() instanceof Player hider) {
               if (this.seekers.contains(seeker.getUniqueId()) && !this.seekers.contains(hider.getUniqueId())) {
                  event.setCancelled(true);
                  this.convertToSeeker(hider);
               }
            }
         }
      }
   }

   private void convertToSeeker(Player hider) {
      this.seekers.add(hider.getUniqueId());
      hider.getInventory().clear();
      hider.getInventory().setHelmet(this.seekerHelmet());
      hider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, this.matchLength * 20, 0, false, false));
      this.plugin.getMessagesManager().broadcast("hide_and_seek.tagged", "player", hider.getName());
      long hidersLeft = this.alive.stream().filter(p -> !this.seekers.contains(p.getUniqueId())).count();
      if (hidersLeft == 0L) {
         this.stopTicking();
         this.plugin.getMessagesManager().broadcast("hide_and_seek.seekers-win-all-found");
         this.finish(null);
      }
   }

   private void concludeByTimer() {
      this.stopTicking();
      long hidersLeft = this.alive.stream().filter(p -> !this.seekers.contains(p.getUniqueId())).count();
      if (hidersLeft > 0L) {
         this.plugin.getMessagesManager().broadcast("hide_and_seek.hiders-win-timer");
      } else {
         this.plugin.getMessagesManager().broadcast("hide_and_seek.seekers-win-timer");
      }

      this.finish(null);
   }

   @Override
   public Map<String, String> scoreboardExtras() {
      long hidersLeft = this.alive.stream().filter(p -> !this.seekers.contains(p.getUniqueId())).count();
      return Map.of("phase", this.hidingPhase ? "Hiding" : "Seeking", "hiders", String.valueOf(hidersLeft), "seekers", String.valueOf(this.seekers.size()));
   }

   @Override
   public void eliminate(Player player, boolean broadcastMessage) {
      this.alive.remove(player);
      this.seekers.remove(player.getUniqueId());
      if (broadcastMessage) {
         this.plugin.getMessagesManager().broadcast("hide_and_seek.left-event", "player", player.getName(), "minigame", this.getType().displayName());
      }

      if (this.running) {
         long hidersLeft = this.alive.stream().filter(p -> !this.seekers.contains(p.getUniqueId())).count();
         if (hidersLeft == 0L && !this.hidingPhase) {
            this.stopTicking();
            this.plugin.getMessagesManager().broadcast("hide_and_seek.seekers-win-no-hiders");
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
