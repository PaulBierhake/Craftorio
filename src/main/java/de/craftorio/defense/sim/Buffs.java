package de.craftorio.defense.sim;

/**
 * Temporary or aura bonuses a tower gets from outside: a village's drums, an ability's boost. Factors multiply, the other
 * values add up.
 *
 * @param cooldown multiplies the attack cooldown (0.5 = twice as fast)
 * @param range    multiplies the range
 * @param pierce   extra pierce for every attack
 * @param damage   extra damage for every attack
 * @param camo     sees camouflaged enemies
 * @param kind     every hit deals this damage kind instead (null: keep)
 * @param radius   extra radius of projectiles, blasts and auras (blocks)
 */
public record Buffs(double cooldown, double range, int pierce, double damage, boolean camo, DamageKind kind, double radius) {
    public static final Buffs NONE = new Buffs(1, 1, 0, 0, false, null, 0);

    public Buffs merge(Buffs other) {
        return new Buffs(cooldown * other.cooldown, range * other.range, pierce + other.pierce, damage + other.damage, camo || other.camo,
                other.kind != null ? other.kind : kind, radius + other.radius);
    }

    public static Buffs faster(double cooldownFactor) {
        return new Buffs(cooldownFactor, 1, 0, 0, false, null, 0);
    }

    public static Buffs seesCamo() {
        return new Buffs(1, 1, 0, 0, true, null, 0);
    }
}
