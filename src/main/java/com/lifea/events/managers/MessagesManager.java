package com.lifea.events.managers;

import com.lifea.events.LifeaEvents;
import com.lifea.events.util.MessageUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class MessagesManager {
   private final LifeaEvents plugin;
   private final File file;
   private YamlConfiguration yml;

   public MessagesManager(LifeaEvents plugin) {
      this.plugin = plugin;
      this.file = new File(plugin.getDataFolder(), "messages.yml");
      this.load();
   }

   public void load() {
      if (!this.file.exists()) {
         this.plugin.saveResource("messages.yml", false);
      }

      this.yml = YamlConfiguration.loadConfiguration(this.file);

      try (InputStream defStream = this.plugin.getResource("messages.yml")) {
         if (defStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            boolean changed = false;

            for (String key : defaults.getKeys(true)) {
               if (!defaults.isConfigurationSection(key) && !this.yml.isSet(key)) {
                  this.yml.set(key, defaults.get(key));
                  changed = true;
               }
            }

            if (changed) {
               this.yml.save(this.file);
            }
         }
      } catch (IOException var8) {
         this.plugin.getLogger().warning("Could not merge new default messages into messages.yml: " + var8.getMessage());
      }
   }

   public void reload() {
      this.load();
   }

   public String get(String key, String... placeholderPairs) {
      String raw = this.yml.getString(key, key);

      for (int i = 0; i + 1 < placeholderPairs.length; i += 2) {
         raw = raw.replace("%" + placeholderPairs[i] + "%", placeholderPairs[i + 1]);
      }

      return raw;
   }

   public void send(CommandSender target, String key, String... placeholderPairs) {
      MessageUtil.send(target, this.get(key, placeholderPairs));
   }

   public void broadcastRaw(String key, String... placeholderPairs) {
      MessageUtil.broadcast(this.get(key, placeholderPairs));
   }

   public void broadcast(String key, String... placeholderPairs) {
      MessageUtil.broadcast(this.get(key, placeholderPairs));
   }

   public void actionBar(Player player, String key, String... placeholderPairs) {
      MessageUtil.actionBar(player, this.get(key, placeholderPairs));
   }

   public void title(Player player, String titleKey, String subtitleKey, int fadeIn, int stay, int fadeOut, String... placeholderPairs) {
      MessageUtil.title(player, this.get(titleKey, placeholderPairs), this.get(subtitleKey, placeholderPairs), fadeIn, stay, fadeOut);
   }
}
