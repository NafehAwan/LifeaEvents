package com.lifea.events;

import com.lifea.events.arena.ArenaManager;
import com.lifea.events.commands.EventPlayerCommand;
import com.lifea.events.gui.GuiListener;
import com.lifea.events.listeners.ArenaEnforcementListener;
import com.lifea.events.listeners.PlayerConnectionListener;
import com.lifea.events.listeners.ProtectionListener;
import com.lifea.events.listeners.SetupWizardListener;
import com.lifea.events.listeners.SpectatorListener;
import com.lifea.events.managers.ConfigManager;
import com.lifea.events.managers.EventManager;
import com.lifea.events.managers.MessagesManager;
import com.lifea.events.managers.MusicManager;
import com.lifea.events.managers.PlayerStateManager;
import com.lifea.events.managers.ScoreboardManager;
import com.lifea.events.managers.SetupWizardManager;
import com.lifea.events.managers.SpectatorManager;
import com.lifea.events.minigames.impl.mace.MaceArenaManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LifeaEvents extends JavaPlugin {
   private static LifeaEvents instance;
   private ConfigManager configManager;
   private MessagesManager messagesManager;
   private ArenaManager arenaManager;
   private MaceArenaManager maceArenaManager;
   private EventManager eventManager;
   private PlayerStateManager playerStateManager;
   private SetupWizardManager setupWizardManager;
   private SpectatorManager spectatorManager;
   private ScoreboardManager scoreboardManager;
   private MusicManager musicManager;

   public void onEnable() {
      instance = this;
      this.configManager = new ConfigManager(this);
      this.messagesManager = new MessagesManager(this);
      this.arenaManager = new ArenaManager(this);
      this.maceArenaManager = new MaceArenaManager(this);
      this.playerStateManager = new PlayerStateManager();
      this.setupWizardManager = new SetupWizardManager(this);
      this.scoreboardManager = new ScoreboardManager(this);
      this.musicManager = new MusicManager();
      this.eventManager = new EventManager(this);
      this.spectatorManager = new SpectatorManager(this);
      EventPlayerCommand eventPlayerCommand = new EventPlayerCommand(this);
      PluginCommand eventCmd = this.getCommand("event");
      if (eventCmd != null) {
         eventCmd.setExecutor(eventPlayerCommand);
         eventCmd.setTabCompleter(eventPlayerCommand);
      }

      this.getServer().getPluginManager().registerEvents(new GuiListener(this), this);
      this.getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
      this.getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
      this.getServer().getPluginManager().registerEvents(new SetupWizardListener(this), this);
      this.getServer().getPluginManager().registerEvents(new ArenaEnforcementListener(this), this);
      this.getServer().getPluginManager().registerEvents(new SpectatorListener(this), this);
      this.getLogger().info("LifeaEvents has been enabled.");
   }

   public void onDisable() {
      if (this.eventManager != null) {
         this.eventManager.forceEnd();
      }

      if (this.arenaManager != null) {
         this.arenaManager.save();
      }

      if (this.maceArenaManager != null) {
         this.maceArenaManager.save();
      }

      this.getLogger().info("LifeaEvents has been disabled.");
   }

   public static LifeaEvents get() {
      return instance;
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public MessagesManager getMessagesManager() {
      return this.messagesManager;
   }

   public ArenaManager getArenaManager() {
      return this.arenaManager;
   }

   public MaceArenaManager getMaceArenaManager() {
      return this.maceArenaManager;
   }

   public EventManager getEventManager() {
      return this.eventManager;
   }

   public PlayerStateManager getPlayerStateManager() {
      return this.playerStateManager;
   }

   public SetupWizardManager getSetupWizardManager() {
      return this.setupWizardManager;
   }

   public SpectatorManager getSpectatorManager() {
      return this.spectatorManager;
   }

   public ScoreboardManager getScoreboardManager() {
      return this.scoreboardManager;
   }

   public MusicManager getMusicManager() {
      return this.musicManager;
   }
}
