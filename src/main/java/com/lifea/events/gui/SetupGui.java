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

public final class SetupGui {
   public static final int[] MINIGAME_SLOTS = new int[]{11, 12, 13, 14, 15, 22};

   private SetupGui() {
   }

   public static void open(LifeaEvents plugin, Player admin) {
      SetupGuiHolder holder = new SetupGuiHolder();
      Inventory inv = Bukkit.createInventory(holder, 36, MessageUtil.parse("<red><b>" + Lang.ARENA_SETUP + "</b></red>"));
      holder.setInventory(inv);
      ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

      for (int i = 0; i < inv.getSize(); i++) {
         inv.setItem(i, filler);
      }

      MinigameType[] types = MinigameType.values();

      for (int i = 0; i < types.length; i++) {
         MinigameType type = types[i];
         Arena arena = plugin.getArenaManager().getArena(type);
         boolean configured = arena.isFullyConfigured();
         if (type == MinigameType.MACE_ROULETTE) {
            configured = configured && plugin.getMaceArenaManager().isFullyConfigured();
         }

         inv.setItem(
            MINIGAME_SLOTS[i],
            new ItemBuilder(type.icon())
               .name("<red><b>" + type.smallCapsName() + "</b></red>")
               .lore(
                  configured ? "<green><b>" + Lang.FULLY_CONFIGURED + "</b></green>" : "<yellow><b>" + Lang.INCOMPLETE_SETUP + "</b></yellow>",
                  "",
                  "<gray>" + Lang.CLICK_TO_CONFIGURE + "</gray>"
               )
               .glow(configured)
               .build()
         );
      }

      admin.openInventory(inv);
   }
}
