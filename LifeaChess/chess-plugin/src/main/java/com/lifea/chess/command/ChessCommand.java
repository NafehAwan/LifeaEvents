package com.lifea.chess.command;

import com.lifea.chess.LifeaChessPlugin;
import com.lifea.chess.config.Messages;
import com.lifea.chess.engine.Move;
import com.lifea.chess.engine.San;
import com.lifea.chess.engine.Uci;
import com.lifea.chess.game.ActiveMatch;
import com.lifea.chess.match.TimeControl;
import com.lifea.chess.storage.RatedPlayer;
import com.lifea.chess.table.ChessTable;
import com.lifea.chess.ui.MenuService;
import com.lifea.chess.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * /chess — opens the menu with no arguments; every subcommand is kept as a
 * shortcut. Admin table management is GUI-first with command equivalents.
 */
public final class ChessCommand implements CommandExecutor, TabCompleter {
    private static final List<String> PLAYER_SUBS = List.of("play", "accept", "deny", "cancel", "resign", "draw", "stats",
            "leaderboard", "spectate", "leave", "move", "rematch", "queue", "sit", "menu");
    private static final List<String> ADMIN_SUBS = List.of("reload", "admin");
    private static final List<String> TABLE_SUBS = List.of("create", "setchair", "setboard", "remove", "list", "tp", "stop");

    private final LifeaChessPlugin plugin;

    public ChessCommand(LifeaChessPlugin plugin) {
        this.plugin = plugin;
    }

    private Messages msg() {
        return plugin.messages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.mm(msg().string("general.players-only")));
            return true;
        }
        if (!player.hasPermission("lifeachess.play")) {
            msg().send(player, "general.no-permission");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            plugin.menus().openModes(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "play", "challenge" -> play(player, args);
            case "accept" -> plugin.matches().accept(player);
            case "deny", "decline" -> plugin.matches().deny(player);
            case "cancel" -> plugin.matches().cancel(player);
            case "resign" -> plugin.matches().resign(player);
            case "draw" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("decline")) {
                    plugin.matches().declineDraw(player);
                } else {
                    plugin.matches().drawAction(player);
                }
            }
            case "stats" -> showStats(player, args.length > 1 ? args[1] : player.getName());
            case "leaderboard", "top" -> showLeaderboard(player);
            case "spectate", "watch" -> spectate(player, args);
            case "leave" -> plugin.matches().leave(player);
            case "move" -> move(player, args);
            case "rematch" -> plugin.matches().rematch(player);
            case "queue", "find" -> queue(player, args);
            case "sit" -> sit(player);
            case "reload" -> {
                if (!player.hasPermission("lifeachess.admin")) {
                    msg().send(player, "general.no-permission");
                    return true;
                }
                plugin.reloadAll();
                msg().send(player, "general.reloaded");
            }
            case "admin" -> admin(player, args);
            default -> msg().send(player, "general.usage");
        }
        return true;
    }

    private void play(Player player, String[] args) {
        if (args.length < 2) {
            msg().send(player, "general.usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            msg().send(player, "general.player-not-found", "player", args[1]);
            return;
        }
        if (args.length == 2) {
            plugin.menus().openTimeControls(player, MenuService.Purpose.CHALLENGE, target.getUniqueId(), null);
            return;
        }
        TimeControl tc = parseTimeControl(player, args[2]);
        if (tc == null) {
            return;
        }
        boolean ranked = args.length > 3 ? args[3].equalsIgnoreCase("ranked") : plugin.matches().lastSettings(player.getUniqueId()).ranked();
        plugin.matches().challenge(player, target, tc, ranked);
    }

    private void queue(Player player, String[] args) {
        if (args.length == 1) {
            plugin.menus().openTimeControls(player, MenuService.Purpose.FIND_MATCH, null, null);
            return;
        }
        TimeControl tc = parseTimeControl(player, args[1]);
        if (tc == null) {
            return;
        }
        boolean ranked = args.length > 2 ? args[2].equalsIgnoreCase("ranked") : plugin.matches().lastSettings(player.getUniqueId()).ranked();
        plugin.matches().joinQueue(player, tc, ranked);
    }

    private TimeControl parseTimeControl(Player player, String text) {
        try {
            TimeControl tc = TimeControl.parse(text);
            if (!plugin.config().customAllowed(tc)) {
                player.sendMessage(Text.mm(msg().prefix() + "<red>That time control is not allowed here.</red>"));
                return null;
            }
            return tc;
        } catch (RuntimeException e) {
            player.sendMessage(Text.mm(msg().prefix() + "<red>Time control must look like </red><white>3+2</white><red>.</red>"));
            return null;
        }
    }

    private void sit(Player player) {
        ChessTable t = plugin.tables().nearest(player.getLocation(), 16);
        if (t == null) {
            msg().send(player, "table.none-nearby");
            return;
        }
        int seat = plugin.seats().occupant(t, ChessTable.WHITE_SEAT) == null ? ChessTable.WHITE_SEAT : ChessTable.BLACK_SEAT;
        plugin.matches().sitAt(player, t, seat);
    }

    private void spectate(Player player, String[] args) {
        if (args.length < 2) {
            plugin.menus().openTables(player, false);
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            msg().send(player, "general.player-not-found", "player", args[1]);
            return;
        }
        plugin.matches().spectate(player, target);
    }

    /** Chat/command move input: accepts UCI (e2e4, e7e8q) or SAN (Nf3, O-O). */
    private void move(Player player, String[] args) {
        if (args.length < 2) {
            msg().send(player, "match.moved");
            return;
        }
        ActiveMatch am = plugin.matches().matchOf(player);
        if (am == null || !am.match().isActive()) {
            msg().send(player, "match.not-in-match");
            return;
        }
        Move m;
        try {
            m = Uci.parse(args[1].toLowerCase(Locale.ROOT));
        } catch (RuntimeException e) {
            try {
                m = San.parse(am.match().game().position(), args[1]);
            } catch (RuntimeException e2) {
                msg().send(player, "match.illegal-move");
                return;
            }
        }
        plugin.matches().playMove(player, m);
    }

    public void showStats(Player viewer, String name) {
        plugin.stats().lookup(name, (opt, rank) -> {
            if (opt.isEmpty()) {
                msg().send(viewer, "stats.unknown", "player", name);
                return;
            }
            RatedPlayer p = opt.get();
            var s = p.stats();
            msg().sendRaw(viewer, "stats.header", "player", p.name());
            msg().sendRaw(viewer, "stats.line-rating", "rating", String.valueOf(s.rating()), "peak", String.valueOf(s.peakRating()),
                    "rank", rank > 0 ? String.valueOf(rank) : msg().string("stats.unranked"));
            msg().sendRaw(viewer, "stats.line-record", "wins", String.valueOf(s.wins()), "losses", String.valueOf(s.losses()),
                    "draws", String.valueOf(s.draws()), "games", String.valueOf(s.games()),
                    "win_rate", String.format(Locale.ROOT, "%.0f", s.winRate() * 100));
            msg().sendRaw(viewer, "stats.line-streak", "streak", String.valueOf(s.streak()), "best_streak", String.valueOf(s.bestStreak()));
        });
    }

    public void showLeaderboard(Player viewer) {
        plugin.stats().top(10, list -> {
            msg().sendRaw(viewer, "leaderboard.header");
            if (list.isEmpty()) {
                msg().sendRaw(viewer, "leaderboard.empty");
                return;
            }
            int rank = 1;
            for (RatedPlayer p : list) {
                msg().sendRaw(viewer, "leaderboard.entry", "rank", String.valueOf(rank++), "player", p.name(),
                        "rating", String.valueOf(p.stats().rating()), "wins", String.valueOf(p.stats().wins()),
                        "losses", String.valueOf(p.stats().losses()), "draws", String.valueOf(p.stats().draws()));
            }
        });
    }

    // ------------------------------------------------------------------ admin

    private void admin(Player player, String[] args) {
        if (!player.hasPermission("lifeachess.admin")) {
            msg().send(player, "general.no-permission");
            return;
        }
        if (args.length < 2) {
            plugin.menus().openAdmin(player);
            return;
        }
        if (!args[1].equalsIgnoreCase("table") || args.length < 3) {
            player.sendMessage(Text.mm(msg().prefix() + "<white>/chess admin table <create|setchair|setboard|remove|list|tp|stop> ...</white>"));
            return;
        }
        String action = args[2].toLowerCase(Locale.ROOT);
        String id = args.length > 3 ? args[3].toLowerCase(Locale.ROOT) : null;
        switch (action) {
            case "list" -> {
                for (ChessTable t : plugin.tables().all()) {
                    player.sendMessage(Text.mm(msg().prefix() + "<white>" + t.id() + "</white> <gray>- " + (t.isComplete() ? "complete" : "incomplete")
                            + (plugin.tables().isBusy(t) ? ", in game" : "") + "</gray>"));
                }
                if (plugin.tables().all().isEmpty()) {
                    player.sendMessage(Text.mm(msg().prefix() + "<gray>No tables yet.</gray>"));
                }
            }
            case "create" -> {
                if (id == null) {
                    player.sendMessage(Text.mm(msg().prefix() + "<red>Usage: /chess admin table create <id></red>"));
                    return;
                }
                if (plugin.tables().exists(id)) {
                    msg().send(player, "table.exists", "table", id);
                    return;
                }
                plugin.tables().create(id, player.getLocation());
                msg().send(player, "table.created", "table", id);
            }
            case "setchair" -> {
                ChessTable t = requireTable(player, id);
                if (t == null) {
                    return;
                }
                int index = args.length > 4 && args[4].equals("2") ? 1 : 0;
                t.setChair(index, player.getLocation());
                plugin.tables().save();
                plugin.bigBoards().remove(t);
                plugin.bigBoards().update(t, plugin.matches().atTable(t));
                msg().send(player, "table.chair-set", "index", String.valueOf(index + 1), "table", t.id());
            }
            case "setboard" -> {
                ChessTable t = requireTable(player, id);
                if (t == null) {
                    return;
                }
                t.setBoard(player.getLocation());
                plugin.tables().save();
                plugin.bigBoards().remove(t);
                plugin.bigBoards().update(t, plugin.matches().atTable(t));
                msg().send(player, "table.board-set", "table", t.id());
            }
            case "remove" -> {
                ChessTable t = requireTable(player, id);
                if (t == null) {
                    return;
                }
                plugin.matches().abortAt(t);
                plugin.seats().standAt(t);
                plugin.bigBoards().remove(t);
                plugin.tables().remove(t.id());
                msg().send(player, "table.removed", "table", t.id());
            }
            case "tp" -> {
                ChessTable t = requireTable(player, id);
                if (t != null && t.anchor() != null) {
                    player.teleport(t.anchor().add(0, 0.5, 0));
                    msg().send(player, "table.teleported", "table", t.id());
                }
            }
            case "stop" -> {
                if (id == null) {
                    plugin.matches().abortAll();
                    player.sendMessage(Text.mm(msg().prefix() + "<yellow>All matches aborted.</yellow>"));
                } else {
                    ChessTable t = requireTable(player, id);
                    if (t != null) {
                        plugin.matches().abortAt(t);
                        player.sendMessage(Text.mm(msg().prefix() + "<yellow>Match at " + t.id() + " aborted.</yellow>"));
                    }
                }
            }
            default -> player.sendMessage(Text.mm(msg().prefix() + "<white>/chess admin table <create|setchair|setboard|remove|list|tp|stop> ...</white>"));
        }
    }

    private ChessTable requireTable(Player player, String id) {
        ChessTable t = plugin.tables().get(id);
        if (t == null) {
            msg().send(player, "table.unknown", "table", id == null ? "?" : id);
        }
        return t;
    }

    // ------------------------------------------------------------------ tab completion

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean admin = sender.hasPermission("lifeachess.admin");
        if (args.length == 1) {
            List<String> all = new ArrayList<>(PLAYER_SUBS);
            if (admin) {
                all.addAll(ADMIN_SUBS);
            }
            return filter(all, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            switch (sub) {
                case "play", "challenge", "spectate", "watch", "stats" -> {
                    List<String> names = new ArrayList<>();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        names.add(p.getName());
                    }
                    return filter(names, args[1]);
                }
                case "draw" -> {
                    return filter(List.of("decline"), args[1]);
                }
                case "queue", "find" -> {
                    return filter(timeControls(), args[1]);
                }
                case "admin" -> {
                    return admin ? filter(List.of("table"), args[1]) : List.of();
                }
                default -> {
                    return List.of();
                }
            }
        }
        if (sub.equals("admin") && admin) {
            if (args.length == 3) {
                return filter(TABLE_SUBS, args[2]);
            }
            if (args.length == 4) {
                return filter(plugin.tables().ids(), args[3]);
            }
            if (args.length == 5 && args[2].equalsIgnoreCase("setchair")) {
                return filter(List.of("1", "2"), args[4]);
            }
        }
        if ((sub.equals("play") || sub.equals("challenge")) && args.length == 3) {
            return filter(timeControls(), args[2]);
        }
        if ((sub.equals("play") || sub.equals("challenge")) && args.length == 4 || (sub.equals("queue") || sub.equals("find")) && args.length == 3) {
            return filter(List.of("ranked", "casual"), args[args.length - 1]);
        }
        return List.of();
    }

    private List<String> timeControls() {
        List<String> out = new ArrayList<>();
        for (TimeControl tc : plugin.config().presets) {
            out.add(tc.notation());
        }
        return out;
    }

    private static List<String> filter(List<String> options, String prefix) {
        List<String> out = new ArrayList<>();
        String p = prefix.toLowerCase(Locale.ROOT);
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }
}
