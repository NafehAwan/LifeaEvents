package com.lifea.chess.storage;

import java.util.UUID;

/** A finished match as persisted. */
public record StoredMatch(UUID id, UUID whiteId, String whiteName, UUID blackId, String blackName,
                          String timeControl, boolean ranked, String outcome, String reason,
                          String movetext, String finalFen, long startedAt, long endedAt,
                          int whiteDelta, int blackDelta) {
}
