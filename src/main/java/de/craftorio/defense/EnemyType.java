package de.craftorio.defense;

/** Base stats of tower defense enemies; health is scaled per level by {@link LevelPlan}. */
public enum EnemyType {
    /** Fast and weak, comes in swarms, nibbles towers while walking past. */
    CRAWLER(12, 0.10, 2, 1.8, 20, false, 1),
    /** Slow and tough, stops to smash towers next to the path. */
    BREAKER(60, 0.05, 8, 2.2, 30, true, 2),
    /** Spits at towers from a distance while walking. */
    SPITTER(25, 0.07, 4, 7.0, 40, false, 1),
    /** Boss of every tenth level. */
    BROOD_MOTHER(400, 0.04, 15, 2.6, 30, true, 10);

    private final double health;
    private final double speed;
    private final double damage;
    private final double reach;
    private final int attackInterval;
    private final boolean stopsToAttack;
    private final int leakCost;

    EnemyType(double health, double speed, double damage, double reach, int attackInterval, boolean stopsToAttack, int leakCost) {
        this.health = health;
        this.speed = speed;
        this.damage = damage;
        this.reach = reach;
        this.attackInterval = attackInterval;
        this.stopsToAttack = stopsToAttack;
        this.leakCost = leakCost;
    }

    public double health() {
        return health;
    }

    /** Blocks per tick along the path. */
    public double speed() {
        return speed;
    }

    public double damage() {
        return damage;
    }

    public double reach() {
        return reach;
    }

    public int attackInterval() {
        return attackInterval;
    }

    public boolean stopsToAttack() {
        return stopsToAttack;
    }

    /** Lives lost when this enemy reaches the core. */
    public int leakCost() {
        return leakCost;
    }
}
