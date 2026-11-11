package com.lifea.chess.match;

/**
 * What happens when a player leaves an active match: their clock is paused for
 * {@code graceMillis}; if they do not return the match is forfeited (FORFEIT) or
 * adjourned for later resumption (ADJOURN).
 */
public record DisconnectPolicy(Action action, long graceMillis) {

    public enum Action { FORFEIT, ADJOURN }

    public static DisconnectPolicy forfeitAfterSeconds(int seconds) {
        return new DisconnectPolicy(Action.FORFEIT, seconds * 1_000L);
    }
}
