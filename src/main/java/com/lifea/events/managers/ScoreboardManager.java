package com.lifea.events.managers;

import com.lifea.events.LifeaEvents;
import com.lifea.events.minigames.MinigameType;
import com.lifea.events.util.MessageUtil;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

public class ScoreboardManager {
   private static final String OBJECTIVE_NAME = "lifea_board";
   private final LifeaEvents plugin;
   private final File file;
   private YamlConfiguration yml;
   private final Set<UUID> shown = new HashSet<>();

   public ScoreboardManager(LifeaEvents plugin) {
      this.plugin = plugin;
      this.file = new File(plugin.getDataFolder(), "scoreboard.yml");
      this.load();
   }

   public void load() {
      if (!this.file.exists()) {
         this.plugin.saveResource("scoreboard.yml", false);
      }

      this.yml = YamlConfiguration.loadConfiguration(this.file);
   }

   public void reload() {
      this.load();
   }

   public void render(Player player, MinigameType type, String phase, Map<String, String> placeholders) {
      String base = type.name() + "." + phase;
      String title = this.yml.getString(base + ".title");
      List<String> lines = this.yml.getStringList(base + ".lines");
      if (title != null) {
         Scoreboard board = this.plugin.getServer().getScoreboardManager().getNewScoreboard();
         Objective objective = board.registerNewObjective("lifea_board", "dummy", MessageUtil.parse(this.apply(title, placeholders)));
         objective.setDisplaySlot(DisplaySlot.SIDEBAR);
         player.setScoreboard(board);
         this.shown.add(player.getUniqueId());
         int score = lines.size();
         int blank = 0;

         for (String rawLine : lines) {
            String line = this.apply(rawLine, placeholders);
            String entryKey = line.isBlank() ? "blank_" + blank++ : "line_" + score;
            Score scoreObj = objective.getScore(entryKey);
            scoreObj.customName(MessageUtil.parse(line));
            scoreObj.setScore(score--);
         }
      }
   }

   private String apply(String line, Map<String, String> placeholders) {
      String result = line.replace("%server%", this.yml.getString("server", ""));

      for (Entry<String, String> entry : placeholders.entrySet()) {
         result = result.replace("%" + entry.getKey() + "%", entry.getValue());
      }

      return result;
   }

   public boolean isShowing(Player player) {
      return this.shown.contains(player.getUniqueId());
   }

   public void clear(Player player) {
      this.shown.remove(player.getUniqueId());
      player.setScoreboard(this.plugin.getServer().getScoreboardManager().getMainScoreboard());
   }
}
