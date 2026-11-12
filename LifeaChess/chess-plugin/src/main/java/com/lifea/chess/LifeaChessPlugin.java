package com.lifea.chess;

import com.lifea.chess.command.ChessCommand;
import com.lifea.chess.config.ChessConfig;
import com.lifea.chess.config.Messages;
import com.lifea.chess.config.Theme;
import com.lifea.chess.game.MatchManager;
import com.lifea.chess.game.StatsService;
import com.lifea.chess.listener.ChessListeners;
import com.lifea.chess.table.SeatService;
import com.lifea.chess.table.TableManager;
import com.lifea.chess.ui.BigBoardView;
import com.lifea.chess.ui.MenuService;
import com.lifea.chess.ui.ScreenBoardView;
import com.lifea.chess.util.Sounds;
import java.sql.SQLException;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * LifeaChess — tabletop chess for Paper. Requires ExyliaLib (and PacketEvents
 * for the packet screen board). Wires configuration, persistence, tables,
 * seating, matches, menus, boards, the /chess command and listeners.
 */
public final class LifeaChessPlugin extends JavaPlugin {
    private ChessConfig config;
    private Messages messages;
    private StatsService stats;
    private TableManager tables;
    private SeatService seats;
    private MatchManager matches;
    private MenuService menus;
    private ScreenBoardView screens;
    private BigBoardView bigBoards;
    private ChessCommand command;

    @Override
    public void onEnable() {
        config = ChessConfig.load(this);
        messages = new Messages(this);
        Sounds.setEnabled(config.sounds);

        stats = new StatsService(this);
        try {
            stats.open();
        } catch (SQLException e) {
            getLogger().severe("Could not open the database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        tables = new TableManager(this);
        tables.load();
        seats = new SeatService(this);
        seats.standAll();
        screens = new ScreenBoardView(this);
        bigBoards = new BigBoardView(this);
        matches = new MatchManager(this);
        menus = new MenuService(this);
        command = new ChessCommand(this);

        PluginCommand cmd = getCommand("chess");
        if (cmd != null) {
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        }
        getServer().getPluginManager().registerEvents(new ChessListeners(this), this);
        matches.start();
        // Boards are entities; spawn them once worlds are ready.
        getServer().getScheduler().runTask(this, () -> bigBoards.showAll());
        for (Player p : getServer().getOnlinePlayers()) {
            stats.preload(p);
        }
        screens.available();
        getLogger().info("LifeaChess enabled with " + tables.all().size() + " table(s).");
    }

    @Override
    public void onDisable() {
        if (matches != null) {
            matches.abortAll();
            matches.shutdown();
        }
        if (seats != null) {
            seats.standAll();
        }
        if (bigBoards != null) {
            bigBoards.removeAll();
        }
        if (tables != null) {
            tables.save();
        }
        if (stats != null) {
            stats.close();
        }
        getLogger().info("LifeaChess disabled.");
    }

    /** Reloads config.yml and messages.yml and redraws the in-world boards. */
    public void reloadAll() {
        config = ChessConfig.load(this);
        messages.reload();
        Sounds.setEnabled(config.sounds);
        bigBoards.removeAll();
        bigBoards.showAll();
    }

    public ChessConfig config() {
        return config;
    }

    public Theme theme() {
        return config.theme;
    }

    public Messages messages() {
        return messages;
    }

    public StatsService stats() {
        return stats;
    }

    public TableManager tables() {
        return tables;
    }

    public SeatService seats() {
        return seats;
    }

    public MatchManager matches() {
        return matches;
    }

    public MenuService menus() {
        return menus;
    }

    public ScreenBoardView screens() {
        return screens;
    }

    public BigBoardView bigBoards() {
        return bigBoards;
    }

    public ChessCommand commandHelper() {
        return command;
    }
}
