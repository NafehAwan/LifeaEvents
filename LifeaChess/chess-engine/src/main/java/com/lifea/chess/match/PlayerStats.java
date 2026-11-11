package com.lifea.chess.match;

/** Immutable rating/statistics snapshot for one player. */
public record PlayerStats(int rating, int peakRating, int wins, int losses, int draws, int streak, int bestStreak) {

    public static PlayerStats fresh(int startingRating) {
        return new PlayerStats(startingRating, startingRating, 0, 0, 0, 0, 0);
    }

    public int games() {
        return wins + losses + draws;
    }

    public double winRate() {
        int g = games();
        return g == 0 ? 0.0 : (double) wins / g;
    }

    public PlayerStats afterWin(int delta) {
        int newRating = rating + delta;
        int newStreak = Math.max(0, streak) + 1;
        return new PlayerStats(newRating, Math.max(peakRating, newRating), wins + 1, losses, draws,
                newStreak, Math.max(bestStreak, newStreak));
    }

    public PlayerStats afterLoss(int delta) {
        int newRating = rating + delta;
        return new PlayerStats(newRating, peakRating, wins, losses + 1, draws, 0, bestStreak);
    }

    public PlayerStats afterDraw(int delta) {
        int newRating = rating + delta;
        return new PlayerStats(newRating, Math.max(peakRating, newRating), wins, losses, draws + 1, 0, bestStreak);
    }
}
