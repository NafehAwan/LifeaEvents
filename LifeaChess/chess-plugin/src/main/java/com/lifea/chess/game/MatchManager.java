package com.lifea.chess.game;

import com.lifea.chess.LifeaChessPlugin;
import com.lifea.chess.config.Messages;
import com.lifea.chess.engine.Color;
import com.lifea.chess.engine.GameResult;
import com.lifea.chess.engine.Move;
import com.lifea.chess.engine.MoveRecord;
import com.lifea.chess.engine.Piece;
import com.lifea.chess.engine.PieceType;
import com.lifea.chess.engine.Square;
import com.lifea.chess.match.Invitation;
import com.lifea.chess.match.InvitationService;
import com.lifea.chess.match.Match;
import com.lifea.chess.match.TimeControl;
import com.lifea.chess.table.ChessTable;
import com.lifea.chess.table.SeatRef;
import com.lifea.chess.util.Sounds;
import com.lifea.chess.util.Text;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Orchestrates everything around a match: seating, lobbies, challenges and
 * matchmaking, move input from any UI, clocks, disconnects, endings, ratings,
 * rematches and spectators. All methods run on the main thread.
 */
public final class MatchManager {
    private static final long END_SCREEN_MILLIS = 20_000L;

    public record TableLobby(TimeControl timeControl, boolean ranked) {
    }

    private record QueueEntry(UUID player, TimeControl timeControl, boolean ranked) {
    }

    private final LifeaChessPlugin plugin;
    private final InvitationService invitations;
    private final Map<UUID, ActiveMatch> byId = new LinkedHashMap<>();
    private final Map<UUID, ActiveMatch> byPlayer = new HashMap<>();
    private final Map<UUID, ActiveMatch> bySpectator = new HashMap<>();
    private final Map<String, ActiveMatch> byTable = new HashMap<>();
    private final Map<String, TableLobby> lobbies = new HashMap<>();
    private final Map<UUID, UUID> lastOpponent = new HashMap<>();
    private final Map<UUID, TableLobby> lastSettings = new HashMap<>();
    private final Map<UUID, Long> endedAt = new HashMap<>();
    private final List<QueueEntry> queue = new ArrayList<>();
    private final Map<UUID, Integer> lowTimeWarned = new HashMap<>();
    private BukkitTask ticker;
    private int tick;

    public MatchManager(LifeaChessPlugin plugin) {
        this.plugin = plugin;
        this.invitations = new InvitationService(plugin.config().inviteExpiryMillis);
    }

    public void start() {
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    public void shutdown() {
        if (ticker != null) {
            ticker.cancel();
        }
    }

    private Messages msg() {
        return plugin.messages();
    }

    // ------------------------------------------------------------------ lookups

    public ActiveMatch matchOf(UUID player) {
        return byPlayer.get(player);
    }

    public ActiveMatch matchOf(Player player) {
        return byPlayer.get(player.getUniqueId());
    }

    public ActiveMatch spectating(UUID player) {
        return bySpectator.get(player);
    }

    public ActiveMatch atTable(ChessTable table) {
        return byTable.get(table.id());
    }

    public Collection<ActiveMatch> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int activeCount() {
        int n = 0;
        for (ActiveMatch am : byId.values()) {
            if (!am.match().isFinished()) {
                n++;
            }
        }
        return n;
    }

    public InvitationService invitations() {
        return invitations;
    }

    public TableLobby lobby(ChessTable table) {
        return lobbies.getOrDefault(table.id(), new TableLobby(plugin.config().defaultTimeControl, plugin.config().rankedEnabled));
    }

    public void setLobby(ChessTable table, TimeControl tc, boolean ranked) {
        lobbies.put(table.id(), new TableLobby(tc, ranked));
        plugin.bigBoards().update(table, atTable(table));
    }

    public TableLobby lastSettings(UUID player) {
        return lastSettings.getOrDefault(player, new TableLobby(plugin.config().defaultTimeControl, plugin.config().rankedEnabled));
    }

    public String modeLabel(boolean ranked) {
        return msg().string(ranked ? "mode.ranked" : "mode.casual");
    }

    // ------------------------------------------------------------------ seating

    /** Seats a player at a chair; starts the match when the second chair fills. */
    public boolean sitAt(Player player, ChessTable table, int index) {
        if (plugin.config().isWorldDisabled(player.getWorld().getName())) {
            msg().send(player, "general.disabled-world");
            return false;
        }
        if (!table.isComplete()) {
            msg().send(player, "table.incomplete", "table", table.id());
            return false;
        }
        ActiveMatch existing = matchOf(player);
        ActiveMatch here = atTable(table);
        if (existing != null && existing != here && !existing.match().isFinished()) {
            msg().send(player, "invite.you-busy");
            return false;
        }
        if (here != null && !here.match().isFinished()) {
            // Only a disconnected/stood-up participant may re-take their own chair.
            Color c = here.match().colorOf(player.getUniqueId());
            if (c == null || (c == Color.WHITE ? ChessTable.WHITE_SEAT : ChessTable.BLACK_SEAT) != index) {
                msg().send(player, "table.in-use");
                return false;
            }
        }
        Player occupant = plugin.seats().occupant(table, index);
        if (occupant != null && !occupant.equals(player)) {
            msg().send(player, "table.seat-taken");
            return false;
        }
        leaveQueue(player, false);
        stopSpectating(player, false);
        if (!plugin.seats().sit(player, table, index)) {
            return false;
        }
        Sounds.select(player);
        if (here != null && !here.match().isFinished()) {
            here.match().markReconnected(player.getUniqueId());
            broadcast(here, "match.reconnected", "player", player.getName());
            if (plugin.config().autoOpenBoard) {
                plugin.screens().open(player, here);
            }
            refresh(here);
            return true;
        }
        Player other = plugin.seats().occupant(table, 1 - index);
        if (other == null) {
            TableLobby lobby = lobby(table);
            msg().send(player, "table.seated", "color", index == ChessTable.WHITE_SEAT ? "White" : "Black");
            msg().send(player, "lobby.settings", "time_control", lobby.timeControl().notation(), "mode", modeLabel(lobby.ranked()));
            plugin.menus().openTimeControls(player, com.lifea.chess.ui.MenuService.Purpose.TABLE_LOBBY, null, table.id());
            plugin.bigBoards().update(table, null);
            return true;
        }
        msg().send(player, "table.seated-vs", "color", index == ChessTable.WHITE_SEAT ? "White" : "Black", "opponent", other.getName());
        startMatch(table);
        return true;
    }

    /** Called when a player stops sitting (dismount, sneak, death, world change). */
    public void onStoodUp(Player player, SeatRef seat) {
        if (seat == null) {
            return;
        }
        ActiveMatch am = atTable(seat.table());
        if (am == null || am.match().isFinished()) {
            if (am == null) {
                plugin.bigBoards().update(seat.table(), null);
            }
            msg().send(player, "table.stood-up");
            return;
        }
        if (am.match().isPlayer(player.getUniqueId())) {
            plugin.screens().close(player);
            am.match().markDisconnected(player.getUniqueId());
            broadcast(am, "match.stood-up", "player", player.getName(),
                    "seconds", String.valueOf(plugin.config().disconnectPolicy.graceMillis() / 1000L));
            refresh(am);
        }
    }

    private void startMatch(ChessTable table) {
        Player white = plugin.seats().occupant(table, ChessTable.WHITE_SEAT);
        Player black = plugin.seats().occupant(table, ChessTable.BLACK_SEAT);
        if (white == null || black == null) {
            return;
        }
        TableLobby lobby = lobby(table);
        boolean ranked = lobby.ranked() && plugin.config().rankedEnabled;
        Match match = new Match(white.getUniqueId(), white.getName(), black.getUniqueId(), black.getName(),
                lobby.timeControl(), ranked, plugin.config().disconnectPolicy);
        ActiveMatch am = new ActiveMatch(match, table);
        byId.put(am.id(), am);
        byPlayer.put(white.getUniqueId(), am);
        byPlayer.put(black.getUniqueId(), am);
        byTable.put(table.id(), am);
        plugin.tables().setBusy(table, true);
        lastOpponent.put(white.getUniqueId(), black.getUniqueId());
        lastOpponent.put(black.getUniqueId(), white.getUniqueId());
        lastSettings.put(white.getUniqueId(), lobby);
        lastSettings.put(black.getUniqueId(), lobby);
        match.start();
        for (Player p : List.of(white, black)) {
            msg().send(p, "match.started", "white", white.getName(), "black", black.getName(),
                    "time_control", lobby.timeControl().notation(), "mode", modeLabel(ranked));
            Sounds.start(p);
            if (plugin.config().autoOpenBoard) {
                plugin.screens().open(p, am);
            }
        }
        msg().send(white, "match.your-turn");
        refresh(am);
    }

    // ------------------------------------------------------------------ challenges & queue

    public boolean challenge(Player from, Player to, TimeControl tc, boolean ranked) {
        if (from.equals(to)) {
            msg().send(from, "invite.self");
            return false;
        }
        if (!modeAllowed(from, ranked)) {
            return false;
        }
        if (isBusy(from)) {
            msg().send(from, "invite.you-busy");
            return false;
        }
        if (isBusy(to)) {
            msg().send(from, "invite.target-busy", "player", to.getName());
            return false;
        }
        InvitationService.SendResult r = invitations.send(from.getUniqueId(), from.getName(), to.getUniqueId(), to.getName(), tc, ranked);
        if (r.failure() == InvitationService.Failure.ALREADY_PENDING) {
            msg().send(from, "invite.already-pending", "player", to.getName());
            return false;
        }
        if (!r.ok()) {
            msg().send(from, "invite.self");
            return false;
        }
        String mode = modeLabel(ranked);
        msg().send(from, "invite.sent", "player", to.getName(), "time_control", tc.notation(), "mode", mode,
                "seconds", String.valueOf(plugin.config().inviteExpiryMillis / 1000L));
        msg().send(to, "invite.received", "player", from.getName(), "time_control", tc.notation(), "mode", mode);
        msg().sendRaw(to, "invite.received-buttons");
        Sounds.invite(to);
        return true;
    }

    public boolean accept(Player player) {
        Invitation inv = invitations.accept(player.getUniqueId()).orElse(null);
        if (inv == null) {
            msg().send(player, "invite.none-pending");
            return false;
        }
        Player from = Bukkit.getPlayer(inv.from());
        if (from == null || !from.isOnline()) {
            msg().send(player, "invite.target-offline");
            return false;
        }
        if (isBusy(from)) {
            msg().send(player, "invite.target-busy", "player", from.getName());
            return false;
        }
        if (isBusy(player)) {
            msg().send(player, "invite.you-busy");
            return false;
        }
        msg().send(from, "invite.accepted", "player", player.getName());
        return pairAtTable(from, player, inv.timeControl(), inv.ranked());
    }

    public boolean deny(Player player) {
        Invitation inv = invitations.deny(player.getUniqueId()).orElse(null);
        if (inv == null) {
            msg().send(player, "invite.none-pending");
            return false;
        }
        Player from = Bukkit.getPlayer(inv.from());
        if (from != null) {
            msg().send(from, "invite.denied", "player", player.getName());
        }
        return true;
    }

    public boolean cancel(Player player) {
        Invitation inv = invitations.cancel(player.getUniqueId()).orElse(null);
        if (inv == null) {
            msg().send(player, "invite.none-pending");
            return false;
        }
        msg().send(player, "invite.cancelled", "player", inv.toName());
        Player to = Bukkit.getPlayer(inv.to());
        if (to != null) {
            msg().send(to, "invite.cancelled-notice", "player", player.getName());
        }
        return true;
    }

    public boolean rematch(Player player) {
        UUID oppId = lastOpponent.get(player.getUniqueId());
        Player opp = oppId == null ? null : Bukkit.getPlayer(oppId);
        if (opp == null) {
            msg().send(player, "match.rematch-none");
            return false;
        }
        TableLobby s = lastSettings(player.getUniqueId());
        // If the opponent already offered, accept immediately.
        Invitation pending = invitations.latestFor(player.getUniqueId()).orElse(null);
        if (pending != null && pending.from().equals(oppId)) {
            return accept(player);
        }
        if (challenge(player, opp, s.timeControl(), s.ranked())) {
            msg().send(player, "match.rematch-offered", "player", opp.getName());
            return true;
        }
        return false;
    }

    public boolean joinQueue(Player player, TimeControl tc, boolean ranked) {
        if (!modeAllowed(player, ranked) || isBusy(player)) {
            if (isBusy(player)) {
                msg().send(player, "invite.you-busy");
            }
            return false;
        }
        if (inQueue(player)) {
            msg().send(player, "queue.already");
            return false;
        }
        for (Iterator<QueueEntry> it = queue.iterator(); it.hasNext(); ) {
            QueueEntry e = it.next();
            Player other = Bukkit.getPlayer(e.player());
            if (other == null || !other.isOnline() || isBusy(other)) {
                it.remove();
                continue;
            }
            if (e.ranked() == ranked && e.timeControl().equals(tc)) {
                it.remove();
                return pairAtTable(other, player, tc, ranked);
            }
        }
        queue.add(new QueueEntry(player.getUniqueId(), tc, ranked));
        msg().send(player, "queue.joined", "time_control", tc.notation(), "mode", modeLabel(ranked));
        return true;
    }

    public boolean inQueue(Player player) {
        for (QueueEntry e : queue) {
            if (e.player().equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public boolean leaveQueue(Player player, boolean notify) {
        boolean removed = queue.removeIf(e -> e.player().equals(player.getUniqueId()));
        if (removed && notify) {
            msg().send(player, "queue.left");
        }
        return removed;
    }

    private boolean modeAllowed(Player p, boolean ranked) {
        if (ranked && !plugin.config().rankedEnabled) {
            msg().send(p, "invite.ranked-disabled");
            return false;
        }
        if (!ranked && !plugin.config().casualEnabled) {
            msg().send(p, "invite.casual-disabled");
            return false;
        }
        return true;
    }

    public boolean isBusy(Player p) {
        ActiveMatch am = matchOf(p);
        return am != null && !am.match().isFinished();
    }

    /** Finds a free table, seats both players (first as White) and starts. */
    private boolean pairAtTable(Player a, Player b, TimeControl tc, boolean ranked) {
        ChessTable table = plugin.tables().freeTable(a.getLocation());
        if (table == null) {
            msg().send(a, "queue.no-table");
            msg().send(b, "queue.no-table");
            return false;
        }
        leaveQueue(a, false);
        leaveQueue(b, false);
        setLobby(table, tc, ranked);
        plugin.seats().standAt(table);
        boolean whiteFirst = Math.random() < 0.5;
        Player white = whiteFirst ? a : b;
        Player black = whiteFirst ? b : a;
        Location anchor = table.anchor();
        if (anchor != null) {
            msg().send(a, "table.teleported", "table", table.id());
            msg().send(b, "table.teleported", "table", table.id());
        }
        if (!plugin.seats().sit(white, table, ChessTable.WHITE_SEAT)) {
            return false;
        }
        if (!plugin.seats().sit(black, table, ChessTable.BLACK_SEAT)) {
            plugin.seats().stand(white);
            return false;
        }
        startMatch(table);
        return true;
    }

    // ------------------------------------------------------------------ moves

    /** Board click from any UI: selects, re-selects, cancels, or moves. */
    public void onSquareClick(Player player, int square) {
        ActiveMatch am = matchOf(player);
        if (am == null || !am.match().isActive()) {
            return;
        }
        UUID id = player.getUniqueId();
        Color mine = am.match().colorOf(id);
        if (mine == null || am.match().game().sideToMove() != mine) {
            Sounds.error(player);
            return;
        }
        if (am.pendingPromotion(id) != null) {
            return;
        }
        Integer sel = am.selected(id);
        Piece target = am.match().game().position().pieceAt(square);
        if (sel == null) {
            if (target != null && target.color() == mine && !am.match().game().legalMovesFrom(square).isEmpty()) {
                am.select(id, square);
                Sounds.select(player);
                plugin.screens().refreshFor(player, am);
            }
            return;
        }
        if (sel == square) {
            am.select(id, null);
            plugin.screens().refreshFor(player, am);
            return;
        }
        if (target != null && target.color() == mine) {
            am.select(id, square);
            Sounds.select(player);
            plugin.screens().refreshFor(player, am);
            return;
        }
        Move candidate = null;
        boolean promotion = false;
        for (Move m : am.match().game().legalMovesFrom(sel)) {
            if (m.to() == square) {
                candidate = m;
                promotion = m.isPromotion();
                break;
            }
        }
        if (candidate == null) {
            am.select(id, null);
            plugin.screens().refreshFor(player, am);
            return;
        }
        if (promotion) {
            am.setPendingPromotion(id, new Move(sel, square));
            plugin.screens().refreshFor(player, am);
            if (!plugin.screens().available()) {
                plugin.menus().openPromotion(player);
            }
            return;
        }
        playMove(player, candidate);
    }

    public void completePromotion(Player player, PieceType piece) {
        ActiveMatch am = matchOf(player);
        if (am == null) {
            return;
        }
        Move base = am.pendingPromotion(player.getUniqueId());
        if (base == null) {
            return;
        }
        am.setPendingPromotion(player.getUniqueId(), null);
        playMove(player, new Move(base.from(), base.to(), piece));
    }

    public void cancelPromotion(Player player) {
        ActiveMatch am = matchOf(player);
        if (am != null) {
            am.setPendingPromotion(player.getUniqueId(), null);
            am.select(player.getUniqueId(), null);
            plugin.screens().refreshFor(player, am);
        }
    }

    /** Plays a move for the player; returns true when it was accepted. */
    public boolean playMove(Player player, Move move) {
        ActiveMatch am = matchOf(player);
        if (am == null) {
            msg().send(player, "match.not-in-match");
            return false;
        }
        Match.MoveOutcome out = am.match().playMove(player.getUniqueId(), move);
        if (!out.ok()) {
            switch (out.failure()) {
                case NOT_YOUR_TURN -> msg().send(player, "match.not-your-turn");
                case ILLEGAL_MOVE -> msg().send(player, "match.illegal-move");
                case FLAGGED -> {
                    finish(am);
                    return false;
                }
                default -> msg().send(player, "match.not-in-match");
            }
            Sounds.error(player);
            return false;
        }
        am.clearUiState(player.getUniqueId());
        am.touch();
        lowTimeWarned.remove(am.id());
        MoveRecord rec = out.record();
        for (UUID v : am.viewers()) {
            Player p = Bukkit.getPlayer(v);
            if (p == null) {
                continue;
            }
            if (rec.captured() != null) {
                Sounds.capture(p);
            } else {
                Sounds.move(p);
            }
            if (rec.check() && !rec.checkmate()) {
                Sounds.check(p);
            }
            if (move.isPromotion()) {
                Sounds.promotion(p);
            }
        }
        Player opponent = Bukkit.getPlayer(am.match().opponentOf(player.getUniqueId()));
        if (opponent != null && !am.match().isFinished()) {
            opponent.sendActionBar(msg().raw("match.your-turn"));
            if (rec.check()) {
                msg().send(opponent, "match.check");
            }
        }
        if (am.match().isFinished()) {
            finish(am);
        } else {
            refresh(am);
        }
        return true;
    }

    public boolean resign(Player player) {
        ActiveMatch am = matchOf(player);
        if (am == null || am.match().isFinished()) {
            msg().send(player, "match.not-in-match");
            return false;
        }
        if (am.match().resign(player.getUniqueId())) {
            broadcast(am, "match.resigned", "player", player.getName());
            finish(am);
            return true;
        }
        return false;
    }

    public void drawAction(Player player) {
        ActiveMatch am = matchOf(player);
        if (am == null || !am.match().isActive()) {
            msg().send(player, "match.not-in-match");
            return;
        }
        Match.DrawAction action = am.match().offerOrAcceptDraw(player.getUniqueId());
        switch (action) {
            case OFFERED -> {
                msg().send(player, "match.draw-offered");
                Player opp = Bukkit.getPlayer(am.match().opponentOf(player.getUniqueId()));
                if (opp != null) {
                    msg().send(opp, "match.draw-offer-received", "player", player.getName());
                    Sounds.invite(opp);
                }
                refresh(am);
            }
            case ACCEPTED -> finish(am);
            case CLAIMED -> {
                broadcast(am, "match.draw-claimed", "reason", msg().reason(am.match().result().reason()));
                finish(am);
            }
            case REJECTED -> msg().send(player, "match.draw-already-offered");
            default -> {
            }
        }
    }

    public void declineDraw(Player player) {
        ActiveMatch am = matchOf(player);
        if (am == null) {
            return;
        }
        if (am.match().declineDraw(player.getUniqueId()) == Match.DrawAction.DECLINED) {
            broadcast(am, "match.draw-declined", "player", player.getName());
            refresh(am);
        }
    }

    /** Leaves whatever the player is doing: queue, spectating, a pending seat, or an active match (forfeit). */
    public void leave(Player player) {
        if (leaveQueue(player, true)) {
            return;
        }
        if (stopSpectating(player, true)) {
            return;
        }
        ActiveMatch am = matchOf(player);
        if (am != null && !am.match().isFinished()) {
            am.match().resign(player.getUniqueId());
            broadcast(am, "match.resigned", "player", player.getName());
            finish(am);
        }
        SeatRef seat = plugin.seats().stand(player);
        if (seat != null) {
            msg().send(player, "match.left");
            if (atTable(seat.table()) == null) {
                plugin.bigBoards().update(seat.table(), null);
            }
        } else if (am == null) {
            msg().send(player, "match.not-in-match");
        }
        plugin.screens().close(player);
    }

    // ------------------------------------------------------------------ spectating

    public boolean spectate(Player player, Player target) {
        if (!plugin.config().spectatorsEnabled) {
            msg().send(player, "match.spectators-disabled");
            return false;
        }
        ActiveMatch am = matchOf(target);
        if (am == null || am.match().isFinished()) {
            msg().send(player, "match.spectate-none", "player", target.getName());
            return false;
        }
        if (isBusy(player)) {
            msg().send(player, "invite.you-busy");
            return false;
        }
        stopSpectating(player, false);
        am.match().addSpectator(player.getUniqueId());
        bySpectator.put(player.getUniqueId(), am);
        msg().send(player, "match.spectating", "white", am.match().whiteName(), "black", am.match().blackName());
        plugin.screens().open(player, am);
        return true;
    }

    public boolean stopSpectating(Player player, boolean notify) {
        ActiveMatch am = bySpectator.remove(player.getUniqueId());
        if (am == null) {
            return false;
        }
        am.match().removeSpectator(player.getUniqueId());
        plugin.screens().close(player);
        if (notify) {
            msg().send(player, "match.spectate-stop");
        }
        return true;
    }

    // ------------------------------------------------------------------ connection

    public void onQuit(Player player) {
        invitations.clearInvolving(player.getUniqueId());
        leaveQueue(player, false);
        stopSpectating(player, false);
        ActiveMatch am = matchOf(player);
        SeatRef seat = plugin.seats().stand(player);
        plugin.screens().close(player);
        if (am != null && !am.match().isFinished()) {
            am.match().markDisconnected(player.getUniqueId());
            broadcast(am, "match.disconnected", "player", player.getName(),
                    "seconds", String.valueOf(plugin.config().disconnectPolicy.graceMillis() / 1000L));
            refresh(am);
        } else if (seat != null && am == null) {
            plugin.bigBoards().update(seat.table(), null);
        }
    }

    public void onJoin(Player player) {
        ActiveMatch am = matchOf(player);
        if (am == null || am.match().isFinished()) {
            return;
        }
        Color c = am.match().colorOf(player.getUniqueId());
        int seat = c == Color.WHITE ? ChessTable.WHITE_SEAT : ChessTable.BLACK_SEAT;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !am.match().isFinished()) {
                Location chair = am.table().chair(seat);
                if (chair != null) {
                    player.teleport(chair);
                }
                sitAt(player, am.table(), seat);
            }
        }, 10L);
    }

    // ------------------------------------------------------------------ ticking & endings

    private void tick() {
        tick++;
        invitations.purgeExpired().forEach(inv -> {
            Player from = Bukkit.getPlayer(inv.from());
            if (from != null) {
                msg().send(from, "invite.expired", "player", inv.toName());
            }
        });
        long now = System.currentTimeMillis();
        for (ActiveMatch am : new ArrayList<>(byId.values())) {
            Match m = am.match();
            if (m.isFinished()) {
                Long t = endedAt.get(am.id());
                if (t != null && now - t > END_SCREEN_MILLIS) {
                    cleanup(am);
                }
                continue;
            }
            if (m.checkFlag() != null) {
                Color flagged = m.result().winner() == null ? m.game().sideToMove() : m.result().winner().opposite();
                broadcast(am, "match.flag", "player", m.nameOf(flagged));
                finish(am);
                continue;
            }
            if (m.checkDisconnectTimeout()) {
                broadcast(am, "match.forfeited", "player", m.nameOf(m.result().winner().opposite()));
                finish(am);
                continue;
            }
            if (m.isActive() && tick % 4 == 0) {
                plugin.screens().refreshClocks(am);
                plugin.bigBoards().updateHeader(am.table(), am);
                Color side = m.game().sideToMove();
                long left = m.clock().remaining(side);
                int warned = lowTimeWarned.getOrDefault(am.id(), 0);
                if (left <= 10_000L && warned != 10) {
                    lowTimeWarned.put(am.id(), 10);
                    Player p = Bukkit.getPlayer(m.idOf(side));
                    if (p != null) {
                        p.sendActionBar(msg().raw("match.low-time", "seconds", "10"));
                        Sounds.lowTime(p);
                    }
                }
            }
        }
    }

    /** Ends a match: announce, rate, persist, show end screens, free the table. */
    private void finish(ActiveMatch am) {
        Match m = am.match();
        if (!m.isFinished()) {
            return;
        }
        if (endedAt.containsKey(am.id())) {
            return;
        }
        endedAt.put(am.id(), System.currentTimeMillis());
        plugin.tables().setBusy(am.table(), false);
        GameResult result = m.result();
        String reason = msg().reason(result.reason());
        String duration = Text.duration(m.duration());
        for (UUID v : am.viewers()) {
            Player p = Bukkit.getPlayer(v);
            if (p == null) {
                continue;
            }
            msg().sendRaw(p, "result.header");
            if (result.isDraw()) {
                msg().sendRaw(p, "result.draw", "reason", reason, "duration", duration);
                Sounds.draw(p);
            } else {
                msg().sendRaw(p, "result.win", "winner", m.nameOf(result.winner()), "reason", reason, "duration", duration);
                Color mine = m.colorOf(v);
                if (mine == null) {
                    Sounds.draw(p);
                } else if (mine == result.winner()) {
                    Sounds.victory(p);
                } else {
                    Sounds.defeat(p);
                }
            }
            if (result.reason() == GameResult.Reason.CHECKMATE) {
                Sounds.checkmate(p);
            }
        }
        refresh(am);
        plugin.stats().applyResult(am, applied -> {
            for (UUID v : am.viewers()) {
                Player p = Bukkit.getPlayer(v);
                if (p == null) {
                    continue;
                }
                if (applied.rated()) {
                    msg().sendRaw(p, "result.rating-change", "player", m.whiteName(),
                            "rating", String.valueOf(applied.white().stats().rating()), "delta", Text.signed(applied.whiteDelta()));
                    msg().sendRaw(p, "result.rating-change", "player", m.blackName(),
                            "rating", String.valueOf(applied.black().stats().rating()), "delta", Text.signed(applied.blackDelta()));
                } else if (applied.farmBlocked()) {
                    msg().sendRaw(p, "result.rating-farm");
                } else {
                    msg().sendRaw(p, "result.rating-unchanged");
                }
                if (m.isPlayer(v)) {
                    msg().sendRaw(p, "result.rematch-hint");
                }
            }
            plugin.screens().refresh(am);
        });
    }

    private void cleanup(ActiveMatch am) {
        byId.remove(am.id());
        endedAt.remove(am.id());
        lowTimeWarned.remove(am.id());
        if (byTable.get(am.table().id()) == am) {
            byTable.remove(am.table().id());
        }
        for (UUID p : List.of(am.match().whiteId(), am.match().blackId())) {
            if (byPlayer.get(p) == am) {
                byPlayer.remove(p);
            }
        }
        for (UUID s : new ArrayList<>(am.match().spectators())) {
            if (bySpectator.get(s) == am) {
                bySpectator.remove(s);
            }
        }
        plugin.screens().closeAll(am);
        plugin.bigBoards().update(am.table(), null);
        // Players who are still seated get the lobby prompt again so they can rematch at the table.
        for (int i = 0; i < 2; i++) {
            Player p = plugin.seats().occupant(am.table(), i);
            if (p != null && plugin.seats().occupant(am.table(), 1 - i) != null) {
                startMatch(am.table());
                return;
            }
        }
    }

    /** Admin stop: ends without rating. */
    public void abort(ActiveMatch am, boolean notify) {
        if (!am.match().isFinished()) {
            am.match().abort();
            am.markProcessed(false, 0, 0);
            if (notify) {
                broadcast(am, "match.aborted");
            }
        }
        plugin.tables().setBusy(am.table(), false);
        cleanup(am);
    }

    public void abortAll() {
        for (ActiveMatch am : new ArrayList<>(byId.values())) {
            abort(am, true);
        }
        queue.clear();
    }

    public boolean abortAt(ChessTable table) {
        ActiveMatch am = atTable(table);
        if (am == null) {
            return false;
        }
        abort(am, true);
        return true;
    }

    // ------------------------------------------------------------------ helpers

    private void refresh(ActiveMatch am) {
        plugin.screens().refresh(am);
        plugin.bigBoards().update(am.table(), am);
    }

    private void broadcast(ActiveMatch am, String key, String... pairs) {
        for (UUID v : am.viewers()) {
            Player p = Bukkit.getPlayer(v);
            if (p != null) {
                msg().send(p, key, pairs);
            }
        }
    }

    /** Human-readable square name helper for chat fallbacks. */
    public static String squareName(int sq) {
        return Square.name(sq);
    }
}
