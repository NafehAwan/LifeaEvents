package com.lifea.events.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public class RegionSnapshot {
   private final int sizeX;
   private final int sizeY;
   private final int sizeZ;
   private final BlockData[] data;

   private RegionSnapshot(int sizeX, int sizeY, int sizeZ, BlockData[] data) {
      this.sizeX = sizeX;
      this.sizeY = sizeY;
      this.sizeZ = sizeZ;
      this.data = data;
   }

   public static RegionSnapshot capture(Location corner1, Location corner2) {
      World world = corner1.getWorld();
      int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
      int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
      int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
      int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
      int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
      int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
      int sizeX = maxX - minX + 1;
      int sizeY = maxY - minY + 1;
      int sizeZ = maxZ - minZ + 1;
      BlockData[] data = new BlockData[sizeX * sizeY * sizeZ];
      int i = 0;

      for (int y = minY; y <= maxY; y++) {
         for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
               data[i++] = world.getBlockAt(x, y, z).getBlockData().clone();
            }
         }
      }

      return new RegionSnapshot(sizeX, sizeY, sizeZ, data);
   }

   public void paste(Location corner1, Location corner2) {
      World world = corner1.getWorld();
      int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
      int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
      int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
      int destSizeX = Math.abs(corner1.getBlockX() - corner2.getBlockX()) + 1;
      int destSizeY = Math.abs(corner1.getBlockY() - corner2.getBlockY()) + 1;
      int destSizeZ = Math.abs(corner1.getBlockZ() - corner2.getBlockZ()) + 1;
      int pasteX = Math.min(this.sizeX, destSizeX);
      int pasteY = Math.min(this.sizeY, destSizeY);
      int pasteZ = Math.min(this.sizeZ, destSizeZ);

      for (int dy = 0; dy < pasteY; dy++) {
         for (int dx = 0; dx < pasteX; dx++) {
            for (int dz = 0; dz < pasteZ; dz++) {
               int i = dy * this.sizeX * this.sizeZ + dx * this.sizeZ + dz;
               if (i < this.data.length) {
                  Block block = world.getBlockAt(minX + dx, minY + dy, minZ + dz);
                  block.setBlockData(this.data[i], false);
               }
            }
         }
      }
   }

   public boolean matchesSize(Location corner1, Location corner2) {
      int destSizeX = Math.abs(corner1.getBlockX() - corner2.getBlockX()) + 1;
      int destSizeY = Math.abs(corner1.getBlockY() - corner2.getBlockY()) + 1;
      int destSizeZ = Math.abs(corner1.getBlockZ() - corner2.getBlockZ()) + 1;
      return this.sizeX == destSizeX && this.sizeY == destSizeY && this.sizeZ == destSizeZ;
   }

   public int sizeX() {
      return this.sizeX;
   }

   public int sizeY() {
      return this.sizeY;
   }

   public int sizeZ() {
      return this.sizeZ;
   }
}
