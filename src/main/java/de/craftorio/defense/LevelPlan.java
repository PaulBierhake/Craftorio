package de.craftorio.defense;

import de.craftorio.defense.sim.RoundDef;
import de.craftorio.defense.sim.RoundDefs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a level consists of: two rounds of the standard round list (level L is rounds 2L-1 and 2L) and the reward.
 * Pure logic so the difficulty curve can be unit tested.
 */
public record LevelPlan(int level, List<RoundDef> rounds, long reward, KeyReward keyReward) {
    /** Lives on the standard difficulty; see {@link Difficulty}. */
    public static final int LIVES = Difficulty.MEDIUM.lives();
    /** Ticks of rest between the two rounds of a level. */
    public static final int ROUND_DELAY = 200;
    public static final int ROUNDS_PER_LEVEL = 2;

    public enum KeyReward {
        NONE, BRONZE_SEAL, SILVER_SEAL, GOLD_SEAL, PLATINUM_SEAL, DIAMOND_SEAL, STAR_SEAL
    }

    public static LevelPlan of(int level, int players) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1: " + level);
        }
        RoundDef first = RoundDefs.get(firstRound(level));
        RoundDef second = RoundDefs.get(firstRound(level) + 1);
        long reward = 100 + 40L * level + (level % 10 == 0 ? 50L * level : 0);
        return new LevelPlan(level, List.of(first, second), reward, keyReward(level));
    }

    /** The number of the first of the level's two rounds. */
    public static int firstRound(int level) {
        return ROUNDS_PER_LEVEL * level - 1;
    }

    /** The arena seals: bronze at level 5, silver at 10, gold at 20, platinum at 30, diamond at 40 and star at 50 (first research of a new science pack). */
    public static KeyReward keyReward(int level) {
        return switch (level) {
            case 5 -> KeyReward.BRONZE_SEAL;
            case 10 -> KeyReward.SILVER_SEAL;
            case 20 -> KeyReward.GOLD_SEAL;
            case 30 -> KeyReward.PLATINUM_SEAL;
            case 40 -> KeyReward.DIAMOND_SEAL;
            case 50 -> KeyReward.STAR_SEAL;
            default -> KeyReward.NONE;
        };
    }

    /** RBE of all enemies of the level together. */
    public double totalRbe() {
        return rounds.stream().mapToDouble(RoundDefs::rbe).sum();
    }

    public int enemyCount() {
        return rounds.stream().mapToInt(RoundDef::enemyCount).sum();
    }

    /** 3 stars for no lost life, 2 for at least half of the lives left, otherwise 1. */
    public static int stars(int livesLeft, int maxLives) {
        if (livesLeft >= maxLives) {
            return 3;
        }
        return livesLeft * 2 >= maxLives ? 2 : 1;
    }

    public static int stars(int livesLeft) {
        return stars(livesLeft, LIVES);
    }

    /** Extra credits for stars: +25 % of the reward per star above the first. */
    public static long starBonus(long reward, int stars) {
        return reward * Math.max(0, stars - 1) / 4;
    }

    /** Code of an enemy group in packets: the enemy's index, the modifier bits (1 camo, 2 regrow, 4 fortified) above it. */
    public static int previewCode(String enemy, boolean camo, boolean regrow, boolean fortified) {
        int index = de.craftorio.defense.sim.EnemyDefs.get(enemy).index();
        return index | ((camo ? 1 : 0) | (regrow ? 2 : 0) | (fortified ? 4 : 0)) << 8;
    }

    /** How many enemies of each kind (and modifiers) a round holds, in order of first appearance, by {@link #previewCode}. */
    public static Map<Integer, Integer> summary(RoundDef round) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (RoundDef.Group group : round.groups()) {
            counts.merge(previewCode(group.enemy(), group.camo(), group.regrow(), group.fortified()), group.count(), Integer::sum);
        }
        return counts;
    }
}
