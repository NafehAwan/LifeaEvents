package com.lifea.events.managers;

import com.lifea.events.LifeaEvents;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
   private final LifeaEvents plugin;

   public ConfigManager(LifeaEvents plugin) {
      this.plugin = plugin;
      plugin.saveDefaultConfig();
   }

   private FileConfiguration cfg() {
      return this.plugin.getConfig();
   }

   public int countdownSeconds() {
      return this.cfg().getInt("pregame.countdown-seconds", 90);
   }

   public List<Integer> alertSeconds() {
      return this.cfg().getIntegerList("pregame.alert-seconds");
   }

   public int reminderIntervalSeconds() {
      return this.cfg().getInt("pregame.reminder-interval-seconds", 20);
   }

   public int minPlayers() {
      return this.cfg().getInt("defaults.min-players", 10);
   }

   public int defaultMaxPlayers() {
      return this.cfg().getInt("defaults.max-players", 32);
   }

   public int minLimit() {
      return this.cfg().getInt("defaults.min-limit", 2);
   }

   public int maxLimit() {
      return this.cfg().getInt("defaults.max-limit", 100);
   }

   public int getInt(String path, int def) {
      return this.cfg().getInt(path, def);
   }

   public void reload() {
      this.plugin.reloadConfig();
   }
}
