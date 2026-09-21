package com.lifea.events.util;

import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageUtil {
   public static final String BRIGHT_RED = "#FF1A1A";
   private static final int[] SMALL_CAPS = new int[]{
      7424, 665, 7428, 7429, 7431, 1171, 610, 668, 618, 7434, 7435, 671, 7437, 628, 7439, 7448, 491, 640, 1109, 7451, 7452, 7456, 7457, 88, 655, 7458
   };
   private static final MiniMessage MM = MiniMessage.miniMessage();
   private static String prefixRaw = "<color:#FF1A1A>" + smallCapsStatic("LifeaEvents") + "</color> <gray>»</gray> ";

   private MessageUtil() {
   }

   public static void setPrefix(String raw) {
      prefixRaw = raw;
   }

   public static Component parse(String raw) {
      return MM.deserialize(raw == null ? "" : raw);
   }

   public static Component parsePrefixed(String raw) {
      return MM.deserialize(prefixRaw + raw);
   }

   public static void send(CommandSender target, String raw) {
      target.sendMessage(parsePrefixed(raw));
   }

   public static void sendRaw(CommandSender target, String raw) {
      target.sendMessage(parse(raw));
   }

   public static void broadcast(String raw) {
      Bukkit.broadcast(parse(raw));
   }

   public static void title(Player player, String titleRaw, String subtitleRaw, int fadeIn, int stay, int fadeOut) {
      player.showTitle(
         Title.title(
            parse(titleRaw),
            parse(subtitleRaw == null ? "" : subtitleRaw),
            Times.times(Duration.ofMillis((long)fadeIn * 50L), Duration.ofMillis((long)stay * 50L), Duration.ofMillis((long)fadeOut * 50L))
         )
      );
   }

   public static void actionBar(Player player, String raw) {
      player.sendActionBar(parse(raw));
   }

   public static String smallCaps(String input) {
      return smallCapsStatic(input);
   }

   public static String brandBold() {
      return "<color:#FF1A1A><b>" + smallCapsStatic("LifeaEvents") + "</b></color>";
   }

   private static String smallCapsStatic(String input) {
      StringBuilder sb = new StringBuilder();
      String lower = input.toLowerCase();

      for (char c : lower.toCharArray()) {
         int idx = "abcdefghijklmnopqrstuvwxyz".indexOf(c);
         if (idx >= 0) {
            sb.appendCodePoint(SMALL_CAPS[idx]);
         } else {
            sb.append(c);
         }
      }

      return sb.toString();
   }
}
