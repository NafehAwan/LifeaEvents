package com.lifea.events.minigames.impl.mace;

public enum MaceTier {
   TIER_1("Arena 1", 25),
   TIER_2("Arena 2", 14),
   TIER_3("Arena 3", 8),
   TIER_4("Arena 4", 4),
   TIER_5("Arena 5", 0);

   private final String label;
   private final int minPlayers;

   private MaceTier(String label, int minPlayers) {
      this.label = label;
      this.minPlayers = minPlayers;
   }

   public String label() {
      return this.label;
   }

   public int minPlayers() {
      return this.minPlayers;
   }

   public static MaceTier forPlayerCount(int count) {
      for (MaceTier tier : values()) {
         if (count >= tier.minPlayers) {
            return tier;
         }
      }

      return TIER_5;
   }
}
