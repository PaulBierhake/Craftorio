package de.craftorio.defense.sim;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One attack of a tower with all its numbers; upgrades change them (see {@link TowerProfile}). Distances are in
 * blocks, times in seconds.
 */
public final class Attack {
    public String id = "main";
    public AttackKind kind = AttackKind.PROJECTILE;
    public double cooldown = 1;
    public int projectiles = 1;
    /** Total angle the projectiles are spread over, in degrees (360 for a ring). */
    public double spread;
    public double randomAngle;
    public int pierce = 1;
    public double damage = 1;
    public DamageKind damageKind = DamageKind.SHARP;
    /** How far a projectile flies. */
    public double length = 8;
    /** Hit radius of a projectile, or the radius of a blast or an aura. */
    public double radius = 0.2;
    /** Extra damage against enemies of a class: lead, ceramic, fortified, moab, camo, regrow, frozen, glued. */
    public final Map<String, Double> bonus = new LinkedHashMap<>();
    /** Hits camouflaged enemies (only matters if the tower can see them). */
    public boolean hitsCamo;
    /** Cannot be aimed at or hit MOAB-class enemies (frost and glue at first). */
    public boolean skipBoss;
    /** Freeze time in seconds (0: no freeze) and how many layers a frozen enemy loses its freeze on. */
    public double freeze;
    public int freezeLayers = 2;
    /** Glue: speed factor, duration and layers. */
    public double glue;
    public double glueDuration;
    public int glueLayers = 3;

    /** Attacks that start where this one hits ({@code trigger} "hit": at every enemy hit; "shot": once, at the first hit). */
    public final java.util.List<Attack> children = new java.util.ArrayList<>();
    public String trigger = "hit";
    /** A child starts a random distance from its parent's impact (cluster bombs): between these two. */
    public double scatterMin;
    public double scatterMax;
    /** Every {@code critEvery}th shot does {@code critDamage} instead of its damage (0: never). */
    public int critEvery;
    public double critDamage;
    /** Stun in seconds (and for MOAB-class enemies), pushback in blocks (and the factor for MOAB-class), knockback as a speed multiplier. */
    public double stun;
    public double stunBoss;
    public double pushback;
    public double pushbackBoss = 1;
    /** Extra targets the shot bounces to (sniper) within {@code bounceRadius}. */
    public int bounces;
    public double bounceRadius = 4;
    /** Flies straight to its targets whatever stands in between (seeking shots, bullets). */
    public boolean homing;
    /** Damage over time: damage per second and how long. */
    public double burn;
    public double burnSeconds;
    /** Enemies that are not MOAB-class lose their camo and regrow when hit. */
    public boolean stripCamo;
    /** Ignores enemies that are frozen (the frost tower cannot hit them). */
    public boolean skipFrozen;
    /** Coins per hit enemy (supply drops are handled elsewhere). */
    public double coinsPerHit;
    /** Whether only MOAB-class enemies can be hit. */
    public boolean bossOnly;
    /** Only fires while this ability of the tower is active (blade storms); empty for ordinary attacks. */
    public String abilityId = "";
    /** Fires without a target, turning around (blade storms). */
    public boolean noTarget;
    /** Aims at enemies anywhere, whatever the tower's range (meteors). */
    public boolean unlimitedRange;
    /** Maim (sniper): immobilises MOAB-class enemies; 4 or 5 picks the table of the upgrade, 0 none. */
    public int maim;
    /** Reach (length of the shots, radius of an aura) grows with the tower's range upgrades. */
    public boolean followRange;
    /** Damage that is left over after a MOAB-class layer pops goes on to its MOAB-class children (assassins). */
    public boolean soak;

    public Attack copy() {
        Attack copy = new Attack();
        copy.id = id;
        copy.kind = kind;
        copy.cooldown = cooldown;
        copy.projectiles = projectiles;
        copy.spread = spread;
        copy.randomAngle = randomAngle;
        copy.pierce = pierce;
        copy.damage = damage;
        copy.damageKind = damageKind;
        copy.length = length;
        copy.radius = radius;
        copy.bonus.putAll(bonus);
        copy.hitsCamo = hitsCamo;
        copy.skipBoss = skipBoss;
        copy.freeze = freeze;
        copy.freezeLayers = freezeLayers;
        copy.glue = glue;
        copy.glueDuration = glueDuration;
        copy.glueLayers = glueLayers;
        children.forEach(child -> copy.children.add(child.copy()));
        copy.trigger = trigger;
        copy.scatterMin = scatterMin;
        copy.scatterMax = scatterMax;
        copy.critEvery = critEvery;
        copy.critDamage = critDamage;
        copy.stun = stun;
        copy.stunBoss = stunBoss;
        copy.pushback = pushback;
        copy.pushbackBoss = pushbackBoss;
        copy.bounces = bounces;
        copy.bounceRadius = bounceRadius;
        copy.homing = homing;
        copy.burn = burn;
        copy.burnSeconds = burnSeconds;
        copy.stripCamo = stripCamo;
        copy.skipFrozen = skipFrozen;
        copy.coinsPerHit = coinsPerHit;
        copy.bossOnly = bossOnly;
        copy.abilityId = abilityId;
        copy.noTarget = noTarget;
        copy.unlimitedRange = unlimitedRange;
        copy.maim = maim;
        copy.followRange = followRange;
        copy.soak = soak;
        return copy;
    }

    /** Damage against this enemy: the base damage and the bonuses of the classes it belongs to. */
    public double damageAgainst(SimEnemy enemy) {
        double total = damage;
        if (!bonus.isEmpty()) {
            EnemyDef def = enemy.def();
            for (Map.Entry<String, Double> entry : bonus.entrySet()) {
                if (matches(entry.getKey(), enemy, def)) {
                    total += entry.getValue();
                }
            }
        }
        return total;
    }

    /** Is the enemy of the class {@code key}? */
    static boolean matches(String key, SimEnemy enemy, EnemyDef def) {
        return switch (key) {
            case "lead" -> def.id().equals("ironbreaker") || def.id().equals("shadow_hunter");
            case "ceramic" -> def.id().equals("crystal_golem");
            case "moab" -> def.boss();
            case "fortified" -> enemy.fortified();
            case "camo" -> enemy.camo();
            case "regrow" -> enemy.regrow();
            case "non_boss" -> !def.boss();
            default -> false;
        };
    }

    /** Milliseconds-exact cooldown in ticks (may be below 1 for very fast attacks). */
    public double cooldownTicks() {
        return Math.max(0.05, cooldown * TdUnits.TICKS_PER_SECOND);
    }
}
