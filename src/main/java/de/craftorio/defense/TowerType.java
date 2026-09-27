package de.craftorio.defense;

/** Tower base stats; upgrades scale damage and health (see {@link TowerStats}). */
public enum TowerType {
    CROSSBOW(100, 4, 10, 20, 0, 1),
    GUN(200, 7, 12, 12, 0, 1),
    /** Uses grid power instead of ammunition and jumps to up to three enemies. */
    TESLA(150, 10, 8, 30, 400, 3);

    private final int health;
    private final double damage;
    private final double range;
    private final int cooldown;
    private final int energyPerShot;
    private final int targets;

    TowerType(int health, double damage, double range, int cooldown, int energyPerShot, int targets) {
        this.health = health;
        this.damage = damage;
        this.range = range;
        this.cooldown = cooldown;
        this.energyPerShot = energyPerShot;
        this.targets = targets;
    }

    public int health() {
        return health;
    }

    public double damage() {
        return damage;
    }

    public double range() {
        return range;
    }

    public int cooldown() {
        return cooldown;
    }

    public int energyPerShot() {
        return energyPerShot;
    }

    public boolean usesEnergy() {
        return energyPerShot > 0;
    }

    public int targets() {
        return targets;
    }

    /** Credits to rebuild a destroyed tower from its ruin. */
    public long rebuildCost() {
        return switch (this) {
            case CROSSBOW -> 60;
            case GUN -> 250;
            case TESLA -> 400;
        };
    }
}
