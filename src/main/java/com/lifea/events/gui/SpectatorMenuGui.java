package com.lifea.events.gui;

import com.lifea.events.LifeaEvents;
import com.lifea.events.minigames.Minigame;
import com.lifea.events.util.ItemBuilder;
import com.lifea.events.util.MessageUtil;
import java.util.List;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public final class SpectatorMenuGui {
   private SpectatorMenuGui() {
   }

   public static void open(LifeaEvents plugin, Player spectator) {
      Minigame minigame = plugin.getEventManager().getActiveMinigame();
      if (minigame == null) {
         MessageUtil.send(spectator, "<red>There is no active event to spectate.</red>");
      } else {
         SpectatorMenuGuiHolder holder = new SpectatorMenuGuiHolder();
         int rows = Math.max(3, Math.min(6, minigame.getAlive().size() / 9 + 2));
         Inventory inv = Bukkit.createInventory(holder, rows * 9, MessageUtil.parse("<color:#FF1A1A><b>" + MessageUtil.smallCaps("Spectate") + "</b></color>"));
         holder.setInventory(inv);
         ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

         for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
         }

         int slot = 0;

         for (Player target : minigame.getAlive()) {
            if (slot >= inv.getSize()) {
               break;
            }

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta)head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.displayName(MessageUtil.parse("<color:#FF1A1A><b>" + target.getName() + "</b></color>").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(MessageUtil.parse("<gray>" + MessageUtil.smallCaps("Click to teleport") + "</gray>").decoration(TextDecoration.ITALIC, false)));
            head.setItemMeta(meta);
            inv.setItem(slot, head);
            holder.map(slot, target);
            slot++;
         }

         if (minigame.getAlive().isEmpty()) {
            MessageUtil.send(spectator, "<gray>No remaining players to spectate right now.</gray>");
         }

         spectator.openInventory(inv);
      }
   }
}
