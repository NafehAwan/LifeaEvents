package com.lifea.chess.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Named sound cues; every call is a no-op when sounds are disabled. */
public final class Sounds {
    private static volatile boolean enabled = true;

    private Sounds() {
    }

    public static void setEnabled(boolean on) {
        enabled = on;
    }

    private static void play(Player p, Sound sound, float volume, float pitch) {
        if (enabled && p != null && p.isOnline()) {
            p.playSound(p.getLocation(), sound, volume, pitch);
        }
    }

    public static void select(Player p) {
        play(p, Sound.UI_BUTTON_CLICK, 0.6f, 1.6f);
    }

    public static void move(Player p) {
        play(p, Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 0.8f, 1.1f);
    }

    public static void capture(Player p) {
        play(p, Sound.ENTITY_ITEM_PICKUP, 0.9f, 0.7f);
    }

    public static void check(Player p) {
        play(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.8f);
    }

    public static void checkmate(Player p) {
        play(p, Sound.ENTITY_WITHER_SPAWN, 0.4f, 1.4f);
    }

    public static void promotion(Player p) {
        play(p, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
    }

    public static void start(Player p) {
        play(p, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
    }

    public static void victory(Player p) {
        play(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    public static void defeat(Player p) {
        play(p, Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
    }

    public static void draw(Player p) {
        play(p, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
    }

    public static void invite(Player p) {
        play(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
    }

    public static void lowTime(Player p) {
        play(p, Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1.0f);
    }

    public static void error(Player p) {
        play(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
    }
}
