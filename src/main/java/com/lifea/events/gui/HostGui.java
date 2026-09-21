package com.lifea.events.gui;

import com.lifea.events.LifeaEvents;
import com.lifea.events.minigames.MinigameType;
import com.lifea.events.util.ItemBuilder;
import com.lifea.events.util.Lang;
import com.lifea.events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class HostGui {
   public static final int[] MINIGAME_SLOTS = new int[]{10, 11, 12, 14, 15, 16};
   public static final int MIN_MINUS_1 = 28;
   public static final int MIN_DISPLAY = 29;
   public static final int MIN_PLUS_1 = 30;
   public static final int MAX_MINUS_10 = 37;
   public static final int MAX_MINUS_1 = 38;
   public static final int MAX_DISPLAY = 39;
   public static final int MAX_PLUS_1 = 40;
   public static final int MAX_PLUS_10 = 41;
   public static final int COUNTDOWN_MINUS_10 = 19;
   public static final int COUNTDOWN_MINUS_1 = 20;
   public static final int COUNTDOWN_DISPLAY = 21;
   public static final int COUNTDOWN_PLUS_1 = 22;
   public static final int COUNTDOWN_PLUS_10 = 23;
   public static final int CONFIRM_SLOT = 43;

   private HostGui() {
   }

   public static void open(LifeaEvents plugin, Player host) {
      HostGuiHolder holder = new HostGuiHolder(
         host, plugin.getConfigManager().minPlayers(), plugin.getConfigManager().defaultMaxPlayers(), plugin.getConfigManager().countdownSeconds()
      );
      Inventory inv = Bukkit.createInventory(holder, 45, MessageUtil.parse("<red><b>" + Lang.HOST_EVENT + "</b></red>"));
      holder.setInventory(inv);
      render(plugin, holder);
      host.openInventory(inv);
   }

   public static void render(LifeaEvents plugin, HostGuiHolder holder) {
      Inventory inv = holder.getInventory();
      ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

      for (int i = 0; i < inv.getSize(); i++) {
         inv.setItem(i, filler);
      }

      MinigameType[] types = MinigameType.values();

      for (int i = 0; i < types.length; i++) {
         MinigameType type = types[i];
         boolean selected = type == holder.getSelected();
         ItemBuilder builder = new ItemBuilder(type.icon())
            .name("<red><b>" + type.smallCapsName() + "</b></red>")
            .lore(selected ? "<green><b>✔ " + Lang.SELECTED + "</b></green>" : "<gray>" + Lang.NOT_SELECTED + "</gray>")
            .glow(selected);
         inv.setItem(MINIGAME_SLOTS[i], builder.build());
      }

      inv.setItem(28, new ItemBuilder(Material.RED_DYE).name("<red><b>-1</b></red>").build());
      inv.setItem(
         29,
         new ItemBuilder(Material.OAK_SIGN)
            .name("<white><b>" + Lang.MIN_PLAYERS + "</b></white>")
            .lore("<red><b>" + holder.getMinPlayers() + "</b></red>")
            .build()
      );
      inv.setItem(30, new ItemBuilder(Material.LIME_DYE).name("<green><b>+1</b></green>").build());
      inv.setItem(37, new ItemBuilder(Material.RED_DYE).name("<red><b>-10</b></red>").build());
      inv.setItem(38, new ItemBuilder(Material.RED_DYE).name("<red><b>-1</b></red>").build());
      inv.setItem(
         39,
         new ItemBuilder(Material.PLAYER_HEAD)
            .name("<white><b>" + Lang.MAX_PLAYERS + "</b></white>")
            .lore("<red><b>" + holder.getMaxPlayers() + "</b></red>")
            .build()
      );
      inv.setItem(40, new ItemBuilder(Material.LIME_DYE).name("<green><b>+1</b></green>").build());
      inv.setItem(41, new ItemBuilder(Material.LIME_DYE).name("<green><b>+10</b></green>").build());
      inv.setItem(19, new ItemBuilder(Material.RED_DYE).name("<red><b>-10s</b></red>").build());
      inv.setItem(20, new ItemBuilder(Material.RED_DYE).name("<red><b>-1s</b></red>").build());
      inv.setItem(
         21, new ItemBuilder(Material.CLOCK).name("<white><b>Starting In</b></white>").lore("<red><b>" + holder.getCountdownSeconds() + "s</b></red>").build()
      );
      inv.setItem(22, new ItemBuilder(Material.LIME_DYE).name("<green><b>+1s</b></green>").build());
      inv.setItem(23, new ItemBuilder(Material.LIME_DYE).name("<green><b>+10s</b></green>").build());
      boolean ready = holder.getSelected() != null;
      ItemBuilder confirm = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
         .name("<green><b>" + Lang.CONFIRM_AND_ANNOUNCE + "</b></green>")
         .lore(
            "<white>Minigame: <red>" + (ready ? holder.getSelected().smallCapsName() : "None") + "</red></white>",
            "<white>" + Lang.MIN_PLAYERS + ": <red>" + holder.getMinPlayers() + "</red></white>",
            "<white>" + Lang.MAX_PLAYERS + ": <red>" + holder.getMaxPlayers() + "</red></white>",
            "<white>Starting In: <red>" + holder.getCountdownSeconds() + "s</red></white>",
            "",
            ready ? "<green>" + Lang.CLICK_TO_ANNOUNCE + "</green>" : "<red>" + Lang.SELECT_A_MINIGAME_FIRST + "</red>"
         )
         .glow(ready);
      inv.setItem(43, confirm.build());
   }

   public static void adjustMin(LifeaEvents plugin, HostGuiHolder holder, int delta) {
      int newVal = Math.max(2, Math.min(holder.getMaxPlayers(), holder.getMinPlayers() + delta));
      holder.setMinPlayers(newVal);
      render(plugin, holder);
   }

   public static void adjustMax(LifeaEvents plugin, HostGuiHolder holder, int delta) {
      int min = plugin.getConfigManager().minLimit();
      int max = plugin.getConfigManager().maxLimit();
      int newVal = Math.max(Math.max(min, holder.getMinPlayers()), Math.min(max, holder.getMaxPlayers() + delta));
      holder.setMaxPlayers(newVal);
      render(plugin, holder);
   }

   public static void adjustCountdown(LifeaEvents plugin, HostGuiHolder holder, int delta) {
      int newVal = Math.max(10, Math.min(3600, holder.getCountdownSeconds() + delta));
      holder.setCountdownSeconds(newVal);
      render(plugin, holder);
   }
}
