package de.craftorio.defense.sim;

/** The money rules of Bloons TD 6 by round (bloons wiki: Rounds and Freeplay). */
public final class RoundRules {
    /** Coins a team starts the campaign with. */
    public static final long START_COINS = 650;
    /** Share of the price a sold tower pays back. */
    public static final double SELL_SHARE = 0.7;

    private RoundRules() {
    }

    /** What popping one layer pays relative to the normal 1 coin: less and less from round 51 on. */
    public static double incomeFactor(int round) {
        if (round <= 50) {
            return 1;
        }
        if (round <= 60) {
            return 0.5;
        }
        if (round <= 85) {
            return 0.2;
        }
        if (round <= 100) {
            return 0.1;
        }
        if (round <= 120) {
            return 0.05;
        }
        return round <= 140 ? 0.04 : 0.02;
    }

    /** Coins at the end of a round. */
    public static long roundBonus(int round) {
        return 100 + round;
    }
}
