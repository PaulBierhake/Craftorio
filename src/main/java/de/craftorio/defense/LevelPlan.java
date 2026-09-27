package de.craftorio.defense;

import java.util.ArrayList;
import java.util.List;

/**
 * What a level consists of: waves of enemies, their health multiplier and the reward. Pure logic so the
 * difficulty curve can be unit tested and tuned in one place.
 */
public record LevelPlan(int level, List<List<EnemyType>> waves, double healthMultiplier, long reward, KeyReward keyReward) {
    public static final int LIVES = 10;
    public static final int SPAWN_INTERVAL = 15;
    public static final int WAVE_DELAY = 200;
    /** Extra enemy health per additional online team member. */
    public static final double HEALTH_PER_EXTRA_PLAYER = 0.35;
    /** Extra crawlers per wave for every additional online team member. */
    public static final int CRAWLERS_PER_EXTRA_PLAYER = 2;

    public enum KeyReward {
        NONE, DRILL_CORE, RESONANCE_CRYSTAL, DEEP_CORE, STAR_SHARD
    }

    public static LevelPlan of(int level, int players) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1: " + level);
        }
        int waveCount = Math.min(8, 3 + level / 5);
        List<List<EnemyType>> waves = new ArrayList<>();
        for (int wave = 1; wave <= waveCount; wave++) {
            List<EnemyType> enemies = new ArrayList<>();
            int crawlers = 3 + level + wave + CRAWLERS_PER_EXTRA_PLAYER * Math.max(0, players - 1);
            int breakers = level >= 5 ? (level + wave) / 4 : 0;
            int spitters = level >= 10 ? (level + wave) / 5 : 0;
            int golems = level >= 20 ? (level + wave) / 8 : 0;
            // Interleave so tougher enemies are escorted by crawlers.
            for (int i = 0; i < Math.max(Math.max(crawlers, golems), Math.max(breakers, spitters)); i++) {
                if (i < crawlers) {
                    enemies.add(EnemyType.CRAWLER);
                }
                if (i < breakers) {
                    enemies.add(EnemyType.BREAKER);
                }
                if (i < spitters) {
                    enemies.add(EnemyType.SPITTER);
                }
                if (i < golems) {
                    enemies.add(EnemyType.CRYSTAL_GOLEM);
                }
            }
            if (level % 10 == 0 && wave == waveCount) {
                enemies.add(EnemyType.BROOD_MOTHER);
            }
            waves.add(List.copyOf(enemies));
        }
        double health = (1 + 0.12 * (level - 1)) * (1 + HEALTH_PER_EXTRA_PLAYER * Math.max(0, players - 1));
        long reward = 100 + 40L * level + (level % 10 == 0 ? 50L * level : 0);
        return new LevelPlan(level, List.copyOf(waves), health, reward, keyReward(level));
    }

    /** Every tenth level unlocks the next factory tier; later milestones keep giving star shards. */
    public static KeyReward keyReward(int level) {
        if (level % 10 != 0) {
            return KeyReward.NONE;
        }
        return switch (level) {
            case 10 -> KeyReward.DRILL_CORE;
            case 20 -> KeyReward.RESONANCE_CRYSTAL;
            case 30 -> KeyReward.DEEP_CORE;
            default -> KeyReward.STAR_SHARD;
        };
    }

    public int enemyCount() {
        return waves.stream().mapToInt(List::size).sum();
    }

    /** 3 stars for no lost life, 2 for at least half of the lives left, otherwise 1. */
    public static int stars(int livesLeft) {
        if (livesLeft >= LIVES) {
            return 3;
        }
        return livesLeft * 2 >= LIVES ? 2 : 1;
    }

    /** Extra credits for stars: +25 % of the reward per star above the first. */
    public static long starBonus(long reward, int stars) {
        return reward * Math.max(0, stars - 1) / 4;
    }

    /** Credits for calling the next wave early: per second skipped, growing with the level. */
    public static long earlyCallBonus(int level, int ticksSkipped) {
        return (long) (ticksSkipped / 20) * (2 + level / 5);
    }

    /** How many enemies of each type a wave holds, in order of first appearance. */
    public static java.util.Map<EnemyType, Integer> summary(List<EnemyType> wave) {
        java.util.Map<EnemyType, Integer> counts = new java.util.LinkedHashMap<>();
        wave.forEach(type -> counts.merge(type, 1, Integer::sum));
        return counts;
    }
}
