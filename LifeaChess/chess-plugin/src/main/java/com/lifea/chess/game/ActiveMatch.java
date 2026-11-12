package com.lifea.chess.game;

import com.lifea.chess.engine.Move;
import com.lifea.chess.match.Match;
import com.lifea.chess.table.ChessTable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** A running match bound to a table, plus per-viewer UI state (selection, pending promotion). */
public final class ActiveMatch {
    private final Match match;
    private final ChessTable table;
    private final Map<UUID, Integer> selected = new HashMap<>();
    private final Map<UUID, Move> pendingPromotion = new HashMap<>();
    private long lastMoveAt = System.currentTimeMillis();
    private boolean resultProcessed;
    private int whiteDelta;
    private int blackDelta;
    private boolean rated;

    public ActiveMatch(Match match, ChessTable table) {
        this.match = match;
        this.table = table;
    }

    public UUID id() {
        return match.id();
    }

    public Match match() {
        return match;
    }

    public ChessTable table() {
        return table;
    }

    public Integer selected(UUID player) {
        return selected.get(player);
    }

    public void select(UUID player, Integer square) {
        if (square == null) {
            selected.remove(player);
        } else {
            selected.put(player, square);
        }
    }

    public Move pendingPromotion(UUID player) {
        return pendingPromotion.get(player);
    }

    public void setPendingPromotion(UUID player, Move base) {
        if (base == null) {
            pendingPromotion.remove(player);
        } else {
            pendingPromotion.put(player, base);
        }
    }

    public void clearUiState(UUID player) {
        selected.remove(player);
        pendingPromotion.remove(player);
    }

    /** Players and spectators, in a stable order. */
    public Set<UUID> viewers() {
        Set<UUID> v = new LinkedHashSet<>();
        v.add(match.whiteId());
        v.add(match.blackId());
        v.addAll(match.spectators());
        return v;
    }

    public long lastMoveAt() {
        return lastMoveAt;
    }

    public void touch() {
        lastMoveAt = System.currentTimeMillis();
    }

    public boolean resultProcessed() {
        return resultProcessed;
    }

    public void markProcessed(boolean rated, int whiteDelta, int blackDelta) {
        this.resultProcessed = true;
        this.rated = rated;
        this.whiteDelta = whiteDelta;
        this.blackDelta = blackDelta;
    }

    public boolean rated() {
        return rated;
    }

    public int whiteDelta() {
        return whiteDelta;
    }

    public int blackDelta() {
        return blackDelta;
    }
}
