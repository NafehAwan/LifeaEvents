package com.lifea.chess.config;

import org.bukkit.configuration.ConfigurationSection;

/** Colour palette (hex strings as configured, plus ARGB helpers). */
public record Theme(String background, String darkSquare, String lightSquare, String lastMove, String selected,
                    String legalMove, String capture, String check, String whitePiece, String blackPiece,
                    String clock, String accent, String text, String muted, String positive, String warning) {

    public static Theme from(ConfigurationSection s) {
        return new Theme(
                hex(s, "background", "#0b0b0f"), hex(s, "dark-square", "#26262b"), hex(s, "light-square", "#3b2b30"),
                hex(s, "last-move", "#2457b8"), hex(s, "selected", "#3f7bff"), hex(s, "legal-move", "#8b5cf6"),
                hex(s, "capture", "#d12e2e"), hex(s, "check", "#ff1a1a"), hex(s, "white-piece", "#f5f0e6"),
                hex(s, "black-piece", "#e8788c"), hex(s, "clock", "#2ee6d6"), hex(s, "accent", "#b14cff"),
                hex(s, "text", "#ffffff"), hex(s, "muted", "#9a9aa3"), hex(s, "positive", "#4ade80"),
                hex(s, "warning", "#f5b942"));
    }

    private static String hex(ConfigurationSection s, String key, String def) {
        String v = s == null ? null : s.getString(key);
        if (v == null || !v.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) {
            return def;
        }
        return v;
    }

    /** Parses #rrggbb or #aarrggbb into packed ARGB (opaque when no alpha given). */
    public static int argb(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        long v = Long.parseLong(h, 16);
        if (h.length() == 6) {
            v |= 0xFF000000L;
        }
        return (int) v;
    }

    public static org.bukkit.Color bukkit(String hex) {
        return org.bukkit.Color.fromARGB(argb(hex));
    }

    /** Opaque hex with a custom alpha byte, e.g. alpha(0x99, "#2457b8") -> "#992457b8". */
    public static String alpha(int alpha, String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() == 8) {
            h = h.substring(2);
        }
        return String.format("#%02x%s", alpha & 0xFF, h);
    }
}
