package de.craftorio.defense.sim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * A tower in action, without Minecraft: its upgrades, where it stands and how long until its attacks are ready. The
 * block entity of the game and the headless balancing simulator both run the same code.
 */
public final class TowerUnit {
    /** Pays for a shot (ammunition, energy, oil, plastic); false stops the tower from shooting. */
    @FunctionalInterface
    public interface Supply {
        boolean pay(TowerDef def, Attack attack);

        Supply FREE = (def, attack) -> true;
    }

    /** What one shot did, for effects: the attack, where it came from and the enemies it hit. */
    public record Shot(Attack attack, double fromX, double fromZ, double aimX, double aimZ, List<SimEnemy> hits) {
    }

    /** A tower never shoots more often than this per tick, however fast its attack is. */
    private static final int MAX_SHOTS_PER_TICK = 3;

    private final TowerDef def;
    private final int[] tiers = new int[TowerDef.PATHS];
    private final double x;
    private final double y;
    private final double z;
    private final Random random;
    private TargetMode mode;
    private TowerProfile profile;
    /** The ammunition in use changes the damage kind and strength of hits (magazines); null for the plain kind. */
    private DamageKind ammoKind;
    private double ammoFactor = 1;
    private double[] timers = new double[0];
    /** Ticks until each ability can be used again, and ticks each one is still in effect. */
    private int[] abilityCooldown = new int[0];
    private int[] abilityActive = new int[0];

    public TowerUnit(TowerDef def, double x, double y, double z, long seed) {
        this.def = def;
        this.x = x;
        this.y = y;
        this.z = z;
        this.random = new Random(seed);
        this.mode = def.defaultTarget();
        setTiers(new int[TowerDef.PATHS]);
    }

    public TowerDef def() {
        return def;
    }

    public int[] tiers() {
        return tiers.clone();
    }

    public TowerProfile profile() {
        return profile;
    }

    /** Sets what the loaded ammunition does: the damage kind it deals (null keeps the attack's own) and a factor on the damage. */
    public void setAmmo(DamageKind kind, double factor) {
        this.ammoKind = kind;
        this.ammoFactor = factor;
    }

    public TargetMode mode() {
        return mode;
    }

    public void setMode(TargetMode mode) {
        this.mode = def.targets().contains(mode) ? mode : def.defaultTarget();
    }

    /** Sets the upgrades; attacks that are new start ready, the others keep their waiting time. */
    public void setTiers(int[] newTiers) {
        System.arraycopy(newTiers, 0, tiers, 0, TowerDef.PATHS);
        profile = TowerDefs.profile(def, tiers);
        double[] resized = new double[profile.attacks.size()];
        for (int i = 0; i < resized.length; i++) {
            resized[i] = i < timers.length ? timers[i] : profile.attacks.get(i).cooldownTicks();
        }
        timers = resized;
        int abilities = profile.abilities.size();
        abilityCooldown = java.util.Arrays.copyOf(abilityCooldown, abilities);
        abilityActive = java.util.Arrays.copyOf(abilityActive, abilities);
    }

    // --- abilities

    public int abilityCount() {
        return profile.abilities.size();
    }

    public boolean abilityReady(int index) {
        return index >= 0 && index < abilityCooldown.length && abilityCooldown[index] <= 0;
    }

    /** Ticks until the ability can be used again (0 when ready). */
    public int abilityCooldown(int index) {
        return index < 0 || index >= abilityCooldown.length ? 0 : Math.max(0, abilityCooldown[index]);
    }

    /** Ticks the ability stays in effect (0 when it is not active). */
    public int abilityActive(int index) {
        return index < 0 || index >= abilityActive.length ? 0 : Math.max(0, abilityActive[index]);
    }

    public boolean abilityIsActive(String id) {
        for (int i = 0; i < profile.abilities.size(); i++) {
            if (profile.abilities.get(i).id().equals(id) && abilityActive[i] > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Uses the ability if it is ready: starts its cooldown (reduced by {@code cooldownFactor}, e.g. by a village) and its
     * duration. The effect itself is up to the caller; towers ask {@link #abilityIsActive} for buffs that last.
     *
     * @return the ability used, or null if it is not ready
     */
    public TowerProfile.Ability activate(int index, double cooldownFactor) {
        if (!abilityReady(index)) {
            return null;
        }
        TowerProfile.Ability ability = profile.abilities.get(index);
        abilityCooldown[index] = (int) Math.round(ability.cooldown() * TdUnits.TICKS_PER_SECOND * cooldownFactor);
        abilityActive[index] = (int) Math.round(ability.duration() * TdUnits.TICKS_PER_SECOND);
        return ability;
    }

    /** Sets the ability timers (when a tower is loaded from disk). */
    public void restoreAbilities(int[] cooldowns, int[] actives) {
        for (int i = 0; i < abilityCooldown.length; i++) {
            abilityCooldown[i] = i < cooldowns.length ? cooldowns[i] : 0;
            abilityActive[i] = i < actives.length ? actives[i] : 0;
        }
    }

    public int[] abilityCooldowns() {
        return abilityCooldown.clone();
    }

    public int[] abilityActives() {
        return abilityActive.clone();
    }

    /** Targeting range in blocks, negative for unlimited, including the range bonus of the place the tower stands on. */
    public double range(double rangeFactor) {
        return profile.range < 0 ? profile.range : profile.range * rangeFactor;
    }

    /**
     * One tick: lets every attack that is ready shoot at the best enemy in range.
     *
     * @param rangeFactor 1 normally, more on a plateau or inside a village's radius
     * @return the shots fired
     */
    public List<Shot> tick(TdSimulation sim, Supply supply, double rangeFactor, boolean villageCamo) {
        for (int i = 0; i < abilityCooldown.length; i++) {
            abilityCooldown[i] = Math.max(0, abilityCooldown[i] - 1);
            abilityActive[i] = Math.max(0, abilityActive[i] - 1);
        }
        List<Shot> shots = new ArrayList<>(0);
        boolean camo = profile.detectsCamo || villageCamo;
        double range = range(rangeFactor);
        for (int i = 0; i < timers.length; i++) {
            Attack attack = profile.attacks.get(i);
            double cooldown = attack.cooldownTicks();
            timers[i] = Math.min(timers[i], cooldown) + 1;
            for (int n = 0; n < MAX_SHOTS_PER_TICK && timers[i] >= cooldown; n++) {
                Predicate<SimEnemy> filter = attack.skipBoss ? enemy -> !enemy.def().boss() : null;
                SimEnemy target = sim.bestTarget(x, z, range, camo, mode.order(x, y, z), filter);
                if (target == null || !supply.pay(def, attack)) {
                    break;
                }
                shots.add(fire(sim, attack, target, camo));
                timers[i] -= cooldown;
            }
        }
        return shots;
    }

    private Shot fire(TdSimulation sim, Attack attack, SimEnemy target, boolean camo) {
        List<SimEnemy> hits = new ArrayList<>();
        switch (attack.kind) {
            case INSTANT -> hit(sim, attack, target, hits);
            case AREA -> {
                List<SimEnemy> around = within(sim, target.x(), target.z(), attack.radius, true, attack);
                around.sort(Comparator.comparingDouble(enemy -> enemy.flatDistanceSqr(target.x(), target.z())));
                for (int i = 0; i < around.size() && i < attack.pierce; i++) {
                    hit(sim, attack, around.get(i), hits);
                }
            }
            case AURA -> {
                List<SimEnemy> around = within(sim, x, z, attack.radius, true, attack);
                around.sort(Comparator.comparingDouble(enemy -> enemy.flatDistanceSqr(x, z)));
                for (int i = 0; i < around.size() && i < attack.pierce; i++) {
                    hit(sim, attack, around.get(i), hits);
                }
            }
            case PROJECTILE, RING -> {
                double base = Math.atan2(target.z() - z, target.x() - x);
                int count = Math.max(1, attack.projectiles);
                for (int p = 0; p < count; p++) {
                    double angle = attack.kind == AttackKind.RING ? base + 2 * Math.PI * p / count
                            : count == 1 ? base : base + Math.toRadians(attack.spread) * (p / (double) (count - 1) - 0.5);
                    if (attack.randomAngle > 0) {
                        angle += Math.toRadians((random.nextDouble() * 2 - 1) * attack.randomAngle / 2);
                    }
                    for (SimEnemy enemy : rayHits(sim, attack, Math.cos(angle), Math.sin(angle), camo)) {
                        hit(sim, attack, enemy, hits);
                    }
                }
            }
        }
        return new Shot(attack, x, z, target.x(), target.z(), hits);
    }

    /** Enemies within a circle; camouflaged ones only if the blast may catch them. */
    private List<SimEnemy> within(TdSimulation sim, double cx, double cz, double radius, boolean camo, Attack attack) {
        List<SimEnemy> found = new ArrayList<>();
        for (SimEnemy enemy : sim.enemies()) {
            if (enemy.flatDistanceSqr(cx, cz) <= Math.pow(radius + enemy.def().radius() * 0.5, 2) && (camo || !enemy.camo)
                    && !(attack.skipBoss && enemy.def().boss())) {
                found.add(enemy);
            }
        }
        return found;
    }

    /** The enemies a projectile flying from the tower in this direction touches, nearest first, at most {@code pierce}. */
    private List<SimEnemy> rayHits(TdSimulation sim, Attack attack, double dx, double dz, boolean camo) {
        List<SimEnemy> candidates = new ArrayList<>();
        List<Double> along = new ArrayList<>();
        for (SimEnemy enemy : sim.enemies()) {
            if (!camo && !attack.hitsCamo && enemy.camo()) {
                continue;
            }
            if (attack.skipBoss && enemy.def().boss()) {
                continue;
            }
            double vx = enemy.x() - x;
            double vz = enemy.z() - z;
            double t = vx * dx + vz * dz;
            double reach = enemy.def().radius();
            if (t < -reach || t > attack.length + reach) {
                continue;
            }
            double perpendicular = Math.abs(vx * dz - vz * dx);
            if (perpendicular <= attack.radius + reach) {
                candidates.add(enemy);
                along.add(t);
            }
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingDouble(along::get));
        List<SimEnemy> hits = new ArrayList<>();
        for (int i = 0; i < order.size() && hits.size() < attack.pierce; i++) {
            hits.add(candidates.get(order.get(i)));
        }
        return hits;
    }

    private void hit(TdSimulation sim, Attack attack, SimEnemy enemy, List<SimEnemy> hits) {
        double damage = attack.damageAgainst(enemy);
        if (ammoFactor != 1 && damage > 0) {
            damage = Math.max(1, Math.round(damage * ammoFactor));
        }
        if (damage > 0) {
            sim.hit(enemy, damage, ammoKind != null ? ammoKind : attack.damageKind);
        }
        hits.add(enemy);
    }
}
