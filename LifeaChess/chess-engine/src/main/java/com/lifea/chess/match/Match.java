package com.lifea.chess.match;

import com.lifea.chess.engine.Color;
import com.lifea.chess.engine.Game;
import com.lifea.chess.engine.GameResult;
import com.lifea.chess.engine.Move;
import com.lifea.chess.engine.MoveRecord;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * One match between two players: authoritative game state, clocks, draw offers,
 * resignation, flag-fall, disconnect handling and spectators. Everything is
 * validated here; callers (commands, GUIs) never mutate the game directly.
 */
public final class Match {

    public enum State { PENDING, ACTIVE, PAUSED, FINISHED }

    private final UUID id = UUID.randomUUID();
    private final UUID whiteId;
    private final String whiteName;
    private final UUID blackId;
    private final String blackName;
    private final TimeControl timeControl;
    private final boolean ranked;
    private final Game game;
    private final ChessClock clock;
    private final LongSupplier now;
    private final DisconnectPolicy disconnectPolicy;
    private final Set<UUID> spectators = new LinkedHashSet<>();

    private State state = State.PENDING;
    private Color drawOfferedBy;
    private Color disconnected;
    private long disconnectedAt;
    private long createdAt;
    private long startedAt;
    private long endedAt;

    public Match(UUID whiteId, String whiteName, UUID blackId, String blackName,
                 TimeControl timeControl, boolean ranked, DisconnectPolicy policy) {
        this(whiteId, whiteName, blackId, blackName, timeControl, ranked, policy, System::currentTimeMillis);
    }

    public Match(UUID whiteId, String whiteName, UUID blackId, String blackName,
                 TimeControl timeControl, boolean ranked, DisconnectPolicy policy, LongSupplier nowMillis) {
        if (whiteId.equals(blackId)) {
            throw new IllegalArgumentException("A player cannot play themselves");
        }
        this.whiteId = whiteId;
        this.whiteName = whiteName;
        this.blackId = blackId;
        this.blackName = blackName;
        this.timeControl = timeControl;
        this.ranked = ranked;
        this.disconnectPolicy = policy;
        this.now = nowMillis;
        this.game = Game.fromStart();
        this.clock = new ChessClock(timeControl, nowMillis);
        this.createdAt = nowMillis.getAsLong();
    }

    // ------------------------------------------------------------------ identity

    public UUID id() {
        return id;
    }

    public UUID whiteId() {
        return whiteId;
    }

    public UUID blackId() {
        return blackId;
    }

    public String whiteName() {
        return whiteName;
    }

    public String blackName() {
        return blackName;
    }

    public String nameOf(Color c) {
        return c == Color.WHITE ? whiteName : blackName;
    }

    public UUID idOf(Color c) {
        return c == Color.WHITE ? whiteId : blackId;
    }

    public Color colorOf(UUID player) {
        if (whiteId.equals(player)) {
            return Color.WHITE;
        }
        if (blackId.equals(player)) {
            return Color.BLACK;
        }
        return null;
    }

    public boolean isPlayer(UUID player) {
        return colorOf(player) != null;
    }

    public UUID opponentOf(UUID player) {
        Color c = colorOf(player);
        return c == null ? null : idOf(c.opposite());
    }

    public TimeControl timeControl() {
        return timeControl;
    }

    public boolean ranked() {
        return ranked;
    }

    public Game game() {
        return game;
    }

    public ChessClock clock() {
        return clock;
    }

    public State state() {
        return state;
    }

    public boolean isActive() {
        return state == State.ACTIVE;
    }

    public boolean isFinished() {
        return state == State.FINISHED;
    }

    public GameResult result() {
        return game.result();
    }

    public Color drawOfferedBy() {
        return drawOfferedBy;
    }

    public Color disconnectedSide() {
        return disconnected;
    }

    public long createdAt() {
        return createdAt;
    }

    public long startedAt() {
        return startedAt;
    }

    public long endedAt() {
        return endedAt;
    }

    /** Duration in millis (live while active). */
    public long duration() {
        if (startedAt == 0L) {
            return 0L;
        }
        long end = endedAt == 0L ? now.getAsLong() : endedAt;
        return end - startedAt;
    }

    public Set<UUID> spectators() {
        return Collections.unmodifiableSet(spectators);
    }

    public boolean addSpectator(UUID player) {
        if (isPlayer(player)) {
            return false;
        }
        return spectators.add(player);
    }

    public boolean removeSpectator(UUID player) {
        return spectators.remove(player);
    }

    // ------------------------------------------------------------------ lifecycle

    /** Both players are seated: start the game and White's clock. */
    public void start() {
        if (state != State.PENDING) {
            throw new IllegalStateException("Match already started");
        }
        state = State.ACTIVE;
        startedAt = now.getAsLong();
        clock.start(Color.WHITE);
    }

    public enum MoveFailure { NOT_ACTIVE, NOT_A_PLAYER, NOT_YOUR_TURN, ILLEGAL_MOVE, FLAGGED, NONE }

    public record MoveOutcome(MoveRecord record, MoveFailure failure) {
        public boolean ok() {
            return failure == MoveFailure.NONE;
        }
    }

    /** Validates and plays a move for {@code player}. Never throws for user error. */
    public MoveOutcome playMove(UUID player, Move move) {
        if (state != State.ACTIVE) {
            return new MoveOutcome(null, MoveFailure.NOT_ACTIVE);
        }
        Color color = colorOf(player);
        if (color == null) {
            return new MoveOutcome(null, MoveFailure.NOT_A_PLAYER);
        }
        if (game.sideToMove() != color) {
            return new MoveOutcome(null, MoveFailure.NOT_YOUR_TURN);
        }
        if (checkFlag() != null) {
            return new MoveOutcome(null, MoveFailure.FLAGGED);
        }
        if (!game.legalMoves().contains(move)) {
            return new MoveOutcome(null, MoveFailure.ILLEGAL_MOVE);
        }
        MoveRecord rec = game.play(move);
        clock.press(color);
        if (drawOfferedBy != null && drawOfferedBy != color) {
            drawOfferedBy = null; // making a move declines a pending offer
        }
        if (game.isOver()) {
            finish();
        }
        return new MoveOutcome(rec, MoveFailure.NONE);
    }

    /**
     * Checks both clocks; if a flag has fallen the game ends by timeout (or draw
     * for insufficient material). Returns the flagged colour, or null.
     */
    public Color checkFlag() {
        if (state != State.ACTIVE) {
            return null;
        }
        Color flagged = clock.flagged();
        if (flagged != null) {
            game.timeout(flagged);
            finish();
        }
        return flagged;
    }

    public boolean resign(UUID player) {
        Color c = colorOf(player);
        if (c == null || state == State.FINISHED || state == State.PENDING) {
            return false;
        }
        game.resign(c);
        finish();
        return true;
    }

    public enum DrawAction { OFFERED, ACCEPTED, DECLINED, CLAIMED, REJECTED }

    /** Offer a draw, or accept the opponent's pending offer. Claims (3-fold / 50-move) are automatic. */
    public DrawAction offerOrAcceptDraw(UUID player) {
        Color c = colorOf(player);
        if (c == null || state != State.ACTIVE) {
            return DrawAction.REJECTED;
        }
        if (game.canClaimDraw()) {
            game.claimDraw();
            finish();
            return DrawAction.CLAIMED;
        }
        if (drawOfferedBy != null && drawOfferedBy != c) {
            game.agreeDraw();
            finish();
            return DrawAction.ACCEPTED;
        }
        if (drawOfferedBy == c) {
            return DrawAction.REJECTED;
        }
        drawOfferedBy = c;
        return DrawAction.OFFERED;
    }

    public DrawAction declineDraw(UUID player) {
        Color c = colorOf(player);
        if (c == null || drawOfferedBy == null || drawOfferedBy == c) {
            return DrawAction.REJECTED;
        }
        drawOfferedBy = null;
        return DrawAction.DECLINED;
    }

    // ------------------------------------------------------------------ disconnects

    /** A player left: pause their clock and start the grace period. */
    public void markDisconnected(UUID player) {
        Color c = colorOf(player);
        if (c == null || state != State.ACTIVE) {
            return;
        }
        disconnected = c;
        disconnectedAt = now.getAsLong();
        clock.pause();
        state = State.PAUSED;
    }

    /** The disconnected player is back: resume the clock of the side to move. */
    public void markReconnected(UUID player) {
        Color c = colorOf(player);
        if (c == null || state != State.PAUSED || disconnected != c) {
            return;
        }
        disconnected = null;
        state = State.ACTIVE;
        clock.start(game.sideToMove());
    }

    /**
     * Call periodically. If the grace period has elapsed, applies the policy:
     * FORFEIT ends the game for the absent side; ADJOURN leaves it paused.
     * Returns true when the match was forfeited by this call.
     */
    public boolean checkDisconnectTimeout() {
        if (state != State.PAUSED || disconnected == null) {
            return false;
        }
        if (now.getAsLong() - disconnectedAt < disconnectPolicy.graceMillis()) {
            return false;
        }
        if (disconnectPolicy.action() == DisconnectPolicy.Action.FORFEIT) {
            game.abandon(disconnected);
            finish();
            return true;
        }
        return false;
    }

    /** Force-end (admin stop, server shutdown). */
    public void abort() {
        if (state == State.FINISHED) {
            return;
        }
        if (!game.isOver()) {
            game.agreeDraw();
        }
        finish();
    }

    private void finish() {
        clock.pause();
        state = State.FINISHED;
        endedAt = now.getAsLong();
        drawOfferedBy = null;
    }
}
