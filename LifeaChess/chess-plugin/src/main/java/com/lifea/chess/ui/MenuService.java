package com.lifea.chess.ui;

import com.lifea.chess.LifeaChessPlugin;
import com.lifea.chess.config.Messages;
import com.lifea.chess.engine.PieceType;
import com.lifea.chess.game.ActiveMatch;
import com.lifea.chess.game.MatchManager;
import com.lifea.chess.match.TimeControl;
import com.lifea.chess.storage.RatedPlayer;
import com.lifea.chess.table.ChessTable;
import com.lifea.chess.util.Sounds;
import com.lifea.chess.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Chest-style menus: the "MODES » TABLETOP" entry menu (reference image 3), the
 * Chess hub, time-control/mode picker, tables list, admin panel and a promotion
 * fallback. Every menu is identified by its InventoryHolder, so clicks are
 * routed here from the listener.
 */
public final class MenuService {

    public enum Purpose { TABLE_LOBBY, CHALLENGE, FIND_MATCH }

    private abstract static class Holder implements InventoryHolder {
        Inventory inv;

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private static final class ModesHolder extends Holder {
    }

    private static final class HubHolder extends Holder {
    }

    private static final class TablesHolder extends Holder {
        final List<ChessTable> tables = new ArrayList<>();
    }

    private static final class AdminHolder extends Holder {
    }

    private static final class PromotionHolder extends Holder {
    }

    private static final class TimeControlHolder extends Holder {
        Purpose purpose;
        UUID target;
        String tableId;
        boolean ranked;
        TimeControl selected;
        final List<TimeControl> options = new ArrayList<>();
    }

    private static final int MODES_CHESS_SLOT = 10;
    private static final int[] TC_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    private final LifeaChessPlugin plugin;

    public MenuService(LifeaChessPlugin plugin) {
        this.plugin = plugin;
    }

    private Messages msg() {
        return plugin.messages();
    }

    // ------------------------------------------------------------------ items

    private static ItemStack item(Material mat, String name, List<String> lore, boolean glow) {
        ItemStack s = new ItemStack(mat);
        ItemMeta m = s.getItemMeta();
        m.displayName(Text.item(name));
        if (lore != null && !lore.isEmpty()) {
            List<Component> l = new ArrayList<>();
            for (String line : lore) {
                l.add(Text.item(line));
            }
            m.lore(l);
        }
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ENCHANTS);
        if (glow) {
            m.setEnchantmentGlintOverride(true);
        }
        s.setItemMeta(m);
        return s;
    }

    private static ItemStack filler() {
        return item(Material.GRAY_STAINED_GLASS_PANE, " ", null, false);
    }

    private static void fill(Inventory inv) {
        ItemStack f = filler();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, f);
        }
    }

    private static String sc(String s) {
        return Text.smallCaps(s);
    }

    // ------------------------------------------------------------------ MODES » TABLETOP

    public void openModes(Player player) {
        ModesHolder h = new ModesHolder();
        h.inv = Bukkit.createInventory(h, 45, Text.mm("<color:#b14cff><b>" + sc("Modes") + "</b></color> <gray>»</gray> <white><b>"
                + sc("Tabletop") + "</b></white> <gray>1/1</gray>"));
        fill(h.inv);
        int playing = 0;
        for (ActiveMatch am : plugin.matches().all()) {
            if (!am.match().isFinished()) {
                playing += 2;
            }
        }
        boolean anyTable = false;
        boolean anyFree = false;
        for (ChessTable t : plugin.tables().all()) {
            if (t.isComplete()) {
                anyTable = true;
                if (!plugin.tables().isBusy(t)) {
                    anyFree = true;
                }
            }
        }
        String state = !anyTable ? msg().string("menu.state-full") : anyFree ? msg().string("menu.state-open") : msg().string("menu.state-in-game");
        List<String> lore = msg().list("menu.chess-info", "state", state, "playing", String.valueOf(playing));
        String title = lore.isEmpty() ? "<color:#b14cff><b>CHESS</b></color>" : lore.remove(0);
        h.inv.setItem(MODES_CHESS_SLOT, item(Material.CRAFTING_TABLE, title, lore, true));
        player.openInventory(h.inv);
    }

    // ------------------------------------------------------------------ Chess hub

    public void openHub(Player player) {
        HubHolder h = new HubHolder();
        h.inv = Bukkit.createInventory(h, 45, Text.mm("<color:#b14cff><b>" + sc("Chess") + "</b></color>"));
        fill(h.inv);
        RatedPlayer me = plugin.stats().cached(player.getUniqueId());
        String rating = me == null ? "…" : String.valueOf(me.stats().rating());
        h.inv.setItem(10, item(Material.OAK_STAIRS, "<green><b>" + sc("Play Chess") + "</b></green>",
                List.of("<gray>Sit at the nearest free table.</gray>", "", "<gold>→ CLICK TO SIT</gold>"), true));
        h.inv.setItem(11, item(Material.COMPASS, "<aqua><b>" + sc("Find Match") + "</b></aqua>",
                List.of("<gray>Queue up and get seated automatically.</gray>", "", "<gold>→ CLICK TO QUEUE</gold>"), false));
        h.inv.setItem(12, item(Material.PLAYER_HEAD, "<yellow><b>" + sc("Challenge Player") + "</b></yellow>",
                List.of("<gray>Pick an online player to challenge.</gray>", "", "<gold>→ CLICK TO CHOOSE</gold>"), false));
        h.inv.setItem(14, item(Material.NETHER_STAR, "<color:#b14cff><b>" + sc("Ranked") + "</b></color>",
                List.of("<gray>Rated games change your Elo.</gray>", "<gray>Your rating: <aqua>" + rating + "</aqua></gray>", "",
                        plugin.config().rankedEnabled ? "<gold>→ CLICK TO QUEUE RANKED</gold>" : "<red>Disabled</red>"), plugin.config().rankedEnabled));
        h.inv.setItem(15, item(Material.FEATHER, "<white><b>" + sc("Casual") + "</b></white>",
                List.of("<gray>Friendly games, ratings untouched.</gray>", "",
                        plugin.config().casualEnabled ? "<gold>→ CLICK TO QUEUE CASUAL</gold>" : "<red>Disabled</red>"), false));
        h.inv.setItem(16, item(Material.SPYGLASS, "<white><b>" + sc("Spectate") + "</b></white>",
                List.of("<gray>Watch a live match.</gray>", "", "<gold>→ CLICK TO BROWSE TABLES</gold>"), false));
        h.inv.setItem(28, item(Material.BOOK, "<white><b>" + sc("Statistics") + "</b></white>",
                List.of("<gray>Your rating, record and streaks.</gray>"), false));
        h.inv.setItem(29, item(Material.GOLD_INGOT, "<gold><b>" + sc("Leaderboard") + "</b></gold>",
                List.of("<gray>Top rated players.</gray>"), false));
        h.inv.setItem(30, item(Material.LECTERN, "<white><b>" + sc("Tables") + "</b></white>",
                List.of("<gray>All chess tables and their status.</gray>"), false));
        h.inv.setItem(32, item(Material.COMPARATOR, "<white><b>" + sc("Settings") + "</b></white>",
                List.of("<gray>Board auto-open, sounds and more.</gray>", "<gray>(uses server defaults)</gray>"), false));
        if (player.hasPermission("lifeachess.admin")) {
            h.inv.setItem(34, item(Material.COMMAND_BLOCK, "<red><b>" + sc("Admin") + "</b></red>",
                    List.of("<gray>Create tables, reload, stop matches.</gray>"), false));
        }
        h.inv.setItem(40, item(Material.ARROW, "<white><b>" + sc("Back") + "</b></white>", null, false));
        player.openInventory(h.inv);
    }

    // ------------------------------------------------------------------ time controls

    public void openTimeControls(Player player, Purpose purpose, UUID target, String tableId) {
        TimeControlHolder h = new TimeControlHolder();
        h.purpose = purpose;
        h.target = target;
        h.tableId = tableId;
        MatchManager.TableLobby base = tableId != null && plugin.tables().get(tableId) != null
                ? plugin.matches().lobby(plugin.tables().get(tableId)) : plugin.matches().lastSettings(player.getUniqueId());
        h.ranked = base.ranked() && plugin.config().rankedEnabled;
        h.selected = base.timeControl();
        h.options.addAll(plugin.config().presets);
        String title = switch (purpose) {
            case TABLE_LOBBY -> "Table Settings";
            case CHALLENGE -> "Challenge";
            case FIND_MATCH -> "Find Match";
        };
        h.inv = Bukkit.createInventory(h, 45, Text.mm("<color:#b14cff><b>" + sc(title) + "</b></color>"));
        render(h);
        player.openInventory(h.inv);
    }

    private void render(TimeControlHolder h) {
        fill(h.inv);
        for (int i = 0; i < h.options.size() && i < TC_SLOTS.length; i++) {
            TimeControl tc = h.options.get(i);
            boolean sel = tc.equals(h.selected);
            Material mat = switch (tc.category()) {
                case BULLET -> Material.BLAZE_POWDER;
                case BLITZ -> Material.GUNPOWDER;
                case RAPID -> Material.CLOCK;
                case CLASSICAL -> Material.ENCHANTED_BOOK;
            };
            h.inv.setItem(TC_SLOTS[i], item(mat, (sel ? "<green>" : "<white>") + "<b>" + tc.name() + " " + tc.notation() + "</b>",
                    List.of(sel ? "<green>✔ " + sc("Selected") + "</green>" : "<gray>" + sc("Click to select") + "</gray>"), sel));
        }
        boolean canRanked = plugin.config().rankedEnabled;
        boolean canCasual = plugin.config().casualEnabled;
        h.inv.setItem(31, item(h.ranked ? Material.NETHER_STAR : Material.FEATHER,
                h.ranked ? "<color:#b14cff><b>" + sc("Ranked") + "</b></color>" : "<white><b>" + sc("Casual") + "</b></white>",
                List.of("<gray>" + sc("Click to toggle") + "</gray>",
                        canRanked && canCasual ? "" : "<red>" + (canRanked ? "Casual" : "Ranked") + " disabled</red>"), h.ranked));
        h.inv.setItem(40, item(Material.LIME_STAINED_GLASS_PANE, "<green><b>" + sc("Confirm") + "</b></green>",
                List.of("<white>" + h.selected.notation() + " · " + (h.ranked ? "RANKED" : "CASUAL") + "</white>"), true));
        h.inv.setItem(36, item(Material.ARROW, "<white><b>" + sc("Back") + "</b></white>", null, false));
    }

    // ------------------------------------------------------------------ tables & admin

    public void openTables(Player player, boolean adminMode) {
        TablesHolder h = new TablesHolder();
        h.inv = Bukkit.createInventory(h, 54, Text.mm("<color:#b14cff><b>" + sc("Chess Tables") + "</b></color>"));
        fill(h.inv);
        int slot = 10;
        for (ChessTable t : plugin.tables().all()) {
            if (slot >= 44) {
                break;
            }
            if (slot % 9 == 8) {
                slot += 2;
            }
            ActiveMatch am = plugin.matches().atTable(t);
            boolean busy = am != null && !am.match().isFinished();
            String state = !t.isComplete() ? "<red>INCOMPLETE</red>" : busy ? msg().string("menu.state-in-game") : msg().string("menu.state-open");
            List<String> lore = new ArrayList<>();
            lore.add("<gray>|</gray> <white>STATE</white> <gray>»</gray> " + state);
            if (busy) {
                lore.add("<gray>|</gray> <white>" + am.match().whiteName() + "</white> <gray>vs</gray> <white>" + am.match().blackName() + "</white>");
                lore.add("<gray>|</gray> <aqua>" + am.match().timeControl().notation() + "</aqua> <gray>·</gray> " + plugin.matches().modeLabel(am.match().ranked()));
            } else if (t.isComplete()) {
                MatchManager.TableLobby lobby = plugin.matches().lobby(t);
                lore.add("<gray>|</gray> <aqua>" + lobby.timeControl().notation() + "</aqua> <gray>·</gray> " + plugin.matches().modeLabel(lobby.ranked()));
                int seated = (plugin.seats().occupant(t, 0) != null ? 1 : 0) + (plugin.seats().occupant(t, 1) != null ? 1 : 0);
                lore.add("<gray>|</gray> <white>SEATED</white> <gray>»</gray> <aqua>" + seated + "/2</aqua>");
            }
            lore.add("");
            lore.add(busy ? "<gold>→ CLICK TO SPECTATE</gold>" : "<gold>→ CLICK TO GO THERE</gold>");
            if (adminMode) {
                lore.add("<red>Shift-click: remove table</red>");
            }
            h.tables.add(t);
            h.inv.setItem(slot, item(busy ? Material.REDSTONE_BLOCK : t.isComplete() ? Material.EMERALD_BLOCK : Material.BARRIER,
                    "<white><b>" + t.id().toUpperCase() + "</b></white>", lore, busy));
            slot++;
        }
        if (h.tables.isEmpty()) {
            h.inv.setItem(22, item(Material.BARRIER, "<red><b>" + sc("No tables yet") + "</b></red>",
                    List.of("<gray>An admin creates one with</gray>", "<white>/chess admin table create <id></white>"), false));
        }
        h.inv.setItem(49, item(Material.ARROW, "<white><b>" + sc("Back") + "</b></white>", null, false));
        player.openInventory(h.inv);
    }

    public void openAdmin(Player player) {
        AdminHolder h = new AdminHolder();
        h.inv = Bukkit.createInventory(h, 27, Text.mm("<red><b>" + sc("Chess Admin") + "</b></red>"));
        fill(h.inv);
        h.inv.setItem(10, item(Material.LECTERN, "<white><b>" + sc("Tables") + "</b></white>",
                List.of("<gray>View, visit or remove tables.</gray>", "", "<gray>Create: </gray><white>/chess admin table create <id></white>",
                        "<gray>Chairs: </gray><white>/chess admin table setchair <id> <1|2></white>",
                        "<gray>Board: </gray><white>/chess admin table setboard <id></white>"), false));
        h.inv.setItem(12, item(Material.REDSTONE, "<red><b>" + sc("Stop all matches") + "</b></red>",
                List.of("<gray>Aborts every running match (unrated).</gray>"), false));
        h.inv.setItem(14, item(Material.REPEATER, "<yellow><b>" + sc("Reload") + "</b></yellow>",
                List.of("<gray>Reloads config.yml and messages.yml.</gray>"), false));
        h.inv.setItem(16, item(Material.PAPER, "<white><b>" + sc("Status") + "</b></white>",
                List.of("<gray>Tables: <white>" + plugin.tables().all().size() + "</white></gray>",
                        "<gray>Active matches: <white>" + plugin.matches().activeCount() + "</white></gray>",
                        "<gray>Screens: " + (plugin.screens().available() ? "<green>available</green>" : "<red>unavailable</red>") + "</gray>"), false));
        h.inv.setItem(22, item(Material.ARROW, "<white><b>" + sc("Back") + "</b></white>", null, false));
        player.openInventory(h.inv);
    }

    /** Fallback promotion chooser when the screen board is unavailable. */
    public void openPromotion(Player player) {
        PromotionHolder h = new PromotionHolder();
        h.inv = Bukkit.createInventory(h, 9, Text.mm("<color:#b14cff><b>" + sc("Promote to") + "</b></color>"));
        fill(h.inv);
        h.inv.setItem(1, item(Material.GOLD_BLOCK, "<gold><b>♕ " + sc("Queen") + "</b></gold>", null, true));
        h.inv.setItem(3, item(Material.IRON_BLOCK, "<white><b>♖ " + sc("Rook") + "</b></white>", null, false));
        h.inv.setItem(5, item(Material.LAPIS_BLOCK, "<aqua><b>♗ " + sc("Bishop") + "</b></aqua>", null, false));
        h.inv.setItem(7, item(Material.SADDLE, "<yellow><b>♘ " + sc("Knight") + "</b></yellow>", null, false));
        player.openInventory(h.inv);
    }

    // ------------------------------------------------------------------ click routing

    /** Returns true when the click belonged to one of our menus (and was handled/cancelled). */
    public boolean handleClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (!(holder instanceof Holder)) {
            return false;
        }
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player) || e.getClickedInventory() != e.getInventory()) {
            return true;
        }
        int slot = e.getSlot();
        if (holder instanceof ModesHolder) {
            if (slot == MODES_CHESS_SLOT) {
                Sounds.select(player);
                openHub(player);
            }
        } else if (holder instanceof HubHolder) {
            hubClick(player, slot);
        } else if (holder instanceof TimeControlHolder h) {
            timeControlClick(player, h, slot);
        } else if (holder instanceof TablesHolder h) {
            tablesClick(player, h, slot, e.isShiftClick());
        } else if (holder instanceof AdminHolder) {
            adminClick(player, slot);
        } else if (holder instanceof PromotionHolder) {
            PieceType t = switch (slot) {
                case 1 -> PieceType.QUEEN;
                case 3 -> PieceType.ROOK;
                case 5 -> PieceType.BISHOP;
                case 7 -> PieceType.KNIGHT;
                default -> null;
            };
            if (t != null) {
                player.closeInventory();
                plugin.matches().completePromotion(player, t);
            }
        }
        return true;
    }

    private void hubClick(Player player, int slot) {
        switch (slot) {
            case 10 -> {
                player.closeInventory();
                sitNearest(player);
            }
            case 11 -> openTimeControls(player, Purpose.FIND_MATCH, null, null);
            case 12 -> {
                player.closeInventory();
                player.sendMessage(Text.mm(msg().prefix() + "<white>Use </white><aqua>/chess play <player></aqua><white> to challenge someone.</white>"));
            }
            case 14 -> {
                if (plugin.config().rankedEnabled) {
                    player.closeInventory();
                    plugin.matches().joinQueue(player, plugin.config().defaultTimeControl, true);
                }
            }
            case 15 -> {
                if (plugin.config().casualEnabled) {
                    player.closeInventory();
                    plugin.matches().joinQueue(player, plugin.config().defaultTimeControl, false);
                }
            }
            case 16, 30 -> openTables(player, false);
            case 28 -> {
                player.closeInventory();
                plugin.commandHelper().showStats(player, player.getName());
            }
            case 29 -> {
                player.closeInventory();
                plugin.commandHelper().showLeaderboard(player);
            }
            case 32 -> {
                player.closeInventory();
                player.sendMessage(Text.mm(msg().prefix() + "<gray>Board auto-open: <white>" + plugin.config().autoOpenBoard
                        + "</white>, sounds: <white>" + plugin.config().sounds + "</white>, piece style: <white>"
                        + plugin.config().pieceStyle + "</white>. Admins change these in config.yml.</gray>"));
            }
            case 34 -> {
                if (player.hasPermission("lifeachess.admin")) {
                    openAdmin(player);
                }
            }
            case 40 -> openModes(player);
            default -> {
            }
        }
    }

    private void sitNearest(Player player) {
        ChessTable t = plugin.tables().nearest(player.getLocation(), 64);
        if (t == null) {
            t = plugin.tables().freeTable(player.getLocation());
        }
        if (t == null) {
            msg().send(player, "table.none-nearby");
            return;
        }
        int seat = plugin.seats().occupant(t, ChessTable.WHITE_SEAT) == null ? ChessTable.WHITE_SEAT : ChessTable.BLACK_SEAT;
        if (plugin.seats().occupant(t, seat) != null) {
            msg().send(player, "table.in-use");
            return;
        }
        plugin.matches().sitAt(player, t, seat);
    }

    private void timeControlClick(Player player, TimeControlHolder h, int slot) {
        for (int i = 0; i < TC_SLOTS.length && i < h.options.size(); i++) {
            if (TC_SLOTS[i] == slot) {
                h.selected = h.options.get(i);
                Sounds.select(player);
                render(h);
                return;
            }
        }
        if (slot == 31) {
            boolean next = !h.ranked;
            if (next && !plugin.config().rankedEnabled || !next && !plugin.config().casualEnabled) {
                Sounds.error(player);
                return;
            }
            h.ranked = next;
            Sounds.select(player);
            render(h);
        } else if (slot == 36) {
            openHub(player);
        } else if (slot == 40) {
            player.closeInventory();
            switch (h.purpose) {
                case TABLE_LOBBY -> {
                    ChessTable t = plugin.tables().get(h.tableId);
                    if (t != null) {
                        plugin.matches().setLobby(t, h.selected, h.ranked);
                        msg().send(player, "lobby.updated", "time_control", h.selected.notation(), "mode", plugin.matches().modeLabel(h.ranked));
                    }
                }
                case CHALLENGE -> {
                    Player target = h.target == null ? null : Bukkit.getPlayer(h.target);
                    if (target == null) {
                        msg().send(player, "invite.target-offline");
                    } else {
                        plugin.matches().challenge(player, target, h.selected, h.ranked);
                    }
                }
                case FIND_MATCH -> plugin.matches().joinQueue(player, h.selected, h.ranked);
            }
        }
    }

    private void tablesClick(Player player, TablesHolder h, int slot, boolean shift) {
        if (slot == 49) {
            openHub(player);
            return;
        }
        int index = slot - 10 - 2 * ((slot - 10) / 9);
        if (slot < 10 || slot % 9 == 8 || slot % 9 == 0 || index < 0 || index >= h.tables.size()) {
            return;
        }
        ChessTable t = h.tables.get(index);
        player.closeInventory();
        if (shift && player.hasPermission("lifeachess.admin")) {
            plugin.matches().abortAt(t);
            plugin.bigBoards().remove(t);
            plugin.tables().remove(t.id());
            msg().send(player, "table.removed", "table", t.id());
            return;
        }
        ActiveMatch am = plugin.matches().atTable(t);
        if (am != null && !am.match().isFinished()) {
            Player w = Bukkit.getPlayer(am.match().whiteId());
            if (w != null) {
                plugin.matches().spectate(player, w);
            }
            return;
        }
        if (t.anchor() != null) {
            player.teleport(t.anchor().add(0, 0.5, 0));
            msg().send(player, "table.teleported", "table", t.id());
            msg().send(player, "lobby.sit-hint");
        }
    }

    private void adminClick(Player player, int slot) {
        if (!player.hasPermission("lifeachess.admin")) {
            return;
        }
        switch (slot) {
            case 10 -> openTables(player, true);
            case 12 -> {
                player.closeInventory();
                plugin.matches().abortAll();
                player.sendMessage(Text.mm(msg().prefix() + "<yellow>All matches aborted.</yellow>"));
            }
            case 14 -> {
                player.closeInventory();
                plugin.reloadAll();
                msg().send(player, "general.reloaded");
            }
            case 22 -> openHub(player);
            default -> {
            }
        }
    }
}
