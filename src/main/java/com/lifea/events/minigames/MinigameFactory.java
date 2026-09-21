package com.lifea.events.minigames;

import com.lifea.events.LifeaEvents;
import com.lifea.events.arena.Arena;
import com.lifea.events.minigames.impl.HideAndSeekGame;
import com.lifea.events.minigames.impl.HotTntGame;
import com.lifea.events.minigames.impl.MaceRouletteGame;
import com.lifea.events.minigames.impl.RedLightGreenLightGame;
import com.lifea.events.minigames.impl.SpleefGame;
import com.lifea.events.minigames.impl.TeamFightGame;

public final class MinigameFactory {
   private MinigameFactory() {
   }

   public static Minigame create(LifeaEvents plugin, MinigameType type, Arena arena) {
      return (Minigame)(switch (type) {
         case MACE_ROULETTE -> new MaceRouletteGame(plugin, arena);
         case TEAM_FIGHT -> new TeamFightGame(plugin, arena);
         case HIDE_AND_SEEK -> new HideAndSeekGame(plugin, arena);
         case HOT_TNT -> new HotTntGame(plugin, arena);
         case SPLEEF -> new SpleefGame(plugin, arena);
         case RED_LIGHT_GREEN_LIGHT -> new RedLightGreenLightGame(plugin, arena);
      });
   }
}
