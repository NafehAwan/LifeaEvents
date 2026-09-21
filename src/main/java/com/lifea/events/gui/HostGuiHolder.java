package com.lifea.events.gui;

import com.lifea.events.minigames.MinigameType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class HostGuiHolder implements InventoryHolder {
   private Inventory inventory;
   private final Player host;
   private MinigameType selected;
   private int minPlayers;
   private int maxPlayers;
   private int countdownSeconds;

   public HostGuiHolder(Player host, int defaultMin, int defaultMax, int defaultCountdown) {
      this.host = host;
      this.minPlayers = defaultMin;
      this.maxPlayers = defaultMax;
      this.countdownSeconds = defaultCountdown;
   }

   public void setInventory(Inventory inventory) {
      this.inventory = inventory;
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   public Player getHost() {
      return this.host;
   }

   public MinigameType getSelected() {
      return this.selected;
   }

   public void setSelected(MinigameType selected) {
      this.selected = selected;
   }

   public int getMinPlayers() {
      return this.minPlayers;
   }

   public void setMinPlayers(int minPlayers) {
      this.minPlayers = minPlayers;
   }

   public int getMaxPlayers() {
      return this.maxPlayers;
   }

   public void setMaxPlayers(int maxPlayers) {
      this.maxPlayers = maxPlayers;
   }

   public int getCountdownSeconds() {
      return this.countdownSeconds;
   }

   public void setCountdownSeconds(int countdownSeconds) {
      this.countdownSeconds = countdownSeconds;
   }
}
