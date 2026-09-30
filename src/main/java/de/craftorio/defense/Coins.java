package de.craftorio.defense;

import de.craftorio.defense.sim.RoundDef;
import de.craftorio.defense.sim.RoundDefs;
import de.craftorio.defense.sim.RoundRules;

/** Coin rules that are pure arithmetic, so they can be unit tested. */
public final class Coins {
    /** Arenas that existed before the coins start with this share of what Bloons TD 6 pays up to their level. */
    public static final double MIGRATION_SHARE = 0.5;

    private Coins() {
    }

    /** Everything popping all enemies and the round bonuses up to the end of {@code lastRound} pay. */
    public static double earnedUpTo(int lastRound) {
        double total = 0;
        for (int round = 1; round <= lastRound; round++) {
            RoundDef def = RoundDefs.get(round);
            total += def.cash() + RoundRules.roundBonus(round);
        }
        return total;
    }

    /** The coins an existing arena at this level starts with: the start money plus half of what the earlier levels would have paid. */
    public static double migratedStart(int level) {
        return RoundRules.START_COINS + MIGRATION_SHARE * earnedUpTo(Math.min(RoundDefs.count(), LevelPlan.ROUNDS_PER_LEVEL * (level - 1)));
    }
}
