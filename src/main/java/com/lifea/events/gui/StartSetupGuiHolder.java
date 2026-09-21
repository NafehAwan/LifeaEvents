package com.lifea.events.gui;

import com.lifea.events.minigames.MinigameType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class StartSetupGuiHolder implements InventoryHolder {
   private Inventory inventory;
   private final MinigameType type;

   public StartSetupGuiHolder(MinigameType type) {
      this.type = type;
   }

   public void setInventory(Inventory inventory) {
      this.inventory = inventory;
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   public MinigameType getType() {
      return this.type;
   }
}
