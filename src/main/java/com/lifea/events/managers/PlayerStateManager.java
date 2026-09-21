package com.lifea.events.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PlayerStateManager {
   private final Map<UUID, PlayerStateManager.Snapshot> snapshots = new HashMap<>();

   public void capture(Player player) {
      PlayerStateManager.Snapshot snap = new PlayerStateManager.Snapshot();
      PlayerInventory inv = player.getInventory();
      snap.contents = (ItemStack[])inv.getContents().clone();
      snap.armor = (ItemStack[])inv.getArmorContents().clone();
      snap.offHand = inv.getItemInOffHand().clone();
      snap.health = player.getHealth();
      snap.foodLevel = player.getFoodLevel();
      snap.saturation = player.getSaturation();
      snap.xpLevel = player.getLevel();
      snap.xpProgress = player.getExp();
      snap.gameMode = player.getGameMode();
      snap.location = player.getLocation().clone();
      if (player.getAttribute(Attribute.SCALE) != null) {
         snap.scale = player.getAttribute(Attribute.SCALE).getBaseValue();
      } else {
         snap.scale = 1.0;
      }

      this.snapshots.put(player.getUniqueId(), snap);
   }

   public boolean hasSnapshot(Player player) {
      return this.snapshots.containsKey(player.getUniqueId());
   }

   public void restore(Player player) {
      PlayerStateManager.Snapshot snap = this.snapshots.remove(player.getUniqueId());
      if (snap != null) {
         PlayerInventory inv = player.getInventory();
         inv.clear();
         inv.setContents(snap.contents);
         inv.setArmorContents(snap.armor);
         inv.setItemInOffHand(snap.offHand);
         player.setHealth(Math.max(1.0, Math.min(snap.health, player.getMaxHealth())));
         player.setFoodLevel(snap.foodLevel);
         player.setSaturation(snap.saturation);
         player.setLevel(snap.xpLevel);
         player.setExp(snap.xpProgress);
         player.setGameMode(snap.gameMode);
         if (player.getAttribute(Attribute.SCALE) != null) {
            player.getAttribute(Attribute.SCALE).setBaseValue(snap.scale);
         }

         player.teleport(snap.location);
         player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
      }
   }

   public void resetScale(Player player) {
      if (player.getAttribute(Attribute.SCALE) != null) {
         player.getAttribute(Attribute.SCALE).setBaseValue(1.0);
      }
   }

   public void setScale(Player player, double scale) {
      if (player.getAttribute(Attribute.SCALE) != null) {
         player.getAttribute(Attribute.SCALE).setBaseValue(scale);
      }
   }

   public static class Snapshot {
      ItemStack[] contents;
      ItemStack[] armor;
      ItemStack offHand;
      double health;
      int foodLevel;
      float saturation;
      int xpLevel;
      float xpProgress;
      double scale;
      GameMode gameMode;
      Location location;
   }
}
