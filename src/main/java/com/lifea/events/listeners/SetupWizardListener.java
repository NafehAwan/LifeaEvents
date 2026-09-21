package com.lifea.events.listeners;

import com.lifea.events.LifeaEvents;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SetupWizardListener implements Listener {
   private final LifeaEvents plugin;

   public SetupWizardListener(LifeaEvents plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onInteract(PlayerInteractEvent event) {
      Action action = event.getAction();
      boolean isLeft = action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR;
      boolean isRight = action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR;
      if (isLeft || isRight) {
         Player player = event.getPlayer();
         ItemStack item = event.getItem();
         if (this.plugin.getSetupWizardManager().isWand(item)) {
            if (this.plugin.getSetupWizardManager().isInWizard(player)) {
               event.setCancelled(true);
               this.plugin.getSetupWizardManager().handleClick(player, item, isLeft);
            }
         }
      }
   }
}
