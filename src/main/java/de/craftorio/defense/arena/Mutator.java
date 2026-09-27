package de.craftorio.defense.arena;

import java.util.Random;

/** A special rule some levels get, for a higher reward. */
public enum Mutator {
    NONE(1.0, 1.0, 1.0, 1.0),
    /** Fog: towers see 25 % less far. */
    FOG(0.75, 1.0, 1.0, 1.25),
    /** Forced march: enemies are 30 % faster. */
    HASTE(1.0, 1.3, 1.0, 1.25),
    /** Hardened: enemies have 35 % more health. */
    HARDENED(1.0, 1.0, 1.35, 1.3);

    /** Share of eligible levels (from level 6, no boss levels) that get a mutator. */
    public static final double CHANCE = 0.35;

    private final double range;
    private final double speed;
    private final double health;
    private final double reward;

    Mutator(double range, double speed, double health, double reward) {
        this.range = range;
        this.speed = speed;
        this.health = health;
        this.reward = reward;
    }

    public double rangeFactor() {
        return range;
    }

    public double speedFactor() {
        return speed;
    }

    public double healthFactor() {
        return health;
    }

    public double rewardFactor() {
        return reward;
    }

    public static Mutator forLevel(long seed, int level) {
        if (level < 6 || level % 10 == 0) {
            return NONE;
        }
        Random random = new Random(seed * 17 + level * 7919L);
        if (random.nextDouble() >= CHANCE) {
            return NONE;
        }
        Mutator[] options = {FOG, HASTE, HARDENED};
        return options[random.nextInt(options.length)];
    }
}
