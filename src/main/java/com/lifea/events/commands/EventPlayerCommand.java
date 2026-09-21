package com.lifea.events.commands;

import com.lifea.events.LifeaEvents;
import com.lifea.events.gui.HostGui;
import com.lifea.events.gui.MainMenuGui;
import com.lifea.events.gui.SetupGui;
import com.lifea.events.util.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class EventPlayerCommand implements CommandExecutor, TabCompleter {
   private final LifeaEvents plugin;

   public EventPlayerCommand(LifeaEvents plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         if (!player.hasPermission("lifeaevents.use")) {
            this.plugin.getMessagesManager().send(player, "event.no-permission");
            return true;
         } else if (args.length == 0) {
            this.plugin.getMessagesManager().send(player, "event.usage");
            return true;
         } else {
            String sub = args[0].toLowerCase();
            switch (sub) {
               case "join": {
                  String error = this.plugin.getEventManager().join(player);
                  if (error != null) {
                     MessageUtil.sendRaw(player, error);
                  }

                  return true;
               }
               case "leave": {
                  String error = this.plugin.getEventManager().leave(player);
                  if (error != null) {
                     MessageUtil.sendRaw(player, error);
                  }

                  return true;
               }
               case "spectate":
                  this.plugin.getSpectatorManager().open(player);
                  return true;
               case "music":
                  boolean nowOn = this.plugin.getMusicManager().toggle(player);
                  this.plugin.getMessagesManager().send(player, nowOn ? "event.music-on" : "event.music-off");
                  return true;
               case "admin":
                  if (!player.hasPermission("lifeaevents.host") && !player.hasPermission("lifeaevents.admin")) {
                     this.plugin.getMessagesManager().send(player, "event.no-permission");
                     return true;
                  }

                  MainMenuGui.open(this.plugin, player);
                  return true;
               case "host":
                  if (!player.hasPermission("lifeaevents.host")) {
                     this.plugin.getMessagesManager().send(player, "event.no-permission");
                     return true;
                  }

                  HostGui.open(this.plugin, player);
                  return true;
               case "setup":
                  if (!player.hasPermission("lifeaevents.admin")) {
                     this.plugin.getMessagesManager().send(player, "event.no-permission");
                     return true;
                  }

                  SetupGui.open(this.plugin, player);
                  return true;
               case "reload":
                  if (!player.hasPermission("lifeaevents.admin")) {
                     this.plugin.getMessagesManager().send(player, "event.no-permission");
                     return true;
                  }

                  this.plugin.getConfigManager().reload();
                  this.plugin.getMessagesManager().reload();
                  this.plugin.getScoreboardManager().reload();
                  MessageUtil.sendRaw(player, "<green>Reloaded config.yml, messages.yml and scoreboard.yml.</green>");
                  return true;
               case "stop":
                  if (!player.hasPermission("lifeaevents.host") && !player.hasPermission("lifeaevents.admin")) {
                     this.plugin.getMessagesManager().send(player, "event.no-permission");
                     return true;
                  } else {
                     if (!this.plugin.getEventManager().isHosting()) {
                        MessageUtil.sendRaw(player, "<red>There is no event running right now.</red>");
                        return true;
                     }

                     this.plugin.getEventManager().forceEnd();
                     MessageUtil.sendRaw(player, "<green>Event forcibly ended and everyone restored.</green>");
                     return true;
                  }
               default:
                  this.plugin.getMessagesManager().send(player, "event.usage");
                  return true;
            }
         }
      } else {
         sender.sendMessage("This command can only be used in-game.");
         return true;
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      return args.length == 1 ? this.filter(List.of("join", "leave", "spectate", "music", "host", "setup", "admin", "reload", "stop"), args[0]) : List.of();
   }

   private List<String> filter(List<String> options, String input) {
      List<String> results = new ArrayList<>();

      for (String opt : options) {
         if (opt.toLowerCase().startsWith(input.toLowerCase())) {
            results.add(opt);
         }
      }

      return results;
   }
}
