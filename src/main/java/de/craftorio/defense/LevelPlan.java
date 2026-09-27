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
            int crawlers = 3 + level + wave;
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
}
