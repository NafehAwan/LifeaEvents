package com.lifea.events.gui;

import com.lifea.events.LifeaEvents;
import com.lifea.events.minigames.MinigameType;
import com.lifea.events.minigames.impl.mace.MaceTier;
import com.lifea.events.util.ItemBuilder;
import com.lifea.events.util.Lang;
import com.lifea.events.util.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class MaceSetupGui {
   public static final int INFO_SLOT = 13;
   public static final int START_SLOT = 22;
   public static final int BACK_SLOT = 18;

   private MaceSetupGui() {
   }

   public static void open(LifeaEvents plugin, Player admin) {
      MaceSetupGuiHolder holder = new MaceSetupGuiHolder();
      Inventory inv = Bukkit.createInventory(holder, 27, MessageUtil.parse("<red><b>" + MinigameType.MACE_ROULETTE.smallCapsName() + "</b></red>"));
      holder.setInventory(inv);
      ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

      for (int i = 0; i < inv.getSize(); i++) {
         inv.setItem(i, filler);
      }

      boolean preLobbySet = plugin.getArenaManager().getArena(MinigameType.MACE_ROULETTE).getPreLobbySpawn() != null;
      boolean fullyConfigured = preLobbySet && plugin.getMaceArenaManager().isFullyConfigured();
      List<String> lore = new ArrayList<>();
      lore.add(fullyConfigured ? "<green><b>" + Lang.FULLY_CONFIGURED + "</b></green>" : "<yellow><b>" + Lang.INCOMPLETE_SETUP + "</b></yellow>");
      lore.add((preLobbySet ? "<green>✔</green>" : "<red>✘</red>") + " <white>Pre-Lobby Spawn</white>");

      for (MaceTier tier : MaceTier.values()) {
         boolean done = plugin.getMaceArenaManager().getArena(tier).isFullyConfigured();
         lore.add((done ? "<green>✔</green>" : "<red>✘</red>") + " <white>" + tier.label() + "</white>");
      }

      inv.setItem(
         13,
         new ItemBuilder(MinigameType.MACE_ROULETTE.icon())
            .name("<red><b>" + MinigameType.MACE_ROULETTE.smallCapsName() + "</b></red>")
            .lore(lore.toArray(new String[0]))
            .glow(fullyConfigured)
            .build()
      );
      inv.setItem(18, new ItemBuilder(Material.ARROW).name("<white><b>" + Lang.BACK + "</b></white>").build());
      inv.setItem(
         22,
         new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
            .name("<green><b>" + Lang.START_SETUP + "</b></green>")
            .lore("<gray>Walks through the pre-lobby spawn and all 5 arena tiers in one guided flow.</gray>")
            .glow(true)
            .build()
      );
      admin.openInventory(inv);
   }
}
