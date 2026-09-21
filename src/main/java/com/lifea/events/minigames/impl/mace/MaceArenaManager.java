package com.lifea.events.minigames.impl.mace;

import com.lifea.events.LifeaEvents;
import com.lifea.events.util.LocationUtil;
import com.lifea.events.util.RegionSnapshot;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

public class MaceArenaManager {
   private final LifeaEvents plugin;
   private final File file;
   private final Map<MaceTier, MaceArena> arenas = new EnumMap<>(MaceTier.class);
   private final Map<MaceTier, RegionSnapshot> donutSnapshotCache = new HashMap<>();

   public MaceArenaManager(LifeaEvents plugin) {
      this.plugin = plugin;
      this.file = new File(plugin.getDataFolder(), "mace_arenas.yml");

      for (MaceTier tier : MaceTier.values()) {
         this.arenas.put(tier, new MaceArena(tier));
      }

      this.load();
   }

   public MaceArena getArena(MaceTier tier) {
      return this.arenas.get(tier);
   }

   public boolean isFullyConfigured() {
      for (MaceArena arena : this.arenas.values()) {
         if (!arena.isFullyConfigured()) {
            return false;
         }
      }

      return true;
   }

   public RegionSnapshot getDonutSnapshot(MaceTier tier) {
      return this.donutSnapshotCache.computeIfAbsent(tier, t -> {
         MaceArena arena = this.arenas.get(t);
         return !arena.isDonutConfigured() ? null : RegionSnapshot.capture(arena.getDonutCorner1(), arena.getDonutCorner2());
      });
   }

   public void invalidateDonutSnapshot(MaceTier tier) {
      this.donutSnapshotCache.remove(tier);
   }

   public void load() {
      if (this.file.exists()) {
         YamlConfiguration yml = YamlConfiguration.loadConfiguration(this.file);

         for (MaceTier tier : MaceTier.values()) {
            String base = tier.name();
            if (yml.getConfigurationSection(base) != null) {
               MaceArena arena = this.arenas.get(tier);
               arena.setNormalCorner1(LocationUtil.load(yml, base + ".normal_corner1"));
               arena.setNormalCorner2(LocationUtil.load(yml, base + ".normal_corner2"));
               arena.setNormalSpawn(LocationUtil.load(yml, base + ".normal_spawn"));
               arena.setDonutCorner1(LocationUtil.load(yml, base + ".donut_corner1"));
               arena.setDonutCorner2(LocationUtil.load(yml, base + ".donut_corner2"));
            }
         }
      }
   }

   public void save() {
      YamlConfiguration yml = new YamlConfiguration();

      for (MaceTier tier : MaceTier.values()) {
         MaceArena arena = this.arenas.get(tier);
         String base = tier.name();
         if (arena.getNormalCorner1() != null) {
            LocationUtil.save(yml, base + ".normal_corner1", arena.getNormalCorner1());
         }

         if (arena.getNormalCorner2() != null) {
            LocationUtil.save(yml, base + ".normal_corner2", arena.getNormalCorner2());
         }

         if (arena.getNormalSpawn() != null) {
            LocationUtil.save(yml, base + ".normal_spawn", arena.getNormalSpawn());
         }

         if (arena.getDonutCorner1() != null) {
            LocationUtil.save(yml, base + ".donut_corner1", arena.getDonutCorner1());
         }

         if (arena.getDonutCorner2() != null) {
            LocationUtil.save(yml, base + ".donut_corner2", arena.getDonutCorner2());
         }
      }

      try {
         if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
         }

         yml.save(this.file);
      } catch (IOException var8) {
         this.plugin.getLogger().severe("Failed to save mace_arenas.yml: " + var8.getMessage());
      }
   }
}
