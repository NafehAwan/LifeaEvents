package com.lifea.chess.ui;

import com.lifea.chess.LifeaChessPlugin;
import com.lifea.chess.config.Theme;
import com.lifea.chess.engine.Color;
import com.lifea.chess.engine.Game;
import com.lifea.chess.engine.GameResult;
import com.lifea.chess.engine.Move;
import com.lifea.chess.engine.PieceType;
import com.lifea.chess.game.ActiveMatch;
import com.lifea.chess.util.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.exylia.lib.screen.Node;
import net.exylia.lib.screen.Screen;
import net.exylia.lib.screen.ScreenEvent;
import net.exylia.lib.screen.ScreenSession;
import net.exylia.lib.screen.Screens;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * The personal full-view packet board (reference image 1), drawn with ExyliaLib
 * Screens: header "CHESS » MODE", status "YOUR TURN » 4:07 ⏱ ♟39 vs 39", an
 * 8x8 board, side column with names/clocks/captures/moves, Resign/Draw/Close
 * controls, a promotion dialog and the match-end overlay.
 *
 * <p><b>ExyliaLib surface used (verify against docs/screens.md when compiling):</b>
 * {@code Screens.of(plugin).open(player, screen)}, {@code Screen.of(session -> node)},
 * {@code Screen#onClose}, {@code Node.row/column/text/box}, {@code width/height/padding/gap/
 * background/hover/grow/scale/align/justify/id/onClick/overlay/onDismiss},
 * {@code ScreenEvent#session()}, {@code ScreenSession#set(key, value)} to trigger a
 * redraw and {@code ScreenSession#back()} to close. Every other class in this
 * plugin is plain Paper API; if a signature here differs in the installed
 * ExyliaLib build, the fix is local to this file.
 */
public final class ScreenBoardView {
    private static final int SQ = 22;

    private final LifeaChessPlugin plugin;
    private final Map<UUID, ScreenSession> sessions = new HashMap<>();
    private final Map<UUID, UUID> matchOfViewer = new HashMap<>();
    private final Map<UUID, Integer> revisions = new HashMap<>();
    private Boolean available;

    public ScreenBoardView(LifeaChessPlugin plugin) {
        this.plugin = plugin;
    }

    /** True when ExyliaLib Screens and PacketEvents are present. */
    public boolean available() {
        if (available == null) {
            boolean lib;
            try {
                Class.forName("net.exylia.lib.screen.Screens");
                lib = true;
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                lib = false;
            }
            boolean pe = Bukkit.getPluginManager().getPlugin("packetevents") != null
                    || Bukkit.getPluginManager().getPlugin("PacketEvents") != null;
            available = lib && pe;
            if (!available) {
                plugin.getLogger().warning("Screen board unavailable: ExyliaLib Screens or PacketEvents missing. "
                        + "Players can still play on the table board with /chess move.");
            }
        }
        return available;
    }

    public void open(Player player, ActiveMatch am) {
        if (!available()) {
            plugin.messages().send(player, "match.screens-unavailable");
            return;
        }
        UUID id = player.getUniqueId();
        matchOfViewer.put(id, am.id());
        try {
            Screen screen = Screen.of(session -> {
                sessions.put(id, session);
                return render(player, am);
            }).onClose(session -> {
                sessions.remove(id);
                matchOfViewer.remove(id);
            });
            Screens.of(plugin).open(player, screen);
        } catch (RuntimeException | LinkageError e) {
            plugin.getLogger().warning("Could not open screen board for " + player.getName() + ": " + e);
            plugin.messages().send(player, "match.screens-unavailable");
        }
    }

    public void close(Player player) {
        UUID id = player.getUniqueId();
        ScreenSession s = sessions.remove(id);
        matchOfViewer.remove(id);
        if (s != null) {
            try {
                s.back();
            } catch (RuntimeException | LinkageError ignored) {
                // Shift closes the screen client-side as a fallback.
            }
        }
    }

    public void closeAll(ActiveMatch am) {
        for (UUID v : new ArrayList<>(am.viewers())) {
            Player p = Bukkit.getPlayer(v);
            if (p != null) {
                close(p);
            }
        }
    }

    /** Redraws every viewer of the match. */
    public void refresh(ActiveMatch am) {
        for (UUID v : am.viewers()) {
            Player p = Bukkit.getPlayer(v);
            if (p != null) {
                refreshFor(p, am);
            }
        }
    }

    /** Redraws one viewer's screen (after a selection change, promotion prompt, etc.). */
    public void refreshFor(Player player, ActiveMatch am) {
        ScreenSession s = sessions.get(player.getUniqueId());
        if (s == null) {
            return;
        }
        int rev = revisions.merge(player.getUniqueId(), 1, Integer::sum);
        try {
            s.set("rev", rev);
        } catch (RuntimeException | LinkageError ignored) {
            // The screen also re-renders once a second on its own.
        }
    }

    /** Cheap per-second update for the clocks. */
    public void refreshClocks(ActiveMatch am) {
        refresh(am);
    }

    // ------------------------------------------------------------------ rendering

    private Node render(Player viewer, ActiveMatch am) {
        Theme t = plugin.config().theme;
        UUID id = viewer.getUniqueId();
        Game game = am.match().game();
        Color mine = am.match().colorOf(id);
        boolean flipped = mine == Color.BLACK;
        boolean spectator = mine == null;

        Node header = Node.row(
                Node.text("<color:" + t.accent() + "><b>CHESS</b></color> <color:" + t.muted() + ">»</color> "
                        + "<color:" + t.positive() + "><b>" + (am.match().ranked() ? "RANKED" : "CASUAL") + "</b></color>"
                        + " <color:" + t.muted() + ">" + am.match().timeControl().notation() + "</color>").grow(1),
                Node.text("<color:" + t.muted() + ">" + am.match().whiteName() + " vs " + am.match().blackName() + "</color>")
        ).gap(6);

        Color stm = game.sideToMove();
        String status = "<color:" + t.clock() + "><b>" + BoardStyle.turnLabel(am, id) + "</b></color> "
                + "<color:" + t.muted() + ">»</color> <color:" + t.clock() + ">" + BoardStyle.clock(am, stm) + " ⏱</color> "
                + "<color:" + t.text() + ">" + BoardStyle.materialLine(game.position()) + "</color>";
        Node statusRow = Node.text(status);

        List<Node> rows = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            List<Node> cells = new ArrayList<>();
            for (int c = 0; c < 8; c++) {
                int sq = BoardStyle.squareAt(r, c, flipped);
                BoardStyle.SquareLook look = BoardStyle.look(t, am, id, sq);
                Node cell = Node.column(Node.text("<color:" + look.glyphColor() + "><b>" + look.glyph() + "</b></color>").scale(1.6))
                        .width(SQ).height(SQ)
                        .background(look.background())
                        .align(Node.Align.CENTER).justify(Node.Justify.CENTER)
                        .id("sq-" + sq);
                if (!spectator && !am.match().isFinished()) {
                    cell = cell.hover(Theme.alpha(0xAA, t.selected()))
                            .onClick(e -> plugin.matches().onSquareClick(viewer, sq));
                }
                cells.add(cell);
            }
            rows.add(Node.row(cells.toArray(new Node[0])).gap(0).id("row-" + r));
        }
        Node board = Node.column(rows.toArray(new Node[0])).gap(0).padding(2).background(t.background()).id("board");

        Color top = flipped ? Color.WHITE : Color.BLACK;
        Color bottom = top.opposite();
        Node side = Node.column(
                playerCard(t, am, top, stm == top),
                Node.text("<color:" + t.muted() + ">" + captured(game, top) + "</color>").id("cap-top"),
                Node.text("<color:" + t.muted() + ">" + BoardStyle.recentMoves(game, 8) + "</color>").grow(1).id("moves"),
                Node.text("<color:" + t.muted() + ">" + captured(game, bottom) + "</color>").id("cap-bottom"),
                playerCard(t, am, bottom, stm == bottom),
                controls(t, am, viewer, spectator)
        ).gap(4).padding(4).grow(1).id("side");

        Node main = Node.row(board, side).gap(8).grow(1);
        Node root = Node.column(header, statusRow, main).padding(6).gap(4).background(t.background()).id("root");

        Move promo = am.pendingPromotion(id);
        if (promo != null && !spectator) {
            return Node.column(root, promotionDialog(t, viewer, mine)).id("root-with-dialog");
        }
        if (am.match().isFinished()) {
            return Node.column(root, endDialog(t, am, viewer)).id("root-with-end");
        }
        return root;
    }

    private static String captured(Game game, Color victim) {
        String s = BoardStyle.captured(game, victim);
        return s.isEmpty() ? " " : s;
    }

    private Node playerCard(Theme t, ActiveMatch am, Color color, boolean active) {
        String name = am.match().nameOf(color);
        String clock = BoardStyle.clock(am, color);
        String bg = active ? Theme.alpha(0x66, t.accent()) : Theme.alpha(0x44, t.darkSquare());
        String clockColor = active ? t.clock() : t.muted();
        return Node.row(
                Node.text("<color:" + (color == Color.WHITE ? t.whitePiece() : t.blackPiece()) + "><b>" + name + "</b></color>").grow(1),
                Node.text("<color:" + clockColor + "><b>" + clock + "</b></color>")
        ).padding(3).gap(4).background(bg).id("card-" + color.name());
    }

    private Node controls(Theme t, ActiveMatch am, Player viewer, boolean spectator) {
        List<Node> buttons = new ArrayList<>();
        if (!spectator && am.match().isActive()) {
            buttons.add(button(t, "RESIGN", t.capture(), e -> plugin.matches().resign(viewer)));
            Color offered = am.match().drawOfferedBy();
            Color mine = am.match().colorOf(viewer.getUniqueId());
            String drawLabel = offered != null && offered != mine ? "ACCEPT DRAW" : offered == mine ? "DRAW OFFERED" : "DRAW";
            buttons.add(button(t, drawLabel, t.warning(), e -> plugin.matches().drawAction(viewer)));
            if (offered != null && offered != mine) {
                buttons.add(button(t, "DECLINE", t.muted(), e -> plugin.matches().declineDraw(viewer)));
            }
        }
        buttons.add(button(t, "CLOSE", t.muted(), e -> close(viewer)));
        return Node.row(buttons.toArray(new Node[0])).gap(4).id("controls");
    }

    private Node button(Theme t, String label, String color, java.util.function.Consumer<ScreenEvent> action) {
        return Node.column(Node.text("<color:" + color + "><b>" + label + "</b></color>"))
                .padding(3).background(Theme.alpha(0x55, color)).hover(Theme.alpha(0x99, color))
                .align(Node.Align.CENTER).justify(Node.Justify.CENTER)
                .onClick(action).id("btn-" + label.toLowerCase().replace(' ', '-'));
    }

    private Node promotionDialog(Theme t, Player viewer, Color mine) {
        List<Node> choices = new ArrayList<>();
        for (PieceType pt : List.of(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
            char glyph = com.lifea.chess.engine.Piece.of(mine, pt).glyph();
            choices.add(Node.column(Node.text("<color:" + (mine == Color.WHITE ? t.whitePiece() : t.blackPiece()) + "><b>" + glyph + "</b></color>").scale(2.2))
                    .width(36).height(36).background(t.darkSquare()).hover(t.selected())
                    .align(Node.Align.CENTER).justify(Node.Justify.CENTER)
                    .onClick(e -> plugin.matches().completePromotion(viewer, pt))
                    .id("promo-" + pt.name()));
        }
        return Node.column(
                Node.text("<color:" + t.accent() + "><b>PROMOTE TO</b></color>"),
                Node.row(choices.toArray(new Node[0])).gap(6)
        ).gap(6).padding(10).background(Theme.alpha(0xE6, t.background()))
                .align(Node.Align.CENTER).overlay(true)
                .onDismiss(session -> plugin.matches().cancelPromotion(viewer))
                .id("promotion");
    }

    private Node endDialog(Theme t, ActiveMatch am, Player viewer) {
        GameResult r = am.match().result();
        String title = r.isDraw() ? "<color:" + t.warning() + "><b>DRAW</b></color>"
                : "<color:" + t.positive() + "><b>" + am.match().nameOf(r.winner()) + " WINS</b></color>";
        String reason = "<color:" + t.muted() + ">by " + plugin.messages().reason(r.reason()) + " · " + Text.duration(am.match().duration()) + "</color>";
        List<Node> lines = new ArrayList<>();
        lines.add(Node.text(title).scale(1.5));
        lines.add(Node.text(reason));
        if (am.resultProcessed() && am.rated()) {
            lines.add(Node.text("<color:" + t.text() + ">" + am.match().whiteName() + " " + Text.signed(am.whiteDelta()) + "   "
                    + am.match().blackName() + " " + Text.signed(am.blackDelta()) + "</color>"));
        } else if (am.resultProcessed()) {
            lines.add(Node.text("<color:" + t.muted() + ">casual · ratings unchanged</color>"));
        }
        List<Node> buttons = new ArrayList<>();
        if (am.match().isPlayer(viewer.getUniqueId())) {
            buttons.add(button(t, "REMATCH", t.positive(), e -> {
                close(viewer);
                plugin.matches().rematch(viewer);
            }));
        }
        buttons.add(button(t, "CLOSE", t.muted(), e -> close(viewer)));
        lines.add(Node.row(buttons.toArray(new Node[0])).gap(6));
        return Node.column(lines.toArray(new Node[0])).gap(6).padding(12)
                .background(Theme.alpha(0xE6, t.background())).align(Node.Align.CENTER)
                .overlay(true).onDismiss(session -> close(viewer)).id("end");
    }
}
