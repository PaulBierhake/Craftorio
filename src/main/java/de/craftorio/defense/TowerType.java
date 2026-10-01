package de.craftorio.defense;

import de.craftorio.defense.sim.TowerDef;
import de.craftorio.defense.sim.TowerDefs;

/**
 * The tower blocks of the game. Everything about what a tower does (price, range, attacks, upgrades) lives in its
 * {@link TowerDef}, read from {@code data/craftorio/td_towers}.
 */
public enum TowerType {
    CROSSBOW("crossbow_tower"),
    GUN("gun_turret"),
    FLAME("flamethrower_turret"),
    MORTAR("mortar_turret"),
    DEPOT("supply_depot"),
    TESLA("tesla_tower"),
    LASER("laser_tower");

    /** Shots one magazine gives a gun turret. */
    public static final int SHOTS_PER_MAGAZINE = 10;
    /** Damage factor of an armour-piercing magazine. */
    public static final double AP_FACTOR = 1.6;
    /** Damage factor of a uranium magazine (24 against 5 of the plain one in Factorio). */
    public static final double URANIUM_FACTOR = 4.8;

    private final String defId;

    TowerType(String defId) {
        this.defId = defId;
    }

    public String defId() {
        return defId;
    }

    public TowerDef def() {
        return TowerDefs.get(defId);
    }

    /** Towers that take items as ammunition (bolts, magazines, grenades, plastic). */
    public boolean usesItemAmmo() {
        String supply = def().supply();
        return supply.equals("ammo") || supply.equals("plastic");
    }

    public boolean usesEnergy() {
        return def().supply().equals("energy");
    }

    public boolean usesFluid() {
        return def().supply().equals("fluid");
    }

    /** Energy one shot takes. */
    public int energyPerShot() {
        return usesEnergy() ? def().supplyCost() : 0;
    }

    /** Units of oil one shot burns. */
    public int fluidPerShot() {
        return usesFluid() ? def().supplyCost() : 0;
    }

    /** Buffer of energy towers: ten shots, at least 200. */
    public int energyCapacity() {
        return usesEnergy() ? Math.max(200, energyPerShot() * 10) : 0;
    }

    /** Does the tower shoot? Support towers (command post, supply depot) do not. */
    public boolean attacks() {
        return def().attack() != null;
    }
}
