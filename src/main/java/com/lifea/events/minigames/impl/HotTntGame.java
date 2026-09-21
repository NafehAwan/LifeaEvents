package com.lifea.events.minigames.impl;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.lifea.events.LifeaEvents;
import com.lifea.events.arena.Arena;
import com.lifea.events.minigames.Minigame;
import com.lifea.events.minigames.MinigameType;
import com.lifea.events.util.ItemBuilder;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HotTntGame extends Minigame {
   private static final String TNT_HEAD_TEXTURE = "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2JkMzhhNjNkMTk1MjY0NjQzYjBkNGVhMjFlYWM1NDQ4ZGRjOWI0MGY3YTQxOWY2ZjhjZjhiNDEy0DEzWY0MSJ9fX0=";
   private final Random random = new Random();
   private Player holder;
   private int fuseSecondsLeft;
   private final int fuseLength;
   private int roundNumber = 0;

   public HotTntGame(LifeaEvents plugin, Arena arena) {
      super(plugin, arena);
      this.fuseLength = plugin.getConfigManager().getInt("minigames.hot_tnt.fuse-seconds", 20);
   }

   @Override
   public MinigameType getType() {
      return MinigameType.HOT_TNT;
   }

   @Override
   public void start(List<Player> players) {
      this.alive.addAll(players);
      this.running = true;
      this.register();
      this.plugin.getMessagesManager().broadcast("hot_tnt.begun", "minigame", this.getType().displayName(), "count", String.valueOf(this.alive.size()));
      this.startRound();
   }

   private void startRound() {
      if (this.alive.size() <= 1) {
         this.finish(this.alive.isEmpty() ? null : this.alive.get(0));
      } else {
         this.roundNumber++;
         Location spawn = this.arena.getSpawn("MAIN_SPAWN");

         for (Player p : this.alive) {
            p.getInventory().clear();
            p.getInventory().setHelmet(null);
            p.removePotionEffect(PotionEffectType.SPEED);
            if (spawn != null) {
               p.teleport(spawn);
            }
         }

         this.holder = this.alive.get(this.random.nextInt(this.alive.size()));
         this.giveTnt(this.holder);
         this.fuseSecondsLeft = this.fuseLength;
         this.plugin.getMessagesManager().broadcast("hot_tnt.round-started", "round", String.valueOf(this.roundNumber), "player", this.holder.getName());
         this.tickTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 20L, 20L);
      }
   }

   private void giveTnt(Player player) {
      player.getInventory().setHelmet(this.tntHead());
      player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, this.fuseLength * 20 + 40, 1, false, false));
   }

   private ItemStack tntHead() {
      ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta)skull.getItemMeta();

      try {
         PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "TNT");
         profile.setProperty(
            new ProfileProperty(
               "textures",
               "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2JkMzhhNjNkMTk1MjY0NjQzYjBkNGVhMjFlYWM1NDQ4ZGRjOWI0MGY3YTQxOWY2ZjhjZjhiNDEy0DEzWY0MSJ9fX0="
            )
         );
         meta.setPlayerProfile(profile);
      } catch (Exception var4) {
         this.plugin.getLogger().warning("Could not apply custom TNT head texture: " + var4.getMessage());
      }

      skull.setItemMeta(meta);
      return new ItemBuilder(skull).name("<red><b>HOT TNT</b></red>").lore("<gray>Punch someone to pass it on!</gray>").build();
   }

   private void transferTnt(Player from, Player to) {
      from.getInventory().setHelmet(null);
      from.removePotionEffect(PotionEffectType.SPEED);
      this.holder = to;
      this.giveTnt(to);
      this.plugin.getMessagesManager().broadcast("hot_tnt.passed", "player", to.getName());
   }

   private void tick() {
      if (this.running) {
         if (this.fuseSecondsLeft <= 0) {
            this.explode();
         } else {
            if (this.fuseSecondsLeft <= 5) {
               for (Player p : this.alive) {
                  this.plugin.getMessagesManager().actionBar(p, "hot_tnt.exploding-actionbar", "seconds", String.valueOf(this.fuseSecondsLeft));
               }
            }

            this.fuseSecondsLeft--;
         }
      }
   }

   private void explode() {
      this.stopTicking();
      Player exploded = this.holder;
      if (exploded != null && exploded.isOnline()) {
         exploded.getWorld().createExplosion(exploded.getLocation(), 0.0F, false, false);
         this.plugin.getMessagesManager().broadcast("hot_tnt.exploded", "player", exploded.getName());
         this.eliminate(exploded, false);
      }

      if (this.running) {
         this.startRound();
      }
   }

   @EventHandler
   public void onPunch(EntityDamageByEntityEvent event) {
      if (this.running) {
         if (event.getDamager() instanceof Player attacker) {
            if (event.getEntity() instanceof Player victim) {
               if (this.alive.contains(attacker) && this.alive.contains(victim)) {
                  if (attacker.equals(this.holder)) {
                     event.setCancelled(true);
                     this.transferTnt(attacker, victim);
                  }
               }
            }
         }
      }
   }

   @Override
   public Map<String, String> scoreboardExtras() {
      return Map.of("round", String.valueOf(this.roundNumber));
   }

   @Override
   public void eliminate(Player player, boolean broadcastMessage) {
      if (this.alive.contains(player)) {
         boolean wasHolder = player.equals(this.holder);
         this.makeSpectator(player);
         if (broadcastMessage) {
            this.plugin.getMessagesManager().broadcast("hot_tnt.eliminated", "player", player.getName(), "minigame", this.getType().displayName());
         }

         if (wasHolder) {
            this.holder = null;
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
