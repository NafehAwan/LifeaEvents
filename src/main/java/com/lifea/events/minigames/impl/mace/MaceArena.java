package com.lifea.events.minigames.impl.mace;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public class MaceArena {
   private final MaceTier tier;
   private Location normalCorner1;
   private Location normalCorner2;
   private Location normalSpawn;
   private Location donutCorner1;
   private Location donutCorner2;

   public MaceArena(MaceTier tier) {
      this.tier = tier;
   }

   public MaceTier getTier() {
      return this.tier;
   }

   public Location getNormalCorner1() {
      return this.normalCorner1;
   }

   public void setNormalCorner1(Location normalCorner1) {
      this.normalCorner1 = normalCorner1;
   }

   public Location getNormalCorner2() {
      return this.normalCorner2;
   }

   public void setNormalCorner2(Location normalCorner2) {
      this.normalCorner2 = normalCorner2;
   }

   public Location getNormalSpawn() {
      return this.normalSpawn;
   }

   public void setNormalSpawn(Location normalSpawn) {
      this.normalSpawn = normalSpawn;
   }

   public Location getDonutCorner1() {
      return this.donutCorner1;
   }

   public void setDonutCorner1(Location donutCorner1) {
      this.donutCorner1 = donutCorner1;
   }

   public Location getDonutCorner2() {
      return this.donutCorner2;
   }

   public void setDonutCorner2(Location donutCorner2) {
      this.donutCorner2 = donutCorner2;
   }

   public BoundingBox getNormalBoundingBox() {
      return this.normalCorner1 != null && this.normalCorner2 != null ? BoundingBox.of(this.normalCorner1, this.normalCorner2) : null;
   }

   public boolean isFullyConfigured() {
      return this.normalCorner1 != null && this.normalCorner2 != null && this.normalSpawn != null && this.donutCorner1 != null && this.donutCorner2 != null;
   }

   public boolean isNormalConfigured() {
      return this.normalCorner1 != null && this.normalCorner2 != null && this.normalSpawn != null;
   }

   public boolean isDonutConfigured() {
      return this.donutCorner1 != null && this.donutCorner2 != null;
   }
}
