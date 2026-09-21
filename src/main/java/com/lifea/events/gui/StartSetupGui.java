package com.lifea.events.gui;

import com.lifea.events.LifeaEvents;
import com.lifea.events.arena.Arena;
import com.lifea.events.minigames.MinigameType;
import com.lifea.events.util.ItemBuilder;
import com.lifea.events.util.Lang;
import com.lifea.events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class StartSetupGui {
   public static final int INFO_SLOT = 13;
   public static final int START_SLOT = 22;
   public static final int BACK_SLOT = 18;

   private StartSetupGui() {
   }

   public static void open(LifeaEvents plugin, Player admin, MinigameType type) {
      StartSetupGuiHolder holder = new StartSetupGuiHolder(type);
      Inventory inv = Bukkit.createInventory(holder, 27, MessageUtil.parse("<red><b>" + type.smallCapsName() + "</b></red>"));
      holder.setInventory(inv);
      ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

      for (int i = 0; i < inv.getSize(); i++) {
         inv.setItem(i, filler);
      }

      Arena arena = plugin.getArenaManager().getArena(type);
      inv.setItem(
         13,
         new ItemBuilder(type.icon())
            .name("<red><b>" + type.smallCapsName() + "</b></red>")
            .lore(arena.isFullyConfigured() ? "<green><b>" + Lang.FULLY_CONFIGURED + "</b></green>" : "<yellow><b>" + Lang.INCOMPLETE_SETUP + "</b></yellow>")
            .glow(arena.isFullyConfigured())
            .build()
      );
      inv.setItem(18, new ItemBuilder(Material.ARROW).name("<white><b>" + Lang.BACK + "</b></white>").build());
      inv.setItem(
         22,
         new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
            .name("<green><b>" + Lang.START_SETUP + "</b></green>")
            .lore("<gray>" + Lang.START_SETUP_DESC + "</gray>")
            .glow(true)
            .build()
      );
      admin.openInventory(inv);
   }
}
