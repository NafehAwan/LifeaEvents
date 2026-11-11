package com.lifea.chess.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A complete game: a live {@link Position}, the move history, and every
 * termination rule — checkmate, stalemate, dead position, the automatic
 * seventy-five-move and fivefold rules, the claimable fifty-move and threefold
 * rules, resignation, agreement and flag-fall (with the insufficient-material
 * exception). The game never trusts callers: illegal moves are rejected.
 */
public final class Game {
    private final Position position;
    private final String startFen;
    private final List<MoveRecord> history = new ArrayList<>();
    private final Map<Long, Integer> repetitions = new HashMap<>();
    private final List<Long> keyHistory = new ArrayList<>();
    private GameResult result;

    private Game(Position position) {
        this.position = position;
        this.startFen = Fen.format(position);
        recordRepetition();
        checkAutomaticTermination();
    }

    public static Game fromStart() {
        return new Game(Position.initial());
    }

    public static Game fromFen(String fen) {
        return new Game(Fen.parse(fen));
    }

    // ------------------------------------------------------------------ state

    public Position position() {
        return position;
    }

    public String startFen() {
        return startFen;
    }

    public String fen() {
        return Fen.format(position);
    }

    public Color sideToMove() {
        return position.sideToMove();
    }

    public List<MoveRecord> history() {
        return Collections.unmodifiableList(history);
    }

    public MoveRecord lastMove() {
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    public GameResult result() {
        return result;
    }

    public boolean isOver() {
        return result != null;
    }

    public boolean inCheck() {
        return position.inCheck();
    }

    public List<Move> legalMoves() {
        return isOver() ? List.of() : position.legalMoves();
    }

    /** Legal moves originating from one square (for UI highlighting). */
    public List<Move> legalMovesFrom(int square) {
        List<Move> out = new ArrayList<>();
        for (Move m : legalMoves()) {
            if (m.from() == square) {
                out.add(m);
            }
        }
        return out;
    }

    public int repetitionCount() {
        return repetitions.getOrDefault(position.repetitionKey(), 0);
    }

    // ------------------------------------------------------------------ playing

    /** Plays a legal move. Throws if the game is over or the move is illegal. */
    public MoveRecord play(Move move) {
        if (isOver()) {
            throw new IllegalStateException("Game is over: " + result);
        }
        List<Move> legal = position.legalMoves();
        if (!legal.contains(move)) {
            throw new IllegalArgumentException("Illegal move " + move.uci() + " in " + fen());
        }
        Color mover = position.sideToMove();
        String san = San.format(position, move, legal);
        Piece captured = position.capturedBy(move);
        position.make(move);
        boolean check = position.inCheck();
        boolean mate = check && !position.hasLegalMoves();
        MoveRecord rec = new MoveRecord(history.size() + 1, mover, move, san, captured, check, mate, fen());
        history.add(rec);
        recordRepetition();
        checkAutomaticTermination();
        return rec;
    }

    public MoveRecord playSan(String san) {
        return play(San.parse(position, san));
    }

    public MoveRecord playUci(String uci) {
        return play(Uci.parse(uci));
    }

    /** Takes back the last move (also clears any result produced by it). */
    public MoveRecord undo() {
        if (history.isEmpty()) {
            throw new IllegalStateException("No moves to undo");
        }
        long key = keyHistory.remove(keyHistory.size() - 1);
        repetitions.merge(key, -1, Integer::sum);
        if (repetitions.get(key) <= 0) {
            repetitions.remove(key);
        }
        position.unmake();
        result = null;
        return history.remove(history.size() - 1);
    }

    private void recordRepetition() {
        long key = position.repetitionKey();
        keyHistory.add(key);
        repetitions.merge(key, 1, Integer::sum);
    }

    /** Applies the rules that end a game without any claim. */
    private void checkAutomaticTermination() {
        if (result != null) {
            return;
        }
        boolean hasMoves = position.hasLegalMoves();
        if (!hasMoves) {
            if (position.inCheck()) {
                result = new GameResult(GameResult.Outcome.winFor(position.sideToMove().opposite()),
                        GameResult.Reason.CHECKMATE);
            } else {
                result = new GameResult(GameResult.Outcome.DRAW, GameResult.Reason.STALEMATE);
            }
            return;
        }
        if (position.isDeadPosition()) {
            result = new GameResult(GameResult.Outcome.DRAW, GameResult.Reason.DEAD_POSITION);
            return;
        }
        if (position.halfmoveClock() >= 150) {
            result = new GameResult(GameResult.Outcome.DRAW, GameResult.Reason.SEVENTY_FIVE_MOVE_RULE);
            return;
        }
        if (repetitionCount() >= 5) {
            result = new GameResult(GameResult.Outcome.DRAW, GameResult.Reason.FIVEFOLD_REPETITION);
        }
    }

    // ------------------------------------------------------------------ claims & other endings

    public boolean canClaimThreefold() {
        return !isOver() && repetitionCount() >= 3;
    }

    public boolean canClaimFiftyMoves() {
        return !isOver() && position.halfmoveClock() >= 100;
    }

    public boolean canClaimDraw() {
        return canClaimThreefold() || canClaimFiftyMoves();
    }

    /** Claims a draw by repetition or the fifty-move rule; returns false if no claim is available. */
    public boolean claimDraw() {
        if (canClaimThreefold()) {
            result = new GameResult(GameResult.Outcome.DRAW, GameResult.Reason.THREEFOLD_REPETITION);
            return true;
        }
        if (canClaimFiftyMoves()) {
            result = new GameResult(GameResult.Outcome.DRAW, GameResult.Reason.FIFTY_MOVE_RULE);
            return true;
        }
        return false;
    }

    public void resign(Color resigning) {
        ensureOngoing();
        result = new GameResult(GameResult.Outcome.winFor(resigning.opposite()), GameResult.Reason.RESIGNATION);
    }

    public void agreeDraw() {
        ensureOngoing();
        result = new GameResult(GameResult.Outcome.DRAW, GameResult.Reason.AGREEMENT);
    }

    /**
     * The flag of {@code flagged} fell. The opponent wins unless they have no
     * mating material, in which case the game is drawn (FIDE 6.9).
     */
    public void timeout(Color flagged) {
        ensureOngoing();
        Color opponent = flagged.opposite();
        if (position.hasMatingMaterial(opponent)) {
            result = new GameResult(GameResult.Outcome.winFor(opponent), GameResult.Reason.TIMEOUT);
        } else {
            result = new GameResult(GameResult.Outcome.DRAW, GameResult.Reason.TIMEOUT_INSUFFICIENT_MATERIAL);
        }
    }

    public void abandon(Color leaving) {
        ensureOngoing();
        result = new GameResult(GameResult.Outcome.winFor(leaving.opposite()), GameResult.Reason.ABANDONED);
    }

    private void ensureOngoing() {
        if (isOver()) {
            throw new IllegalStateException("Game is over: " + result);
        }
    }

    // ------------------------------------------------------------------ export

    /** Move list in PGN movetext form, e.g. "1. e4 e5 2. Nf3 Nc6". */
    public String movetext() {
        StringBuilder sb = new StringBuilder();
        for (MoveRecord r : history) {
            if (r.mover() == Color.WHITE) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(r.moveNumber()).append(". ");
            } else {
                if (sb.length() == 0) {
                    sb.append(r.moveNumber()).append("... ");
                } else {
                    sb.append(' ');
                }
            }
            sb.append(r.san());
        }
        if (result != null) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(result.token());
        }
        return sb.toString();
    }

    /** Pieces captured from {@code victim} so far, in capture order. */
    public List<Piece> capturedFrom(Color victim) {
        List<Piece> out = new ArrayList<>();
        for (MoveRecord r : history) {
            if (r.captured() != null && r.captured().color() == victim) {
                out.add(r.captured());
            }
        }
        return out;
    }
}
