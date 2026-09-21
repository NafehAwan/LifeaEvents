package com.lifea.events.arena;

import com.lifea.events.minigames.MinigameType;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public class Arena {
   private final MinigameType type;
   private Location preLobbySpawn;
   private Location corner1;
   private Location corner2;
   private final Map<String, Location> spawns = new HashMap<>();

   public Arena(MinigameType type) {
      this.type = type;
   }

   public MinigameType getType() {
      return this.type;
   }

   public Location getPreLobbySpawn() {
      return this.preLobbySpawn;
   }

   public void setPreLobbySpawn(Location preLobbySpawn) {
      this.preLobbySpawn = preLobbySpawn;
   }

   public Location getCorner1() {
      return this.corner1;
   }

   public void setCorner1(Location corner1) {
      this.corner1 = corner1;
   }

   public Location getCorner2() {
      return this.corner2;
   }

   public void setCorner2(Location corner2) {
      this.corner2 = corner2;
   }

   public void setSpawn(String key, Location location) {
      this.spawns.put(key, location);
   }

   public Location getSpawn(String key) {
      return this.spawns.get(key);
   }

   public Map<String, Location> getSpawns() {
      return this.spawns;
   }

   public BoundingBox getBoundingBox() {
      return this.corner1 != null && this.corner2 != null ? BoundingBox.of(this.corner1, this.corner2) : null;
   }

   public boolean isFullyConfigured() {
      if (this.preLobbySpawn == null) {
         return false;
      } else if (!this.type.requiresBoundingBox() || this.corner1 != null && this.corner2 != null) {
         for (String required : this.type.requiredSpawns()) {
            if (!this.spawns.containsKey(required) || this.spawns.get(required) == null) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }
}
