package com.lifea.chess.storage;

import com.lifea.chess.match.PlayerStats;
import java.util.UUID;

/** A player row: identity plus rating statistics. */
public record RatedPlayer(UUID uuid, String name, PlayerStats stats) {
}
