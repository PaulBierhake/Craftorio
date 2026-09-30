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
