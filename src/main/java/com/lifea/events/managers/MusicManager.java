package com.lifea.events.managers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class MusicManager {
   private static final Sound PRE_LOBBY_TRACK = Sound.MUSIC_DISC_CAT;
   private static final Sound MATCH_TRACK = Sound.MUSIC_DISC_PIGSTEP;
   private static final long TRACK_LOOP_TICKS = 3900L;
   private final Set<UUID> muted = new HashSet<>();

   public boolean isMuted(Player player) {
      return this.muted.contains(player.getUniqueId());
   }

   public boolean toggle(Player player) {
      if (this.muted.remove(player.getUniqueId())) {
         return true;
      } else {
         this.muted.add(player.getUniqueId());
         this.stopAll(player);
         return false;
      }
   }

   public void playPreLobbyTrack(Player player) {
      if (!this.isMuted(player)) {
         player.playSound(player.getLocation(), PRE_LOBBY_TRACK, SoundCategory.RECORDS, 0.6F, 1.0F);
      }
   }

   public void playMatchTrack(Player player) {
      if (!this.isMuted(player)) {
         player.playSound(player.getLocation(), MATCH_TRACK, SoundCategory.RECORDS, 0.6F, 1.0F);
      }
   }

   public long trackLoopTicks() {
      return 3900L;
   }

   public void stopAll(Player player) {
      player.stopSound(PRE_LOBBY_TRACK, SoundCategory.RECORDS);
      player.stopSound(MATCH_TRACK, SoundCategory.RECORDS);
   }

   public void playSpinTick(Player player, float pitch) {
      player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.MASTER, 0.8F, pitch);
   }

   public void playSpinReveal(Player player) {
      player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.8F, 1.4F);
   }

   public void playCountdownTick(Player player) {
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.MASTER, 1.0F, 1.0F);
   }

   public void playGo(Player player) {
      player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0F, 1.6F);
   }
}
