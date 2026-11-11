package com.lifea.chess.match;

import com.lifea.chess.engine.GameResult;

/** Standard Elo with a configurable K-factor (and a higher one for provisional players). */
public final class EloCalculator {
    private final int kFactor;
    private final int provisionalK;
    private final int provisionalGames;

    public EloCalculator(int kFactor, int provisionalK, int provisionalGames) {
        this.kFactor = kFactor;
        this.provisionalK = provisionalK;
        this.provisionalGames = provisionalGames;
    }

    public EloCalculator() {
        this(32, 40, 30);
    }

    /** Probability that the first player scores against the second. */
    public static double expectedScore(int rating, int opponentRating) {
        return 1.0 / (1.0 + Math.pow(10.0, (opponentRating - rating) / 400.0));
    }

    public record Change(int whiteDelta, int blackDelta) {
    }

    /** Rating changes for a finished game between the given ratings. */
    public Change compute(int whiteRating, int whiteGames, int blackRating, int blackGames, GameResult.Outcome outcome) {
        double whiteScore = switch (outcome) {
            case WHITE_WINS -> 1.0;
            case BLACK_WINS -> 0.0;
            case DRAW -> 0.5;
        };
        double expectedWhite = expectedScore(whiteRating, blackRating);
        double expectedBlack = 1.0 - expectedWhite;
        int kWhite = whiteGames < provisionalGames ? provisionalK : kFactor;
        int kBlack = blackGames < provisionalGames ? provisionalK : kFactor;
        int whiteDelta = (int) Math.round(kWhite * (whiteScore - expectedWhite));
        int blackDelta = (int) Math.round(kBlack * ((1.0 - whiteScore) - expectedBlack));
        return new Change(whiteDelta, blackDelta);
    }
}
