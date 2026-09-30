package de.craftorio.defense.sim;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * One tower kind, read from {@code data/craftorio/td_towers/<id>.json}: price, range, base attack and the three
 * upgrade paths of five tiers each.
 *
 * @param btd6   the Bloons TD 6 tower this stands for
 * @param cost   price in coins on Medium
 * @param range  targeting range in blocks, negative for unlimited
 * @param supply what the tower uses up: ammo, fluid, energy, plastic or none
 * @param supplyCost per shot: energy units or fluid units; for ammo and plastic the number of shots one item lasts
 * @param targets target modes it offers
 * @param attack base attack, null for support towers
 * @param paths  {@code paths.get(path).get(tier - 1)}
 */
public record TowerDef(String id, String btd6, long cost, double range, String supply, int supplyCost, List<TargetMode> targets, Attack attack,
                       List<List<Upgrade>> paths) {
    public static final int PATHS = 3;
    public static final int TIERS = 5;

    /** One upgrade: its price on Medium and the effects it has on the tower's {@link TowerProfile}. */
    public record Upgrade(String btd6, long cost, List<JsonObject> effects) {
    }

    public Upgrade upgrade(int path, int tier) {
        return paths.get(path).get(tier - 1);
    }

    public boolean unlimitedRange() {
        return range < 0;
    }

    /** The mode a fresh tower starts with. */
    public TargetMode defaultTarget() {
        return targets.isEmpty() ? TargetMode.FIRST : targets.get(0);
    }
}
