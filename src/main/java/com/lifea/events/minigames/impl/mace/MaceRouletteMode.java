package com.lifea.events.minigames.impl.mace;

public enum MaceRouletteMode {
   SOLO_MACE("Solo Mace", false, 1),
   DONUT_DOUBLE_MACE("Donut Map + 2 Mace", true, 2),
   NO_MACE("No Mace", false, 0),
   DONUT_SOLO_MACE("Donut Map + 1 Mace", true, 1);

   private final String label;
   private final boolean donut;
   private final int maceCount;

   private MaceRouletteMode(String label, boolean donut, int maceCount) {
      this.label = label;
      this.donut = donut;
      this.maceCount = maceCount;
   }

   public String label() {
      return this.label;
   }

   public boolean isDonut() {
      return this.donut;
   }

   public int maceCount() {
      return this.maceCount;
   }

   public boolean hasMace() {
      return this.maceCount > 0;
   }
}
