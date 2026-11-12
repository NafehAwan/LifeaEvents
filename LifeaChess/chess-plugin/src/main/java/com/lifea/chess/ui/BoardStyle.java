package com.lifea.chess.ui;

import com.lifea.chess.config.Theme;
import com.lifea.chess.engine.Color;
import com.lifea.chess.engine.Game;
import com.lifea.chess.engine.Move;
import com.lifea.chess.engine.MoveRecord;
import com.lifea.chess.engine.Piece;
import com.lifea.chess.engine.PieceType;
import com.lifea.chess.engine.Position;
import com.lifea.chess.engine.Square;
import com.lifea.chess.game.ActiveMatch;
import com.lifea.chess.match.ChessClock;
import java.util.List;
import java.util.UUID;

/** Shared board-drawing decisions so the screen board and the in-world board look alike. */
public final class BoardStyle {
    private BoardStyle() {
    }

    /** What a square should look like for one viewer. */
    public record SquareLook(String background, String glyph, String glyphColor, boolean legalDot, boolean captureRing) {
    }

    public static boolean isLastMoveSquare(Game game, int sq) {
        MoveRecord last = game.lastMove();
        return last != null && (last.move().from() == sq || last.move().to() == sq);
    }

    /** Computes a square's appearance; {@code viewer} may be null for a neutral view. */
    public static SquareLook look(Theme theme, ActiveMatch am, UUID viewer, int sq) {
        Game game = am.match().game();
        Position pos = game.position();
        Piece piece = pos.pieceAt(sq);
        String bg = Square.isLight(sq) ? theme.lightSquare() : theme.darkSquare();
        boolean legal = false;
        boolean capture = false;
        if (isLastMoveSquare(game, sq)) {
            bg = theme.lastMove();
        }
        if (viewer != null) {
            Integer sel = am.selected(viewer);
            if (sel != null) {
                if (sel == sq) {
                    bg = theme.selected();
                } else {
                    for (Move m : game.legalMovesFrom(sel)) {
                        if (m.to() == sq) {
                            if (pos.isCapture(m)) {
                                capture = true;
                            } else {
                                legal = true;
                            }
                            break;
                        }
                    }
                }
            }
        }
        if (piece != null && piece.type() == PieceType.KING && pos.isInCheck(piece.color())) {
            bg = theme.check();
        }
        if (capture) {
            bg = theme.capture();
        }
        String glyph = piece == null ? (legal ? "•" : " ") : String.valueOf(piece.glyph());
        String color = piece == null ? theme.legalMove() : piece.color() == Color.WHITE ? theme.whitePiece() : theme.blackPiece();
        return new SquareLook(bg, glyph, color, legal, capture);
    }

    /** Square index for a board drawn from one side: rank 7 at the top for White, rank 0 at the top for Black. */
    public static int squareAt(int row, int col, boolean flipped) {
        int rank = flipped ? row : 7 - row;
        int file = flipped ? 7 - col : col;
        return Square.of(file, rank);
    }

    public static String materialLine(Position pos) {
        return "♟" + pos.material(Color.WHITE) + " vs " + pos.material(Color.BLACK);
    }

    public static String clock(ActiveMatch am, Color side) {
        return ChessClock.format(am.match().clock().remaining(side));
    }

    public static String captured(Game game, Color victim) {
        StringBuilder sb = new StringBuilder();
        List<Piece> list = game.capturedFrom(victim);
        for (Piece p : list) {
            sb.append(p.glyph());
        }
        return sb.toString();
    }

    public static String turnLabel(ActiveMatch am, UUID viewer) {
        Game game = am.match().game();
        if (am.match().isFinished()) {
            return "GAME OVER";
        }
        Color stm = game.sideToMove();
        Color mine = am.match().colorOf(viewer);
        String base = mine == null ? (stm == Color.WHITE ? "WHITE TO MOVE" : "BLACK TO MOVE")
                : mine == stm ? "YOUR TURN" : "OPPONENT'S TURN";
        return game.inCheck() ? base + " · CHECK!" : base;
    }

    /** Last 6 half-moves as "12. Nf3 Nc6 13. O-O". */
    public static String recentMoves(Game game, int plies) {
        List<MoveRecord> h = game.history();
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, h.size() - plies);
        for (int i = start; i < h.size(); i++) {
            MoveRecord r = h.get(i);
            if (r.mover() == Color.WHITE || i == start) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(r.moveNumber()).append(r.mover() == Color.WHITE ? ". " : "... ");
            } else {
                sb.append(' ');
            }
            sb.append(r.san());
        }
        return sb.toString();
    }
}
