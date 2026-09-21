package com.lifea.events.minigames;

import com.lifea.events.util.MessageUtil;
import org.bukkit.Material;

public enum MinigameType {
   MACE_ROULETTE("Mace Roulette", Material.MACE, new String[0]),
   TEAM_FIGHT("Team Fight", Material.IRON_SWORD, new String[]{"RED_SPAWN", "BLUE_SPAWN"}),
   HIDE_AND_SEEK("Hide and Seek", Material.LEATHER_BOOTS, new String[]{"HIDERS_SPAWN", "SEEKERS_SPAWN"}),
   HOT_TNT("Hot TNT", Material.TNT, new String[]{"MAIN_SPAWN"}),
   SPLEEF("Spleef", Material.DIAMOND_SHOVEL, new String[]{"MAIN_SPAWN"}),
   RED_LIGHT_GREEN_LIGHT(
      "Red Light, Green Light", Material.LIME_DYE, new String[]{"START_LINE_CORNER_1", "START_LINE_CORNER_2", "FINISH_LINE_CORNER_1", "FINISH_LINE_CORNER_2"}
   );

   private final String displayName;
   private final String smallCapsName;
   private final Material icon;
   private final String[] requiredSpawns;

   private MinigameType(String displayName, Material icon, String[] requiredSpawns) {
      this.displayName = displayName;
      this.smallCapsName = MessageUtil.smallCaps(displayName);
      this.icon = icon;
      this.requiredSpawns = requiredSpawns;
   }

   public String displayName() {
      return this.displayName;
   }

   public String smallCapsName() {
      return this.smallCapsName;
   }

   public Material icon() {
      return this.icon;
   }

   public String[] requiredSpawns() {
      return this.requiredSpawns;
   }

   public boolean requiresBoundingBox() {
      return this == HIDE_AND_SEEK || this == HOT_TNT || this == SPLEEF;
   }
}
