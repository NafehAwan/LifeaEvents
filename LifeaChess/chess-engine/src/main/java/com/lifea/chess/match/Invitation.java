package com.lifea.chess.match;

import java.util.UUID;

/** A challenge from one player to another. */
public record Invitation(UUID id, UUID from, String fromName, UUID to, String toName,
                         TimeControl timeControl, boolean ranked, long createdAt, long expiresAt) {

    public boolean isExpired(long nowMillis) {
        return nowMillis >= expiresAt;
    }

    public long remainingMillis(long nowMillis) {
        return Math.max(0L, expiresAt - nowMillis);
    }
}
