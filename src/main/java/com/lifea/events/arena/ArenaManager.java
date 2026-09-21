package com.lifea.events.arena;

import com.lifea.events.LifeaEvents;
import com.lifea.events.minigames.MinigameType;
import com.lifea.events.util.LocationUtil;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

public class ArenaManager {
   private final LifeaEvents plugin;
   private final File file;
   private final Map<MinigameType, Arena> arenas = new EnumMap<>(MinigameType.class);

   public ArenaManager(LifeaEvents plugin) {
      this.plugin = plugin;
      this.file = new File(plugin.getDataFolder(), "arenas.yml");

      for (MinigameType type : MinigameType.values()) {
         this.arenas.put(type, new Arena(type));
      }

      this.load();
   }

   public Arena getArena(MinigameType type) {
      return this.arenas.get(type);
   }

   public void load() {
      if (this.file.exists()) {
         YamlConfiguration yml = YamlConfiguration.loadConfiguration(this.file);

         for (MinigameType type : MinigameType.values()) {
            String base = type.name();
            if (yml.getConfigurationSection(base) != null) {
               Arena arena = this.arenas.get(type);
               arena.setPreLobbySpawn(LocationUtil.load(yml, base + ".pre_lobby"));
               arena.setCorner1(LocationUtil.load(yml, base + ".corner1"));
               arena.setCorner2(LocationUtil.load(yml, base + ".corner2"));

               for (String key : type.requiredSpawns()) {
                  Location loc = LocationUtil.load(yml, base + ".spawns." + key);
                  if (loc != null) {
                     arena.setSpawn(key, loc);
                  }
               }
            }
         }
      }
   }

   public void save() {
      YamlConfiguration yml = new YamlConfiguration();

      for (MinigameType type : MinigameType.values()) {
         Arena arena = this.arenas.get(type);
         String base = type.name();
         if (arena.getPreLobbySpawn() != null) {
            LocationUtil.save(yml, base + ".pre_lobby", arena.getPreLobbySpawn());
         }

         if (arena.getCorner1() != null) {
            LocationUtil.save(yml, base + ".corner1", arena.getCorner1());
         }

         if (arena.getCorner2() != null) {
            LocationUtil.save(yml, base + ".corner2", arena.getCorner2());
         }

         for (Entry<String, Location> e : arena.getSpawns().entrySet()) {
            LocationUtil.save(yml, base + ".spawns." + e.getKey(), e.getValue());
         }
      }

      try {
         if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
         }

         yml.save(this.file);
      } catch (IOException var10) {
         this.plugin.getLogger().severe("Failed to save arenas.yml: " + var10.getMessage());
      }
   }
}
