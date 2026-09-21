package com.lifea.events.minigames.impl;

import com.lifea.events.LifeaEvents;
import com.lifea.events.arena.Arena;
import com.lifea.events.minigames.Minigame;
import com.lifea.events.minigames.MinigameType;
import com.lifea.events.minigames.impl.mace.MaceArena;
import com.lifea.events.minigames.impl.mace.MaceRouletteMode;
import com.lifea.events.minigames.impl.mace.MaceTier;
import com.lifea.events.util.ItemBuilder;
import com.lifea.events.util.RegionSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

public class MaceRouletteGame extends Minigame {
   private static final int ROUND_TIMEOUT_TICKS = 900;
   private static final int CIRCLE_RADIUS = 5;
   private final Random random = new Random();
   private MaceTier tier;
   private MaceArena arenaTier;
   private int roundNumber = 0;
   private int roundToken = 0;
   private boolean currentlyDonut = false;
   private RegionSnapshot normalBackup;
   private MaceRouletteMode currentMode;
   private final Set<UUID> maceHolders = new HashSet<>();
   private final Map<UUID, Integer> windChargesLeft = new HashMap<>();
   private final Set<UUID> frozen = new HashSet<>();
   private boolean roundHitLanded = false;
   private boolean roundActive = false;
   private Location circleCenter;
   private BukkitTask musicTask;

   public MaceRouletteGame(LifeaEvents plugin, Arena arena) {
      super(plugin, arena);
   }

   @Override
   public MinigameType getType() {
      return MinigameType.MACE_ROULETTE;
   }

   @Override
   public void start(List<Player> players) {
      this.alive.addAll(players);
      this.running = true;
      this.register();
      this.tier = MaceTier.forPlayerCount(players.size());
      this.arenaTier = this.plugin.getMaceArenaManager().getArena(this.tier);
      this.plugin.getMessagesManager().broadcast("mace_roulette.begun", "minigame", this.getType().displayName(), "count", String.valueOf(this.alive.size()));
      this.startMatchMusic();
      this.startRound();
   }

   private void startMatchMusic() {
      for (Player p : this.alive) {
         this.plugin.getMusicManager().playMatchTrack(p);
      }

      this.musicTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         if (this.running) {
            for (Player px : this.alive) {
               this.plugin.getMusicManager().playMatchTrack(px);
            }
         }
      }, this.plugin.getMusicManager().trackLoopTicks(), this.plugin.getMusicManager().trackLoopTicks());
   }

   private void startRound() {
      if (this.running) {
         if (this.alive.size() <= 1) {
            this.restoreTerrainIfNeeded();
            Player winner = this.alive.isEmpty() ? null : this.alive.get(0);
            this.finish(winner);
         } else {
            this.roundNumber++;
            this.roundToken++;
            int myToken = this.roundToken;
            this.roundActive = false;
            this.roundHitLanded = false;
            this.maceHolders.clear();
            this.windChargesLeft.clear();
            this.currentMode = MaceRouletteMode.values()[this.random.nextInt(MaceRouletteMode.values().length)];
            this.swapTerrainIfNeeded(this.currentMode.isDonut());

            for (Player p : new ArrayList<>(this.alive)) {
               p.getInventory().clear();
               p.setHealth(p.getMaxHealth());
               p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
               p.setGameMode(GameMode.SURVIVAL);
            }

            this.circleCenter = this.arenaTier.getNormalSpawn();
            this.formCircle();
            this.playSpinAndReveal(myToken);
         }
      }
   }

   private void swapTerrainIfNeeded(boolean needsDonut) {
      if (needsDonut && !this.currentlyDonut) {
         this.normalBackup = RegionSnapshot.capture(this.arenaTier.getNormalCorner1(), this.arenaTier.getNormalCorner2());
         RegionSnapshot donut = this.plugin.getMaceArenaManager().getDonutSnapshot(this.tier);
         if (donut != null) {
            donut.paste(this.arenaTier.getNormalCorner1(), this.arenaTier.getNormalCorner2());
         }

         this.currentlyDonut = true;
      } else if (!needsDonut && this.currentlyDonut) {
         if (this.normalBackup != null) {
            this.normalBackup.paste(this.arenaTier.getNormalCorner1(), this.arenaTier.getNormalCorner2());
         }

         this.currentlyDonut = false;
      }
   }

   private void formCircle() {
      this.frozen.clear();
      List<Player> group = new ArrayList<>(this.alive);
      int count = group.size();

      for (int i = 0; i < count; i++) {
         double angle = (Math.PI * 2) / (double)count * (double)i;
         double x = this.circleCenter.getX() + 5.0 * Math.cos(angle);
         double z = this.circleCenter.getZ() + 5.0 * Math.sin(angle);
         Location spot = new Location(this.circleCenter.getWorld(), x, this.circleCenter.getY(), z);
         double dx = this.circleCenter.getX() - x;
         double dz = this.circleCenter.getZ() - z;
         float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
         spot.setYaw(yaw);
         Player p = group.get(i);
         p.teleport(spot);
         this.frozen.add(p.getUniqueId());
      }
   }

   private void playSpinAndReveal(int myToken) {
      MaceRouletteMode[] modes = MaceRouletteMode.values();
      int[] tickCounter = new int[]{0};
      int[] delayGrowth = new int[]{2};
      Runnable[] holder = new Runnable[1];
      holder[0] = () -> {
         if (myToken == this.roundToken && this.running) {
            tickCounter[0]++;
            MaceRouletteMode flash = modes[this.random.nextInt(modes.length)];

            for (Player p : this.alive) {
               this.plugin.getMessagesManager().actionBar(p, "mace_roulette.spin-actionbar", "mode", flash.label());
               this.plugin.getMusicManager().playSpinTick(p, Math.min(2.0F, 1.0F + (float)tickCounter[0] * 0.05F));
            }

            if (tickCounter[0] < 14) {
               delayGrowth[0] = Math.min(10, delayGrowth[0] + 1);
               Bukkit.getScheduler().runTaskLater(this.plugin, holder[0], (long)delayGrowth[0]);
            } else {
               this.revealMode(myToken);
            }
         }
      };
      Bukkit.getScheduler().runTaskLater(this.plugin, holder[0], 2L);
   }

   private void revealMode(int myToken) {
      if (myToken == this.roundToken && this.running) {
         for (Player p : this.alive) {
            this.plugin.getMusicManager().playSpinReveal(p);
         }

         this.plugin.getMessagesManager().broadcast("mace_roulette.mode-chosen", "mode", this.currentMode.label());
         this.startCountdown(myToken, 3);
      }
   }

   private void startCountdown(int myToken, int seconds) {
      if (myToken == this.roundToken && this.running) {
         if (seconds > 0) {
            for (Player p : this.alive) {
               this.plugin
                  .getMessagesManager()
                  .title(p, "mace_roulette.countdown-title", "mace_roulette.countdown-subtitle", 0, 20, 0, "seconds", String.valueOf(seconds));
               this.plugin.getMusicManager().playCountdownTick(p);
            }

            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.startCountdown(myToken, seconds - 1), 20L);
         } else {
            for (Player p : this.alive) {
               this.plugin.getMessagesManager().title(p, "mace_roulette.go-title", "mace_roulette.countdown-subtitle", 0, 15, 5);
               this.plugin.getMusicManager().playGo(p);
            }

            this.unlockRound(myToken);
         }
      }
   }

   private void unlockRound(int myToken) {
      if (myToken == this.roundToken && this.running) {
         this.frozen.clear();
         this.roundActive = true;
         this.plugin.getMessagesManager().broadcast("mace_roulette.round-started", "round", String.valueOf(this.roundNumber));
         this.applyMode(this.currentMode);
         this.tickTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::safetyNetCheck, 5L, 5L);
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.onRoundTimeout(myToken), 900L);
      }
   }

   private void applyMode(MaceRouletteMode mode) {
      if (mode.hasMace()) {
         List<Player> shuffled = new ArrayList<>(this.alive);
         Collections.shuffle(shuffled, this.random);
         int count = Math.min(mode.maceCount(), shuffled.size());

         for (int i = 0; i < count; i++) {
            Player holder = shuffled.get(i);
            this.maceHolders.add(holder.getUniqueId());
            this.windChargesLeft.put(holder.getUniqueId(), 5);
            holder.getInventory().addItem(new ItemStack[]{this.mace()});
            holder.getInventory().addItem(new ItemStack[]{new ItemStack(Material.WIND_CHARGE, 5)});
            this.plugin.getMessagesManager().broadcast("mace_roulette.got-mace", "player", holder.getName());
         }
      }
   }

   private ItemStack mace() {
      ItemStack stack = new ItemBuilder(Material.MACE).name("<red><b>Heavy Core Mace</b></red>").unbreakable(true).build();
      ItemMeta meta = stack.getItemMeta();
      meta.addEnchant(Enchantment.WIND_BURST, 2, true);
      stack.setItemMeta(meta);
      return stack;
   }

   @EventHandler
   public void onMove(PlayerMoveEvent event) {
      if (!this.frozen.isEmpty()) {
         Player p = event.getPlayer();
         if (this.frozen.contains(p.getUniqueId())) {
            if (!(event.getFrom().distanceSquared(event.getTo()) < 1.0E-4)) {
               Location to = event.getFrom().clone();
               to.setYaw(event.getTo().getYaw());
               to.setPitch(event.getTo().getPitch());
               event.setTo(to);
            }
         }
      }
   }

   @EventHandler
   public void onHit(EntityDamageByEntityEvent event) {
      if (this.roundActive && this.currentMode != null && this.currentMode.hasMace()) {
         if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) {
            return;
         }

         if (this.maceHolders.contains(attacker.getUniqueId())) {
            if (this.alive.contains(victim) && !victim.equals(attacker)) {
               ItemStack hand = attacker.getInventory().getItemInMainHand();
               if (hand.getType() == Material.MACE) {
                  this.roundHitLanded = true;
                  this.plugin.getMessagesManager().broadcast("mace_roulette.smashed", "attacker", attacker.getName(), "victim", victim.getName());
                  int myToken = this.roundToken;
                  Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.resolveSmash(victim, myToken), 20L);
               }
            }
         }
      }
   }

   private void resolveSmash(Player victim, int myToken) {
      if (myToken == this.roundToken && this.running) {
         if (this.alive.contains(victim)) {
            if (!this.hasFallen(victim) && !victim.isDead()) {
               this.plugin.getMessagesManager().broadcast("mace_roulette.survived", "player", victim.getName());
            } else {
               this.plugin.getMessagesManager().broadcast("mace_roulette.eliminated", "player", victim.getName());
               this.eliminate(victim, false);
            }

            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
               if (myToken == this.roundToken) {
                  this.startRound();
               }
            }, 40L);
         }
      }
   }

   @EventHandler
   public void onWindChargeThrow(ProjectileLaunchEvent event) {
      if (this.roundActive) {
         Projectile shooter = event.getEntity();
         if (shooter instanceof Projectile) {
            if (shooter.getType() == EntityType.WIND_CHARGE) {
               if (shooter.getShooter() instanceof Player shooterx) {
                  if (this.maceHolders.contains(shooterx.getUniqueId())) {
                     int remaining = this.windChargesLeft.getOrDefault(shooterx.getUniqueId(), 0) - 1;
                     this.windChargesLeft.put(shooterx.getUniqueId(), Math.max(0, remaining));
                     if (remaining <= 0 && !this.roundHitLanded && this.allChargesSpent()) {
                        int myToken = this.roundToken;
                        Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.onRoundSkipped(myToken), 20L);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean allChargesSpent() {
      for (int left : this.windChargesLeft.values()) {
         if (left > 0) {
            return false;
         }
      }

      return true;
   }

   private void onRoundSkipped(int myToken) {
      if (myToken == this.roundToken && this.running && !this.roundHitLanded) {
         for (UUID id : this.maceHolders) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
               this.plugin.getMessagesManager().broadcast("mace_roulette.round-skipped", "player", p.getName());
            }
         }

         this.startRound();
      }
   }

   private void onRoundTimeout(int myToken) {
      if (myToken == this.roundToken && this.running && !this.roundHitLanded) {
         this.startRound();
      }
   }

   private void safetyNetCheck() {
      if (this.roundActive) {
         for (Player p : new ArrayList<>(this.alive)) {
            if (this.hasFallen(p)) {
               this.plugin.getMessagesManager().broadcast("mace_roulette.eliminated", "player", p.getName());
               this.eliminate(p, false);
            }
         }
      }
   }

   private boolean hasFallen(Player p) {
      BoundingBox box = this.arenaTier.getNormalBoundingBox();
      return box == null ? false : p.getLocation().getY() < box.getMinY() - 2.0;
   }

   @EventHandler
   public void onDeath(PlayerDeathEvent event) {
      this.eliminateOnDeath(event);
   }

   @Override
   public Map<String, String> scoreboardExtras() {
      return Map.of("round", String.valueOf(this.roundNumber));
   }

   @Override
   public void eliminate(Player player, boolean broadcastMessage) {
      if (this.alive.contains(player)) {
         this.makeSpectator(player);
         if (broadcastMessage) {
            this.plugin.getMessagesManager().broadcast("elimination.broadcast-generic", "player", player.getName(), "minigame", this.getType().displayName());
         }

         if (this.alive.size() <= 1 && this.running) {
            this.restoreTerrainIfNeeded();
            Player winner = this.alive.isEmpty() ? null : this.alive.get(0);
            this.finish(winner);
         }
      }
   }

   private void restoreTerrainIfNeeded() {
      if (this.currentlyDonut && this.normalBackup != null && this.arenaTier != null) {
         this.normalBackup.paste(this.arenaTier.getNormalCorner1(), this.arenaTier.getNormalCorner2());
         this.currentlyDonut = false;
      }
   }

   @Override
   protected void finish(Player winner) {
      this.stopMatchMusic();
      super.finish(winner);
   }

   @Override
   public void end() {
      this.roundActive = false;
      this.stopTicking();
      this.stopMatchMusic();
      this.restoreTerrainIfNeeded();
      this.restoreAll();
      this.unregister();
      this.running = false;
   }

   private void stopMatchMusic() {
      if (this.musicTask != null) {
         this.musicTask.cancel();
         this.musicTask = null;
      }

      for (Player p : this.alive) {
         this.plugin.getMusicManager().stopAll(p);
      }

      for (Player p : this.spectators) {
         this.plugin.getMusicManager().stopAll(p);
      }
   }
}
