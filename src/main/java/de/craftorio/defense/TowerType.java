package de.craftorio.defense;

/** Tower base stats; upgrades scale damage and health (see {@link TowerStats}). */
public enum TowerType {
    CROSSBOW(100, 4, 10, 20, 0, 1, 0),
    /** Fires magazines: ten shots each, armour-piercing ones hit 60 % harder. */
    GUN(200, 7, 12, 12, 0, 1, 0),
    /** Uses grid power instead of ammunition and jumps to up to three enemies. */
    TESLA(150, 10, 8, 30, 400, 3, 0),
    /** Long-range energy beam that burns through crystal armour. */
    LASER(250, 30, 14, 20, 800, 1, 0),
    /** Burns crude oil from the arena reserve: short range, hits up to three enemies, fire ignores physical armour. */
    FLAME(250, 12, 8, 10, 0, 3, 6);

    /** Shots one magazine gives a gun turret. */
    public static final int SHOTS_PER_MAGAZINE = 10;
    /** Damage factor of an armour-piercing magazine. */
    public static final double AP_FACTOR = 1.6;

    private final int health;
    private final double damage;
    private final double range;
    private final int cooldown;
    private final int energyPerShot;
    private final int targets;
    private final int fluidPerShot;

    TowerType(int health, double damage, double range, int cooldown, int energyPerShot, int targets, int fluidPerShot) {
        this.health = health;
        this.damage = damage;
        this.range = range;
        this.cooldown = cooldown;
        this.energyPerShot = energyPerShot;
        this.targets = targets;
        this.fluidPerShot = fluidPerShot;
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

    /** Units of fluid one shot burns (flamethrower), 0 for the others. */
    public int fluidPerShot() {
        return fluidPerShot;
    }

    public boolean usesFluid() {
        return fluidPerShot > 0;
    }

    /** Towers that take items as ammunition (bolts, magazines). */
    public boolean usesItemAmmo() {
        return energyPerShot == 0 && fluidPerShot == 0;
    }

    /** Energy weapons deal magic damage, which armour against physical hits does not reduce. */
    public boolean energyWeapon() {
        return usesEnergy();
    }

    /** Buffer of energy towers: ten shots. */
    public int energyCapacity() {
        return energyPerShot * 10;
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
            case LASER -> 1_200;
            case FLAME -> 900;
        };
    }
}
