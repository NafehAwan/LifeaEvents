package com.lifea.events.managers;

import com.lifea.events.LifeaEvents;
import com.lifea.events.arena.Arena;
import com.lifea.events.minigames.GameState;
import com.lifea.events.minigames.Minigame;
import com.lifea.events.minigames.MinigameFactory;
import com.lifea.events.minigames.MinigameType;
import com.lifea.events.util.MessageUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class EventManager {
   private final LifeaEvents plugin;
   private GameState state = GameState.LOBBY;
   private MinigameType selectedType;
   private int minPlayers;
   private int maxPlayers;
   private Player host;
   private final Set<UUID> queuedPlayers = new CopyOnWriteArraySet<>();
   private BossBar bossBar;
   private BukkitTask countdownTask;
   private int secondsRemaining;
   private int totalCountdown;
   private Minigame activeMinigame;
   private BukkitTask scoreboardTask;

   public EventManager(LifeaEvents plugin) {
      this.plugin = plugin;
   }

   public GameState getState() {
      return this.state;
   }

   public MinigameType getSelectedType() {
      return this.selectedType;
   }

   public int getMaxPlayers() {
      return this.maxPlayers;
   }

   public int getMinPlayers() {
      return this.minPlayers;
   }

   public Player getHost() {
      return this.host;
   }

   public int getQueuedCount() {
      return this.queuedPlayers.size();
   }

   public boolean isHosting() {
      return this.state != GameState.LOBBY;
   }

   public boolean beginHost(Player host, MinigameType type, int minPlayers, int maxPlayers, int countdownSeconds) {
      if (this.isHosting()) {
         this.plugin.getMessagesManager().send(host, "announce.already-hosting");
         return false;
      } else {
         Arena arena = this.plugin.getArenaManager().getArena(type);
         boolean configured = arena.isFullyConfigured();
         if (type == MinigameType.MACE_ROULETTE) {
            configured = configured && this.plugin.getMaceArenaManager().isFullyConfigured();
         }

         if (!configured) {
            this.plugin.getMessagesManager().send(host, "announce.arena-not-configured");
            return false;
         } else {
            this.host = host;
            this.selectedType = type;
            this.minPlayers = minPlayers;
            this.maxPlayers = maxPlayers;
            this.queuedPlayers.clear();
            this.state = GameState.PRE_GAME;
            this.totalCountdown = countdownSeconds > 0 ? countdownSeconds : this.plugin.getConfigManager().countdownSeconds();
            this.secondsRemaining = this.totalCountdown;
            this.announce();
            this.startBossBar();
            this.startCountdown();
            return true;
         }
      }
   }

   private void announce() {
      String announcement = this.plugin.getMessagesManager().get("announce.header", "brand", MessageUtil.brandBold(), "host", this.host.getName())
         + "\n"
         + this.plugin.getMessagesManager().get("announce.minigame-line", "minigame", this.selectedType.displayName())
         + "\n"
         + this.plugin
            .getMessagesManager()
            .get("announce.capacity-line", "current", String.valueOf(this.queuedPlayers.size()), "max", String.valueOf(this.maxPlayers))
         + "\n"
         + this.plugin.getMessagesManager().get("announce.starting-line", "time", this.formatTime(this.secondsRemaining))
         + "\n\n<click:run_command:'/event join'><hover:show_text:'<green>Click to join the event!'>"
         + this.plugin.getMessagesManager().get("announce.click-to-join")
         + "</hover></click>";
      MessageUtil.broadcast(announcement);
   }

   private void startBossBar() {
      this.bossBar = BossBar.bossBar(this.bossBarTitle(), 1.0F, Color.RED, Overlay.PROGRESS);

      for (Player p : Bukkit.getOnlinePlayers()) {
         p.showBossBar(this.bossBar);
      }
   }

   private Component bossBarTitle() {
      return MessageUtil.parse(
         MessageUtil.brandBold()
            + " <color:#FF1A1A><b>"
            + this.selectedType.displayName()
            + "</b></color> <gray>-</gray> <white>Starting In "
            + this.formatTime(this.secondsRemaining)
            + "</white>"
      );
   }

   private String formatTime(int seconds) {
      int m = seconds / 60;
      int s = seconds % 60;
      return (m > 0 ? m + "m " : "") + s + "s";
   }

   private Map<String, String> preLobbyPlaceholders(String time) {
      Map<String, String> map = new HashMap<>();
      map.put("event", this.selectedType.displayName());
      map.put("host", this.host.getName());
      map.put("time", time);
      return map;
   }

   private Map<String, String> activePlaceholders() {
      Map<String, String> map = new HashMap<>();
      map.put("event", this.selectedType.displayName());
      map.put("host", this.host != null ? this.host.getName() : "");
      map.put("alive", String.valueOf(this.activeMinigame.getAlive().size()));
      map.put("total", String.valueOf(this.activeMinigame.getAlive().size() + this.activeMinigame.getSpectators().size()));
      map.putAll(this.activeMinigame.scoreboardExtras());
      return map;
   }

   private void startScoreboardTask() {
      this.scoreboardTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         if (this.activeMinigame != null) {
            Map<String, String> placeholders = this.activePlaceholders();

            for (Player p : new ArrayList<>(this.activeMinigame.getAlive())) {
               this.plugin.getScoreboardManager().render(p, this.selectedType, "active", placeholders);
            }

            for (Player p : new ArrayList<>(this.activeMinigame.getSpectators())) {
               this.plugin.getScoreboardManager().render(p, this.selectedType, "spectate", placeholders);
            }
         }
      }, 0L, 20L);
   }

   private void startCountdown() {
      List<Integer> alerts = this.plugin.getConfigManager().alertSeconds();
      int reminderInterval = this.plugin.getConfigManager().reminderIntervalSeconds();
      this.countdownTask = Bukkit.getScheduler()
         .runTaskTimer(
            this.plugin,
            () -> {
               if (this.secondsRemaining <= 0) {
                  this.finishCountdown();
               } else {
                  if (this.bossBar != null) {
                     this.bossBar.name(this.bossBarTitle());
                     this.bossBar.progress(Math.max(0.0F, Math.min(1.0F, (float)this.secondsRemaining / (float)this.totalCountdown)));
                  }

                  for (UUID uuid : this.queuedPlayers) {
                     Player p = Bukkit.getPlayer(uuid);
                     if (p != null) {
                        this.plugin
                           .getScoreboardManager()
                           .render(p, this.selectedType, "pre_lobby", this.preLobbyPlaceholders(this.formatTime(this.secondsRemaining)));
                     }
                  }

                  if (reminderInterval > 0 && this.secondsRemaining < this.totalCountdown && this.secondsRemaining % reminderInterval == 0) {
                     this.announce();
                  }

                  if (alerts.contains(this.secondsRemaining)) {
                     for (UUID uuidx : this.queuedPlayers) {
                        Player p = Bukkit.getPlayer(uuidx);
                        if (p != null) {
                           MessageUtil.title(
                              p,
                              "<color:#FF1A1A><b>" + this.secondsRemaining + "</b></color>",
                              this.plugin.getMessagesManager().get("announce.starting-soon-subtitle", "minigame", this.selectedType.displayName()),
                              5,
                              15,
                              5
                           );
                           p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, this.secondsRemaining <= 5 ? 2.0F : 1.0F);
                        }
                     }
                  }

                  this.secondsRemaining--;
               }
            },
            0L,
            20L
         );
   }

   private void finishCountdown() {
      if (this.countdownTask != null) {
         this.countdownTask.cancel();
      }

      if (this.bossBar != null) {
         for (Player p : Bukkit.getOnlinePlayers()) {
            p.hideBossBar(this.bossBar);
         }

         this.bossBar = null;
      }

      List<Player> finalRoster = new ArrayList<>();

      for (UUID uuid : this.queuedPlayers) {
         Player p = Bukkit.getPlayer(uuid);
         if (p != null && p.isOnline()) {
            finalRoster.add(p);
         }

         if (p != null) {
            this.plugin.getScoreboardManager().clear(p);
         }
      }

      if (finalRoster.size() < this.minPlayers) {
         this.plugin
            .getMessagesManager()
            .broadcast("announce.not-enough-players", "minigame", this.selectedType.displayName(), "min", String.valueOf(this.minPlayers));

         for (Player px : finalRoster) {
            if (this.plugin.getPlayerStateManager().hasSnapshot(px)) {
               this.plugin.getPlayerStateManager().restore(px);
            }
         }

         this.resetToLobby();
      } else {
         this.state = GameState.ACTIVE;
         Arena arena = this.plugin.getArenaManager().getArena(this.selectedType);
         this.activeMinigame = MinigameFactory.create(this.plugin, this.selectedType, arena);

         for (Player pxx : finalRoster) {
            Location anySpawn = arena.getSpawns().values().stream().findFirst().orElse(null);
            if (anySpawn != null) {
               pxx.teleport(anySpawn);
            }

            pxx.setGameMode(GameMode.SURVIVAL);
         }

         this.activeMinigame.start(finalRoster);
         this.startScoreboardTask();
      }
   }

   public String join(Player player) {
      if (this.state != GameState.PRE_GAME) {
         return this.plugin.getMessagesManager().get("event.no-active-event");
      } else if (this.queuedPlayers.contains(player.getUniqueId())) {
         return this.plugin.getMessagesManager().get("event.already-joined");
      } else if (this.queuedPlayers.size() >= this.maxPlayers) {
         return this.plugin.getMessagesManager().get("event.event-full");
      } else {
         this.plugin.getPlayerStateManager().capture(player);
         this.queuedPlayers.add(player.getUniqueId());
         Arena arena = this.plugin.getArenaManager().getArena(this.selectedType);
         if (arena.getPreLobbySpawn() != null) {
            player.teleport(arena.getPreLobbySpawn());
         }

         player.setGameMode(GameMode.ADVENTURE);
         if (this.bossBar != null) {
            player.showBossBar(this.bossBar);
         }

         this.plugin.getScoreboardManager().render(player, this.selectedType, "pre_lobby", this.preLobbyPlaceholders(this.formatTime(this.secondsRemaining)));
         this.plugin.getMusicManager().playPreLobbyTrack(player);
         this.plugin.getMessagesManager().send(player, "event.join-success", "minigame", this.selectedType.displayName());
         return null;
      }
   }

   public String leave(Player player) {
      if (this.state == GameState.PRE_GAME && this.queuedPlayers.contains(player.getUniqueId())) {
         this.queuedPlayers.remove(player.getUniqueId());
         if (this.bossBar != null) {
            player.hideBossBar(this.bossBar);
         }

         this.plugin.getScoreboardManager().clear(player);
         this.plugin.getMusicManager().stopAll(player);
         if (this.plugin.getPlayerStateManager().hasSnapshot(player)) {
            this.plugin.getPlayerStateManager().restore(player);
         }

         this.plugin.getMessagesManager().send(player, "event.leave-success", "minigame", this.selectedType.displayName());
         return null;
      } else {
         return this.plugin.getMessagesManager().get("event.not-in-lobby");
      }
   }

   public boolean isInPreLobby(Player player) {
      return this.state == GameState.PRE_GAME && this.queuedPlayers.contains(player.getUniqueId());
   }

   public boolean isInActiveEvent(Player player) {
      return this.state == GameState.ACTIVE && this.activeMinigame != null && this.activeMinigame.getAlive().contains(player);
   }

   public Minigame getActiveMinigame() {
      return this.activeMinigame;
   }

   public boolean isSpectating(Player player) {
      return this.state == GameState.ACTIVE && this.activeMinigame != null && this.activeMinigame.getSpectators().contains(player);
   }

   public void onMinigameEnded() {
      this.state = GameState.RESTORATION;
      Bukkit.getScheduler().runTaskLater(this.plugin, this::resetToLobby, 60L);
   }

   public void handleDisconnect(Player player) {
      if (this.state == GameState.PRE_GAME && this.queuedPlayers.contains(player.getUniqueId())) {
         this.queuedPlayers.remove(player.getUniqueId());
         this.plugin.getScoreboardManager().clear(player);
         if (this.plugin.getPlayerStateManager().hasSnapshot(player)) {
            this.plugin.getPlayerStateManager().restore(player);
         }
      } else if (this.state == GameState.ACTIVE && this.activeMinigame != null && this.activeMinigame.getAlive().contains(player)) {
         this.plugin.getMessagesManager().broadcast("elimination.disconnected", "player", player.getName());
         this.activeMinigame.eliminate(player, false);
      } else if (this.state == GameState.ACTIVE && this.activeMinigame != null) {
         this.activeMinigame.getSpectators().remove(player);
      }
   }

   public void resetToLobby() {
      this.state = GameState.LOBBY;
      this.selectedType = null;
      this.host = null;
      this.queuedPlayers.clear();
      this.activeMinigame = null;
      if (this.countdownTask != null) {
         this.countdownTask.cancel();
         this.countdownTask = null;
      }

      if (this.bossBar != null) {
         for (Player p : Bukkit.getOnlinePlayers()) {
            p.hideBossBar(this.bossBar);
         }

         this.bossBar = null;
      }

      if (this.scoreboardTask != null) {
         this.scoreboardTask.cancel();
         this.scoreboardTask = null;
      }
   }

   public void forceEnd() {
      if (this.state == GameState.PRE_GAME) {
         for (UUID uuid : this.queuedPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
               if (this.bossBar != null) {
                  p.hideBossBar(this.bossBar);
               }

               this.plugin.getScoreboardManager().clear(p);
               this.plugin.getMusicManager().stopAll(p);
               if (this.plugin.getPlayerStateManager().hasSnapshot(p)) {
                  this.plugin.getPlayerStateManager().restore(p);
               }
            }
         }
      } else if (this.activeMinigame != null) {
         this.activeMinigame.end();
      }

      this.resetToLobby();
   }
}
