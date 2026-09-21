package com.lifea.events.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class LocationUtil {
   private LocationUtil() {
   }

   public static void save(ConfigurationSection section, String path, Location loc) {
      if (loc != null && loc.getWorld() != null) {
         section.set(path + ".world", loc.getWorld().getName());
         section.set(path + ".x", loc.getX());
         section.set(path + ".y", loc.getY());
         section.set(path + ".z", loc.getZ());
         section.set(path + ".yaw", loc.getYaw());
         section.set(path + ".pitch", loc.getPitch());
      }
   }

   public static Location load(ConfigurationSection section, String path) {
      if (section != null && section.isSet(path + ".world")) {
         String worldName = section.getString(path + ".world");
         World world = Bukkit.getWorld(worldName);
         if (world == null) {
            return null;
         } else {
            double x = section.getDouble(path + ".x");
            double y = section.getDouble(path + ".y");
            double z = section.getDouble(path + ".z");
            float yaw = (float)section.getDouble(path + ".yaw");
            float pitch = (float)section.getDouble(path + ".pitch");
            return new Location(world, x, y, z, yaw, pitch);
         }
      } else {
         return null;
      }
   }
}
