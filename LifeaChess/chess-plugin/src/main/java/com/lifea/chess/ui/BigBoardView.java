package com.lifea.chess.ui;

import com.lifea.chess.LifeaChessPlugin;
import com.lifea.chess.config.Theme;
import com.lifea.chess.engine.Color;
import com.lifea.chess.engine.Piece;
import com.lifea.chess.engine.Position;
import com.lifea.chess.game.ActiveMatch;
import com.lifea.chess.game.MatchManager;
import com.lifea.chess.table.ChessTable;
import com.lifea.chess.util.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * The big public board floating above a table (reference image 2): a vertical
 * 8x8 panel of text displays that everyone nearby can watch, a header with the
 * players, clocks and turn, and optional HOW TO PLAY / HOW TO WIN panels.
 * Only squares whose appearance changed are re-sent. Displays are
 * non-persistent, so a crash never leaves junk in the world.
 */
public final class BigBoardView {
    private static final class Board {
        final TextDisplay[] squares = new TextDisplay[64];
        final String[] lastKey = new String[64];
        TextDisplay header;
        TextDisplay panelLeft;
        TextDisplay panelRight;
        String lastHeader = "";
    }

    private final LifeaChessPlugin plugin;
    private final Map<String, Board> boards = new HashMap<>();

    public BigBoardView(LifeaChessPlugin plugin) {
        this.plugin = plugin;
    }

    public void showAll() {
        for (ChessTable t : plugin.tables().all()) {
            if (t.isComplete()) {
                update(t, plugin.matches().atTable(t));
            }
        }
    }

    public void removeAll() {
        for (String id : new ArrayList<>(boards.keySet())) {
            removeBoard(boards.remove(id));
        }
    }

    public void remove(ChessTable table) {
        Board b = boards.remove(table.id());
        if (b != null) {
            removeBoard(b);
        }
    }

    private void removeBoard(Board b) {
        for (TextDisplay d : b.squares) {
            if (d != null && d.isValid()) {
                d.remove();
            }
        }
        for (TextDisplay d : new TextDisplay[] {b.header, b.panelLeft, b.panelRight}) {
            if (d != null && d.isValid()) {
                d.remove();
            }
        }
    }

    /** Draws the table's board: the live match, or the start position with an OPEN header when idle. */
    public void update(ChessTable table, ActiveMatch am) {
        if (!table.isComplete() || table.board() == null || table.board().getWorld() == null) {
            return;
        }
        Board b = boards.get(table.id());
        if (b == null || b.squares[0] == null || !b.squares[0].isValid()) {
            if (b != null) {
                removeBoard(b);
            }
            b = build(table);
            boards.put(table.id(), b);
        }
        Theme theme = plugin.config().theme;
        Position pos = am == null ? Position.initial() : am.match().game().position();
        for (int sq = 0; sq < 64; sq++) {
            BoardStyle.SquareLook look = am == null
                    ? idleLook(theme, pos, sq)
                    : BoardStyle.look(theme, am, null, sq);
            String key = look.background() + "|" + look.glyph() + "|" + look.glyphColor();
            if (key.equals(b.lastKey[sq])) {
                continue;
            }
            b.lastKey[sq] = key;
            TextDisplay d = b.squares[sq];
            d.setBackgroundColor(Theme.bukkit(look.background()));
            d.text(Text.bold(look.glyph(), look.glyphColor()));
        }
        updateHeader(table, am);
    }

    private static BoardStyle.SquareLook idleLook(Theme theme, Position pos, int sq) {
        Piece p = pos.pieceAt(sq);
        String bg = com.lifea.chess.engine.Square.isLight(sq) ? theme.lightSquare() : theme.darkSquare();
        String glyph = p == null ? " " : String.valueOf(p.glyph());
        String color = p == null ? theme.muted() : p.color() == Color.WHITE ? theme.whitePiece() : theme.blackPiece();
        return new BoardStyle.SquareLook(bg, glyph, color, false, false);
    }

    /** Refreshes only the header line (clocks tick once a second). */
    public void updateHeader(ChessTable table, ActiveMatch am) {
        Board b = boards.get(table.id());
        if (b == null || b.header == null || !b.header.isValid()) {
            return;
        }
        Theme theme = plugin.config().theme;
        String text;
        if (am == null) {
            MatchManager.TableLobby lobby = plugin.matches().lobby(table);
            int seated = (plugin.seats().occupant(table, 0) != null ? 1 : 0) + (plugin.seats().occupant(table, 1) != null ? 1 : 0);
            text = "<color:" + theme.accent() + "><b>PLAYING » CHESS</b></color>\n"
                    + "<color:" + theme.positive() + "><b>OPEN</b></color> <gray>·</gray> <white>" + seated + "/2 seated</white>\n"
                    + "<gray>" + lobby.timeControl().notation() + " · " + (lobby.ranked() ? "RANKED" : "CASUAL") + " · sit down to play</gray>";
        } else {
            var m = am.match();
            String turn = m.isFinished()
                    ? (m.result().isDraw() ? "DRAW" : m.nameOf(m.result().winner()) + " WINS")
                    : "TURN » " + m.nameOf(m.game().sideToMove());
            text = "<color:" + theme.accent() + "><b>PLAYING » CHESS</b></color>\n"
                    + "<white><b>" + m.whiteName() + "</b></white> <gray>vs</gray> <white><b>" + m.blackName() + "</b></white>\n"
                    + "<color:" + theme.clock() + ">" + BoardStyle.clock(am, Color.WHITE) + "</color> <gray>|</gray> "
                    + "<color:" + theme.clock() + ">" + BoardStyle.clock(am, Color.BLACK) + "</color> <gray>·</gray> "
                    + "<white>" + turn + "</white> <gray>·</gray> <gray>" + m.timeControl().notation() + " " + (m.ranked() ? "RANKED" : "CASUAL") + "</gray>";
        }
        if (!text.equals(b.lastHeader)) {
            b.lastHeader = text;
            b.header.text(Text.mm(text));
        }
    }

    // ------------------------------------------------------------------ construction

    private Board build(ChessTable table) {
        Board b = new Board();
        Location center = table.board().clone().add(0, plugin.config().boardHeight, 0);
        World world = center.getWorld();
        float yaw = table.boardYaw();
        double s = plugin.config().squareSize;
        double rad = Math.toRadians(yaw);
        // Unit vector along the panel's width (perpendicular to the viewing direction).
        double rx = Math.cos(rad);
        double rz = Math.sin(rad);
        float scale = (float) (s * 3.6);

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int sq = BoardStyle.squareAt(row, col, false);
                double dx = (col - 3.5) * s;
                double dy = (3.5 - row) * s;
                Location loc = center.clone().add(rx * dx, dy, rz * dx);
                loc.setYaw(yaw);
                loc.setPitch(0f);
                b.squares[sq] = spawnText(world, loc, scale, 1, TextDisplay.TextAlignment.CENTER);
            }
        }
        Location headerLoc = center.clone().add(0, 4.8 * s + 0.35, 0);
        headerLoc.setYaw(yaw);
        b.header = spawnText(world, headerLoc, (float) Math.max(0.5, s * 2.2), 260, TextDisplay.TextAlignment.CENTER);
        b.header.setBackgroundColor(Theme.bukkit(Theme.alpha(0xB0, plugin.config().theme.background())));

        if (plugin.config().showHowToPanels) {
            double off = 5.6 * s + 0.6;
            Location left = center.clone().add(-rx * off, 0.4, -rz * off);
            left.setYaw(yaw);
            Location right = center.clone().add(rx * off, 0.4, rz * off);
            right.setYaw(yaw);
            b.panelLeft = spawnText(world, left, (float) Math.max(0.4, s * 1.6), 180, TextDisplay.TextAlignment.LEFT);
            b.panelRight = spawnText(world, right, (float) Math.max(0.4, s * 1.6), 180, TextDisplay.TextAlignment.LEFT);
            b.panelLeft.setBackgroundColor(Theme.bukkit(Theme.alpha(0xB0, plugin.config().theme.background())));
            b.panelRight.setBackgroundColor(Theme.bukkit(Theme.alpha(0xB0, plugin.config().theme.background())));
            b.panelLeft.text(joinLines(plugin.messages().list("howto.play")));
            b.panelRight.text(joinLines(plugin.messages().list("howto.win")));
        }
        return b;
    }

    private static Component joinLines(List<String> lines) {
        return Text.mm(String.join("\n", lines));
    }

    private static TextDisplay spawnText(World world, Location loc, float scale, int lineWidth, TextDisplay.TextAlignment align) {
        return world.spawn(loc, TextDisplay.class, d -> {
            d.setPersistent(false);
            d.setBillboard(Display.Billboard.FIXED);
            d.setAlignment(align);
            d.setLineWidth(lineWidth);
            d.setDefaultBackground(false);
            d.setShadowed(false);
            d.setSeeThrough(false);
            d.setViewRange(1.0f);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setTransformation(new Transformation(new Vector3f(0f, 0f, 0f), new AxisAngle4f(0f, 0f, 1f, 0f),
                    new Vector3f(scale, scale, scale), new AxisAngle4f(0f, 0f, 1f, 0f)));
            d.text(Component.text(" "));
        });
    }
}
