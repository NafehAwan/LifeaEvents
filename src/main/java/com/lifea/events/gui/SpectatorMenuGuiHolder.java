package com.lifea.events.gui;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class SpectatorMenuGuiHolder implements InventoryHolder {
   private Inventory inventory;
   private final Map<Integer, Player> slotToPlayer = new HashMap<>();

   public void setInventory(Inventory inventory) {
      this.inventory = inventory;
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   public void map(int slot, Player player) {
      this.slotToPlayer.put(slot, player);
   }

   public Player get(int slot) {
      return this.slotToPlayer.get(slot);
   }
}
