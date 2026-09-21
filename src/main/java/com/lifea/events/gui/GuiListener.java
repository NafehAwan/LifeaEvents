package com.lifea.events.gui;

import com.lifea.events.LifeaEvents;
import com.lifea.events.minigames.MinigameType;
import com.lifea.events.minigames.impl.mace.MaceArena;
import com.lifea.events.minigames.impl.mace.MaceTier;
import com.lifea.events.util.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class GuiListener implements Listener {
   private final LifeaEvents plugin;

   public GuiListener(LifeaEvents plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         InventoryHolder holder = event.getInventory().getHolder();
         if (holder instanceof MainMenuGuiHolder) {
            event.setCancelled(true);
            this.handleMainMenuClick(player, event.getSlot());
         } else if (holder instanceof HostGuiHolder hostHolder) {
            event.setCancelled(true);
            this.handleHostClick(player, hostHolder, event.getSlot());
         } else if (holder instanceof SetupGuiHolder) {
            event.setCancelled(true);
            this.handleSetupClick(player, event.getSlot());
         } else if (holder instanceof StartSetupGuiHolder startHolder) {
            event.setCancelled(true);
            this.handleStartSetupClick(player, startHolder, event.getSlot());
         } else if (holder instanceof MaceSetupGuiHolder) {
            event.setCancelled(true);
            this.handleMaceSetupClick(player, event.getSlot());
         }
      }
   }

   private void handleMainMenuClick(Player player, int slot) {
      if (slot == 11 && player.hasPermission("lifeaevents.host")) {
         HostGui.open(this.plugin, player);
      } else if (slot == 15 && player.hasPermission("lifeaevents.admin")) {
         SetupGui.open(this.plugin, player);
      }
   }

   private void handleHostClick(Player player, HostGuiHolder holder, int slot) {
      for (int i = 0; i < HostGui.MINIGAME_SLOTS.length; i++) {
         if (HostGui.MINIGAME_SLOTS[i] == slot) {
            holder.setSelected(MinigameType.values()[i]);
            HostGui.render(this.plugin, holder);
            return;
         }
      }

      if (slot == 28) {
         HostGui.adjustMin(this.plugin, holder, -1);
      } else if (slot == 30) {
         HostGui.adjustMin(this.plugin, holder, 1);
      } else if (slot == 37) {
         HostGui.adjustMax(this.plugin, holder, -10);
      } else if (slot == 38) {
         HostGui.adjustMax(this.plugin, holder, -1);
      } else if (slot == 40) {
         HostGui.adjustMax(this.plugin, holder, 1);
      } else if (slot == 41) {
         HostGui.adjustMax(this.plugin, holder, 10);
      } else if (slot == 19) {
         HostGui.adjustCountdown(this.plugin, holder, -10);
      } else if (slot == 20) {
         HostGui.adjustCountdown(this.plugin, holder, -1);
      } else if (slot == 22) {
         HostGui.adjustCountdown(this.plugin, holder, 1);
      } else if (slot == 23) {
         HostGui.adjustCountdown(this.plugin, holder, 10);
      } else if (slot == 43) {
         if (holder.getSelected() == null) {
            MessageUtil.send(player, "<red>Select a minigame before confirming.</red>");
            return;
         }

         player.closeInventory();
         boolean started = this.plugin
            .getEventManager()
            .beginHost(player, holder.getSelected(), holder.getMinPlayers(), holder.getMaxPlayers(), holder.getCountdownSeconds());
         if (started) {
            MessageUtil.send(player, "<green>Event announced! Players can now join with /event join.</green>");
         }
      }
   }

   private void handleSetupClick(Player player, int slot) {
      for (int i = 0; i < SetupGui.MINIGAME_SLOTS.length; i++) {
         if (SetupGui.MINIGAME_SLOTS[i] == slot) {
            MinigameType type = MinigameType.values()[i];
            if (type == MinigameType.MACE_ROULETTE) {
               MaceSetupGui.open(this.plugin, player);
            } else {
               StartSetupGui.open(this.plugin, player, type);
            }

            return;
         }
      }
   }

   private void handleStartSetupClick(Player player, StartSetupGuiHolder holder, int slot) {
      if (slot == 18) {
         SetupGui.open(this.plugin, player);
      } else {
         if (slot == 22) {
            player.closeInventory();
            this.plugin.getSetupWizardManager().begin(player, holder.getType());
         }
      }
   }

   private void handleMaceSetupClick(Player player, int slot) {
      if (slot == 18) {
         SetupGui.open(this.plugin, player);
      } else {
         if (slot == 22) {
            player.closeInventory();
            this.startFullMaceWizard(player);
         }
      }
   }

   private void startFullMaceWizard(Player player) {
      List<String> steps = new ArrayList<>();
      steps.add("PRE_LOBBY");

      for (MaceTier tier : MaceTier.values()) {
         String prefix = tier.name() + "_";
         steps.add(prefix + "NORMAL_CORNER_1");
         steps.add(prefix + "NORMAL_CORNER_2");
         steps.add(prefix + "NORMAL_SPAWN");
         steps.add(prefix + "DONUT_CORNER_1");
         steps.add(prefix + "DONUT_CORNER_2");
      }

      this.plugin
         .getSetupWizardManager()
         .beginCustom(
            player,
            MinigameType.MACE_ROULETTE.displayName(),
            steps,
            locations -> {
               this.plugin.getArenaManager().getArena(MinigameType.MACE_ROULETTE).setPreLobbySpawn(locations.get(0));
               this.plugin.getArenaManager().save();
               MaceTier[] tiers = MaceTier.values();
               List<String> sizeMismatches = new ArrayList<>();

               for (int t = 0; t < tiers.length; t++) {
                  int base = 1 + t * 5;
                  MaceArena arena = this.plugin.getMaceArenaManager().getArena(tiers[t]);
                  arena.setNormalCorner1(locations.get(base));
                  arena.setNormalCorner2(locations.get(base + 1));
                  arena.setNormalSpawn(locations.get(base + 2));
                  arena.setDonutCorner1(locations.get(base + 3));
                  arena.setDonutCorner2(locations.get(base + 4));
                  this.plugin.getMaceArenaManager().invalidateDonutSnapshot(tiers[t]);
                  if (!this.sameSize(arena.getNormalCorner1(), arena.getNormalCorner2(), arena.getDonutCorner1(), arena.getDonutCorner2())) {
                     sizeMismatches.add(tiers[t].label());
                  }
               }

               this.plugin.getMaceArenaManager().save();
               if (!sizeMismatches.isEmpty()) {
                  MessageUtil.send(
                     player,
                     "<red><b>Warning:</b> the donut source region is a DIFFERENT SIZE than the normal arena for: <white>"
                        + String.join(", ", sizeMismatches)
                        + "</white>. Donut rounds for those tiers will only partially swap (or crop) the arena until the two regions match exactly - re-run Start Setup for those tiers with matching dimensions.</red>"
                  );
               }
            }
         );
   }

   private boolean sameSize(Location a1, Location a2, Location b1, Location b2) {
      if (a1 != null && a2 != null && b1 != null && b2 != null) {
         int ax = Math.abs(a1.getBlockX() - a2.getBlockX());
         int ay = Math.abs(a1.getBlockY() - a2.getBlockY());
         int az = Math.abs(a1.getBlockZ() - a2.getBlockZ());
         int bx = Math.abs(b1.getBlockX() - b2.getBlockX());
         int by = Math.abs(b1.getBlockY() - b2.getBlockY());
         int bz = Math.abs(b1.getBlockZ() - b2.getBlockZ());
         return ax == bx && ay == by && az == bz;
      } else {
         return true;
      }
   }
}
