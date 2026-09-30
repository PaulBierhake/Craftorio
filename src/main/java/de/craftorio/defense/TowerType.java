package de.craftorio.defense;

import de.craftorio.defense.sim.DamageKind;

/** Tower base stats; upgrades scale the damage (see {@link TowerStats}). */
public enum TowerType {
    CROSSBOW(4, 10, 20, 0, 1, 0),
    /** Fires magazines: ten shots each, armour-piercing ones hit 60 % harder, uranium ones 4.8 times. */
    GUN(7, 12, 12, 0, 1, 0),
    /** Uses grid power instead of ammunition and jumps to up to three enemies. */
    TESLA(10, 8, 30, 400, 3, 0),
    /** Long-range energy beam. */
    LASER(30, 14, 20, 800, 1, 0),
    /** Burns crude oil from the arena reserve: short range, hits up to three enemies. */
    FLAME(12, 8, 10, 0, 3, 6);

    /** Shots one magazine gives a gun turret. */
    public static final int SHOTS_PER_MAGAZINE = 10;
    /** Damage factor of an armour-piercing magazine. */
    public static final double AP_FACTOR = 1.6;
    /** Damage factor of a uranium magazine (24 against 5 of the plain one in Factorio). */
    public static final double URANIUM_FACTOR = 4.8;

    private final double damage;
    private final double range;
    private final int cooldown;
    private final int energyPerShot;
    private final int targets;
    private final int fluidPerShot;

    TowerType(double damage, double range, int cooldown, int energyPerShot, int targets, int fluidPerShot) {
        this.damage = damage;
        this.range = range;
        this.cooldown = cooldown;
        this.energyPerShot = energyPerShot;
        this.targets = targets;
        this.fluidPerShot = fluidPerShot;
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

    /** Energy weapons deal energy damage, which leaden enemies shrug off. */
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

    /** What kind of damage its hits are; the heavy magazines of the gun hit everything. */
    public DamageKind damageKind(Magazine magazine) {
        return switch (this) {
            case CROSSBOW -> DamageKind.SHARP;
            case GUN -> magazine == Magazine.NORMAL ? DamageKind.SHARP : DamageKind.NORMAL;
            case TESLA, LASER -> DamageKind.ENERGY;
            case FLAME -> DamageKind.FIRE;
        };
    }
}
