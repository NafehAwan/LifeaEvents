package com.lifea.chess.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

/** Small text helpers shared by the UI classes. */
public final class Text {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int[] SMALL_CAPS = {
        7424, 665, 7428, 7429, 7431, 1171, 610, 668, 618, 7434, 7435, 671, 7437, 628, 7439, 7448, 491, 640, 1109, 7451, 7452, 7456, 7457, 88, 655, 7458
    };

    private Text() {
    }

    public static Component mm(String raw) {
        return MM.deserialize(raw == null ? "" : raw);
    }

    /** MiniMessage without italics (for item names and lore). */
    public static Component item(String raw) {
        return mm(raw).decoration(TextDecoration.ITALIC, false);
    }

    public static Component colored(String text, String hex) {
        return Component.text(text).color(TextColor.fromHexString(hex.length() == 9 ? "#" + hex.substring(3) : hex));
    }

    public static Component bold(String text, String hex) {
        return colored(text, hex).decorate(TextDecoration.BOLD);
    }

    /** Converts ASCII letters to Unicode small capitals (the pixel-art menu look). */
    public static String smallCaps(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toLowerCase().toCharArray()) {
            int idx = "abcdefghijklmnopqrstuvwxyz".indexOf(c);
            if (idx >= 0) {
                sb.appendCodePoint(SMALL_CAPS[idx]);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** "4m 07s" style duration. */
    public static String duration(long millis) {
        long s = Math.max(0, millis / 1000L);
        long m = s / 60L;
        long h = m / 60L;
        if (h > 0) {
            return h + "h " + (m % 60) + "m";
        }
        return m + "m " + String.format("%02d", s % 60) + "s";
    }

    public static String signed(int delta) {
        return delta > 0 ? "<green>+" + delta + "</green>" : delta < 0 ? "<red>" + delta + "</red>" : "<gray>±0</gray>";
    }
}
