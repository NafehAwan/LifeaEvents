package com.lifea.events.managers;

import com.lifea.events.LifeaEvents;
import com.lifea.events.arena.Arena;
import com.lifea.events.minigames.MinigameType;
import com.lifea.events.util.ItemBuilder;
import com.lifea.events.util.Lang;
import com.lifea.events.util.MessageUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class SetupWizardManager {
   private static final NamespacedKey WAND_KEY = new NamespacedKey("lifeaevents", "setup_wand");
   private static final long CLICK_COOLDOWN_MS = 350L;
   private final LifeaEvents plugin;
   private final Map<UUID, SetupWizardManager.WizardSession> sessions = new HashMap<>();
   private final Map<UUID, Long> lastClickMillis = new HashMap<>();

   public SetupWizardManager(LifeaEvents plugin) {
      this.plugin = plugin;
   }

   private List<String> buildSteps(MinigameType type) {
      List<String> steps = new ArrayList<>();
      steps.add("PRE_LOBBY");
      if (type.requiresBoundingBox()) {
         steps.add("CORNER_1");
         steps.add("CORNER_2");
      }

      steps.addAll(Arrays.asList(type.requiredSpawns()));
      return steps;
   }

   public void begin(Player player, MinigameType type) {
      SetupWizardManager.WizardSession session = new SetupWizardManager.WizardSession(type, this.buildSteps(type), type.displayName(), null);
      this.sessions.put(player.getUniqueId(), session);
      player.getInventory().addItem(new ItemStack[]{this.wandItem()});
      this.plugin.getMessagesManager().send(player, "setup.started", "minigame", type.displayName());
      this.announceStep(player, session);
   }

   public void beginCustom(Player player, String title, List<String> stepLabels, Consumer<List<Location>> onComplete) {
      SetupWizardManager.WizardSession session = new SetupWizardManager.WizardSession(null, stepLabels, title, onComplete);
      this.sessions.put(player.getUniqueId(), session);
      player.getInventory().addItem(new ItemStack[]{this.wandItem()});
      this.plugin.getMessagesManager().send(player, "setup.started", "minigame", title);
      this.announceStep(player, session);
   }

   private ItemStack wandItem() {
      return new ItemBuilder(Material.GOLDEN_AXE)
         .name("<red><b>" + Lang.SETUP_WAND_NAME + "</b></red>")
         .lore("<gray>" + Lang.SETUP_WAND_LORE + "</gray>")
         .glow(true)
         .pdc(WAND_KEY, PersistentDataType.BOOLEAN, true)
         .build();
   }

   public boolean isWand(ItemStack stack) {
      return stack != null && stack.hasItemMeta() ? stack.getItemMeta().getPersistentDataContainer().has(WAND_KEY, PersistentDataType.BOOLEAN) : false;
   }

   public boolean isInWizard(Player player) {
      return this.sessions.containsKey(player.getUniqueId());
   }

   private String stepLabel(SetupWizardManager.WizardSession session, String key) {
      return session.type != null ? Lang.stepLabel(key) : MessageUtil.smallCaps(key.replace("_", " "));
   }

   private boolean requiresLeftClick(int index) {
      return index % 2 == 0;
   }

   private String clickLabel(boolean left) {
      return left ? "Left" : "Right";
   }

   private void announceStep(Player player, SetupWizardManager.WizardSession session) {
      String key = session.steps.get(session.index);
      String label = this.stepLabel(session, key);
      String click = this.clickLabel(this.requiresLeftClick(session.index));
      this.plugin
         .getMessagesManager()
         .send(player, "setup.step", "index", String.valueOf(session.index + 1), "total", String.valueOf(session.steps.size()), "step", label);
      this.plugin.getMessagesManager().actionBar(player, "setup.step-actionbar", "step", label, "click", click);
   }

   public void handleClick(Player player, ItemStack wand, boolean isLeftClick) {
      SetupWizardManager.WizardSession session = this.sessions.get(player.getUniqueId());
      if (session != null) {
         boolean expectedLeft = this.requiresLeftClick(session.index);
         if (isLeftClick != expectedLeft) {
            this.plugin.getMessagesManager().send(player, "setup.wrong-click", "click", this.clickLabel(expectedLeft));
         } else {
            long now = System.currentTimeMillis();
            long last = this.lastClickMillis.getOrDefault(player.getUniqueId(), 0L);
            if (now - last >= 350L) {
               this.lastClickMillis.put(player.getUniqueId(), now);
               String key = session.steps.get(session.index);
               Location loc = player.getLocation();
               if (session.type != null) {
                  Arena arena = this.plugin.getArenaManager().getArena(session.type);
                  switch (key) {
                     case "PRE_LOBBY":
                        arena.setPreLobbySpawn(loc);
                        break;
                     case "CORNER_1":
                        arena.setCorner1(loc);
                        break;
                     case "CORNER_2":
                        arena.setCorner2(loc);
                        break;
                     default:
                        arena.setSpawn(key, loc);
                  }

                  this.plugin.getArenaManager().save();
               } else {
                  session.results.add(loc);
               }

               this.plugin.getMessagesManager().send(player, "setup.step-set", "step", this.stepLabel(session, key));
               session.index++;
               if (session.index >= session.steps.size()) {
                  this.sessions.remove(player.getUniqueId());
                  this.lastClickMillis.remove(player.getUniqueId());
                  player.getInventory().removeItem(new ItemStack[]{wand});
                  this.plugin.getMessagesManager().send(player, "setup.complete", "minigame", session.title);
                  if (session.onComplete != null) {
                     session.onComplete.accept(session.results);
                  }
               } else {
                  this.announceStep(player, session);
               }
            }
         }
      }
   }

   private static class WizardSession {
      final MinigameType type;
      final List<String> steps;
      final String title;
      final Consumer<List<Location>> onComplete;
      final List<Location> results = new ArrayList<>();
      int index = 0;

      WizardSession(MinigameType type, List<String> steps, String title, Consumer<List<Location>> onComplete) {
         this.type = type;
         this.steps = steps;
         this.title = title;
         this.onComplete = onComplete;
      }
   }
}
