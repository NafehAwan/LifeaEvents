package com.lifea.chess.config;

import com.lifea.chess.engine.GameResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** messages.yml access with MiniMessage parsing and %placeholder% substitution. */
public final class Messages {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yml;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        yml = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource("messages.yml")) {
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                boolean changed = false;
                for (String key : defaults.getKeys(true)) {
                    if (!defaults.isConfigurationSection(key) && !yml.isSet(key)) {
                        yml.set(key, defaults.get(key));
                        changed = true;
                    }
                }
                if (changed) {
                    yml.save(file);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not merge default messages: " + e.getMessage());
        }
    }

    public static Component mm(String raw) {
        return MM.deserialize(raw == null ? "" : raw);
    }

    /** Raw string with placeholders applied (pairs: key, value, key, value...). */
    public String string(String key, String... pairs) {
        String raw = yml.getString(key, key);
        return apply(raw, pairs);
    }

    public List<String> list(String key, String... pairs) {
        List<String> out = new ArrayList<>();
        for (String line : yml.getStringList(key)) {
            out.add(apply(line, pairs));
        }
        return out;
    }

    private static String apply(String raw, String... pairs) {
        String s = raw;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            s = s.replace("%" + pairs[i] + "%", pairs[i + 1] == null ? "" : pairs[i + 1]);
        }
        return s;
    }

    public String prefix() {
        return yml.getString("prefix", "");
    }

    /** Prefixed component. */
    public Component component(String key, String... pairs) {
        return mm(prefix() + string(key, pairs));
    }

    /** Unprefixed component. */
    public Component raw(String key, String... pairs) {
        return mm(string(key, pairs));
    }

    public void send(CommandSender to, String key, String... pairs) {
        to.sendMessage(component(key, pairs));
    }

    public void sendRaw(CommandSender to, String key, String... pairs) {
        to.sendMessage(raw(key, pairs));
    }

    public String reason(GameResult.Reason reason) {
        return yml.getString("result.reasons." + reason.name(), reason.name().toLowerCase().replace('_', ' '));
    }
}
