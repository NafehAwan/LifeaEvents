package com.lifea.events.gui;

import com.lifea.events.LifeaEvents;
import com.lifea.events.util.ItemBuilder;
import com.lifea.events.util.Lang;
import com.lifea.events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class MainMenuGui {
   public static final int HOST_SLOT = 11;
   public static final int SETUP_SLOT = 15;

   private MainMenuGui() {
   }

   public static void open(LifeaEvents plugin, Player player) {
      MainMenuGuiHolder holder = new MainMenuGuiHolder();
      Inventory inv = Bukkit.createInventory(holder, 27, MessageUtil.parse(MessageUtil.brandBold()));
      holder.setInventory(inv);
      ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

      for (int i = 0; i < inv.getSize(); i++) {
         inv.setItem(i, filler);
      }

      if (player.hasPermission("lifeaevents.host")) {
         inv.setItem(
            11,
            new ItemBuilder(Material.NETHER_STAR)
               .name("<color:#FF1A1A><b>" + Lang.HOST_EVENT + "</b></color>")
               .lore("<gray>Pick a minigame and announce a new event.</gray>")
               .glow(true)
               .build()
         );
      }

      if (player.hasPermission("lifeaevents.admin")) {
         inv.setItem(
            15,
            new ItemBuilder(Material.IRON_AXE)
               .name("<color:#FF1A1A><b>" + Lang.ARENA_SETUP + "</b></color>")
               .lore("<gray>Configure arenas with the guided setup wand.</gray>")
               .glow(true)
               .build()
         );
      }

      player.openInventory(inv);
   }
}
