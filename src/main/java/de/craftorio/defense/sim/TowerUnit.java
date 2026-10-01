package de.craftorio.defense.sim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    /** Attacks started by attacks (cluster bombs, shrapnel) per shot, so that chains stay cheap. */
    private static final int CHILD_BUDGET = 240;
    private static final int MAX_DEPTH = 4;

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
    private long[] shotCounts = new long[0];
    /** Ticks until each ability can be used again, and ticks each one is still in effect. */
    private int[] abilityCooldown = new int[0];
    private int[] abilityActive = new int[0];
    /** Timed buffs granted from outside, by id: ticks left and the buff. */
    private final Map<String, int[]> buffTicks = new HashMap<>();
    private final Map<String, Buffs> buffValues = new HashMap<>();
    /** Coins earned by hits that pay coins (supply drops are handled elsewhere); collected by the caller. */
    private double coins;

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

    public double x() {
        return x;
    }

    public double z() {
        return z;
    }

    public int[] tiers() {
        return tiers.clone();
    }

    public TowerProfile profile() {
        return profile;
    }

    /** Gives the unit a profile of its own (a copy of the shared one) and lets {@code tweak} change it. */
    public void customize(java.util.function.Consumer<TowerProfile> tweak) {
        profile = profile.copy();
        tweak.accept(profile);
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
        long[] counts = new long[resized.length];
        for (int i = 0; i < resized.length; i++) {
            resized[i] = i < timers.length ? timers[i] : profile.attacks.get(i).cooldownTicks();
            counts[i] = i < shotCounts.length ? shotCounts[i] : 0;
        }
        timers = resized;
        shotCounts = counts;
        int abilities = profile.abilities.size();
        abilityCooldown = java.util.Arrays.copyOf(abilityCooldown, abilities);
        abilityActive = java.util.Arrays.copyOf(abilityActive, abilities);
    }

    // --- buffs

    /** Gives the tower a buff for a while; the same id again renews it. */
    public void grantBuff(String id, int ticks, Buffs buff) {
        buffTicks.put(id, new int[]{ticks});
        buffValues.put(id, buff);
    }

    public boolean hasBuff(String id) {
        return buffTicks.containsKey(id);
    }

    private Buffs activeBuffs(Buffs outside) {
        Buffs total = outside;
        for (Map.Entry<String, Buffs> entry : buffValues.entrySet()) {
            total = total.merge(entry.getValue());
        }
        return total;
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

    /** Coins earned by hits since the last call. */
    public double takeCoins() {
        double earned = coins;
        coins = 0;
        return earned;
    }

    /** Targeting range in blocks, negative for unlimited, including the range bonus of the place the tower stands on. */
    public double range(double rangeFactor) {
        return profile.range < 0 ? profile.range : profile.range * rangeFactor;
    }

    /** One tick without outside buffs; see {@link #tick(TdSimulation, Supply, double, Buffs)}. */
    public List<Shot> tick(TdSimulation sim, Supply supply, double rangeFactor, boolean villageCamo) {
        return tick(sim, supply, rangeFactor, villageCamo ? Buffs.seesCamo() : Buffs.NONE);
    }

    /**
     * One tick: lets every attack that is ready shoot at the best enemy in range.
     *
     * @param rangeFactor 1 normally, more on a plateau
     * @param outside     buffs from a village or similar, on top of the timed ones granted with {@link #grantBuff}
     * @return the shots fired
     */
    public List<Shot> tick(TdSimulation sim, Supply supply, double rangeFactor, Buffs outside) {
        for (int i = 0; i < abilityCooldown.length; i++) {
            abilityCooldown[i] = Math.max(0, abilityCooldown[i] - 1);
            abilityActive[i] = Math.max(0, abilityActive[i] - 1);
        }
        if (!buffTicks.isEmpty()) {
            buffTicks.entrySet().removeIf(entry -> {
                if (--entry.getValue()[0] <= 0) {
                    buffValues.remove(entry.getKey());
                    return true;
                }
                return false;
            });
        }
        Buffs buffs = activeBuffs(outside);
        List<Shot> shots = new ArrayList<>(0);
        boolean camo = profile.detectsCamo || buffs.camo();
        double range = profile.range < 0 ? profile.range : profile.range * rangeFactor * buffs.range();
        for (int i = 0; i < timers.length; i++) {
            Attack attack = profile.attacks.get(i);
            if (!attack.abilityId.isEmpty() && !abilityIsActive(attack.abilityId)) {
                continue;
            }
            double cooldown = Math.max(0.05, attack.cooldownTicks() * buffs.cooldown());
            timers[i] = Math.min(timers[i], cooldown) + 1;
            for (int n = 0; n < MAX_SHOTS_PER_TICK && timers[i] >= cooldown; n++) {
                Predicate<SimEnemy> filter = eligible(attack);
                SimEnemy target = attack.noTarget ? null : sim.bestTarget(x, z, attack.unlimitedRange ? -1 : range, camo || attack.camoOnly || attack.hitsCamo, mode.order(x, y, z), filter);
                if (target == null && !attack.noTarget || attack.abilityId.isEmpty() && !attack.free && !supply.pay(def, attack)) {
                    break;
                }
                shotCounts[i]++;
                shots.add(fire(sim, attack, target, camo, buffs, shotCounts[i]));
                timers[i] -= cooldown;
            }
        }
        return shots;
    }

    private static Predicate<SimEnemy> eligible(Attack attack) {
        if (!attack.skipBoss && !attack.skipFrozen && !attack.bossOnly && !attack.camoOnly && !(attack.glue > 0 && attack.glueLevel > 0)) {
            return null;
        }
        return enemy -> !(attack.skipBoss && enemy.def().boss()) && !(attack.skipFrozen && enemy.frozen()) && !(attack.bossOnly && !enemy.def().boss())
                && !(attack.camoOnly && !enemy.camo()) && !(attack.glue > 0 && attack.glueLevel > 0 && attack.glueLevel <= enemy.glueLevelNow());
    }

    /** What a shot needs while it runs: the buffs, how many child attacks may still start, and the enemies hit. */
    private static final class Context {
        final Buffs buffs;
        final boolean camo;
        final List<SimEnemy> hits = new ArrayList<>();
        int budget = CHILD_BUDGET;
        boolean crit;

        Context(Buffs buffs, boolean camo) {
            this.buffs = buffs;
            this.camo = camo;
        }
    }

    private Shot fire(TdSimulation sim, Attack attack, SimEnemy target, boolean camo, Buffs buffs, long shotNumber) {
        Context context = new Context(buffs, camo);
        context.crit = attack.critEvery > 0 && shotNumber % attack.critEvery == 0;
        if (target == null) {
            double spin = shotNumber * 0.9;
            resolve(sim, attack, x, z, spin, null, context, 0);
            return new Shot(attack, x, z, x + Math.cos(spin) * attack.length, z + Math.sin(spin) * attack.length, context.hits);
        }
        double angle = Math.atan2(target.z() - z, target.x() - x);
        if (attack.kind == AttackKind.AREA) {
            resolve(sim, attack, target.x(), target.z(), angle, target, context, 0);
        } else {
            resolve(sim, attack, x, z, angle, target, context, 0);
        }
        return new Shot(attack, x, z, target.x(), target.z(), context.hits);
    }

    /**
     * Runs one attack from a place: finds who it hits, applies damage and effects and starts its child attacks.
     *
     * @param primary the enemy aimed at (null for children that look for their own targets)
     */
    private void resolve(TdSimulation sim, Attack a, double ox, double oz, double angle, SimEnemy primary, Context c, int depth) {
        List<SimEnemy> direct = new ArrayList<>();
        int pierce = a.pierce + c.buffs.pierce();
        double radius = a.radius + c.buffs.radius();
        switch (a.kind) {
            case INSTANT -> {
                SimEnemy first = primary != null ? primary : nearest(sim, ox, oz, a.length, a, c, direct);
                if (first != null) {
                    direct.add(first);
                    SimEnemy current = first;
                    for (int b = 0; b < a.bounces; b++) {
                        SimEnemy next = nearest(sim, current.x(), current.z(), a.bounceRadius, a, c, direct);
                        if (next == null) {
                            break;
                        }
                        direct.add(next);
                        current = next;
                    }
                }
            }
            case AREA -> {
                for (SimEnemy enemy : within(sim, ox, oz, radius, a, c, true)) {
                    if (direct.size() >= pierce) {
                        break;
                    }
                    direct.add(enemy);
                }
            }
            case AURA -> {
                for (SimEnemy enemy : within(sim, ox, oz, radius, a, c, true)) {
                    if (direct.size() >= pierce) {
                        break;
                    }
                    direct.add(enemy);
                }
            }
            case PROJECTILE, RING -> {
                if (a.homing && primary != null) {
                    direct.add(primary);
                    List<SimEnemy> others = within(sim, primary.x(), primary.z(), Math.max(radius, a.bounceRadius), a, c, false);
                    for (SimEnemy enemy : others) {
                        if (direct.size() >= pierce) {
                            break;
                        }
                        if (enemy != primary) {
                            direct.add(enemy);
                        }
                    }
                } else {
                    int count = Math.max(1, a.projectiles);
                    for (int p = 0; p < count; p++) {
                        double direction = direction(a, angle, p, count, depth);
                        for (SimEnemy enemy : rayHits(sim, a, ox, oz, direction, radius, pierce, c)) {
                            hit(sim, a, enemy, c);
                            direct.add(enemy);
                        }
                        launchOnShot(sim, a, ox, oz, direction, null, c, depth, p == 0);
                    }
                    for (SimEnemy enemy : direct) {
                        childrenAt(sim, a, enemy.x(), enemy.z(), angle, c, depth, "hit");
                    }
                    return;
                }
            }
        }
        for (SimEnemy enemy : direct) {
            hit(sim, a, enemy, c);
        }
        for (SimEnemy enemy : direct) {
            childrenAt(sim, a, enemy.x(), enemy.z(), angle, c, depth, "hit");
        }
        launchOnShot(sim, a, direct.isEmpty() ? ox : direct.get(0).x(), direct.isEmpty() ? oz : direct.get(0).z(), angle, null, c, depth, true);
    }

    /** "shot" children start once per shot, at the first hit (or where the shot ended). */
    private void launchOnShot(TdSimulation sim, Attack a, double ox, double oz, double angle, SimEnemy ignored, Context c, int depth, boolean first) {
        if (first) {
            childrenAt(sim, a, ox, oz, angle, c, depth, "shot");
        }
    }

    private void childrenAt(TdSimulation sim, Attack parent, double ox, double oz, double angle, Context c, int depth, String trigger) {
        if (parent.children.isEmpty() || depth >= MAX_DEPTH) {
            return;
        }
        for (Attack child : parent.children) {
            if (!child.trigger.equals(trigger) || c.budget <= 0) {
                continue;
            }
            int repeats = child.kind == AttackKind.AREA ? Math.max(1, child.projectiles) : 1;
            for (int r = 0; r < repeats && c.budget > 0; r++) {
                c.budget--;
                double cx = ox;
                double cz = oz;
                if (child.scatterMax > 0) {
                    double distance = child.scatterMin + random.nextDouble() * (child.scatterMax - child.scatterMin);
                    double direction = random.nextDouble() * Math.PI * 2;
                    cx += Math.cos(direction) * distance;
                    cz += Math.sin(direction) * distance;
                }
                resolve(sim, child, cx, cz, angle, null, c, depth + 1);
            }
        }
    }

    private double direction(Attack a, double angle, int index, int count, int depth) {
        double result;
        if (a.kind == AttackKind.RING) {
            result = angle + 2 * Math.PI * index / count;
        } else if (count == 1) {
            result = depth > 0 && a.spread >= 360 ? random.nextDouble() * Math.PI * 2 : angle;
        } else if (a.spread >= 359) {
            result = angle + 2 * Math.PI * index / count;
        } else {
            result = angle + Math.toRadians(a.spread) * (index / (double) (count - 1) - 0.5);
        }
        if (a.randomAngle > 0) {
            result += Math.toRadians((random.nextDouble() * 2 - 1) * a.randomAngle / 2);
        }
        return result;
    }

    private SimEnemy nearest(TdSimulation sim, double cx, double cz, double reach, Attack a, Context c, List<SimEnemy> exclude) {
        SimEnemy best = null;
        double bestDistance = reach * reach;
        for (SimEnemy enemy : sim.enemies()) {
            if (!usable(enemy, a, c) || exclude.contains(enemy)) {
                continue;
            }
            double distance = enemy.flatDistanceSqr(cx, cz);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = enemy;
            }
        }
        return best;
    }

    /** May the attack hit this enemy at all (camouflage, MOAB-class, frozen)? */
    private boolean usable(SimEnemy enemy, Attack a, Context c) {
        return usable(enemy, a, c, false);
    }

    /** As above; blasts (explosions) also catch camouflaged enemies next to the one they hit. */
    private boolean usable(SimEnemy enemy, Attack a, Context c, boolean blast) {
        return (c.camo || a.hitsCamo || a.camoOnly || !enemy.camo() || blast && a.kind == AttackKind.AREA) && !(a.skipBoss && enemy.def().boss()) && !(a.skipFrozen && enemy.frozen())
                && !(a.bossOnly && !enemy.def().boss()) && !(a.camoOnly && !enemy.camo())
                && !(a.glue > 0 && a.glueLevel > 0 && a.glueLevel <= enemy.glueLevelNow());
    }

    /** Enemies within a circle, nearest first; camouflaged ones only if the blast may catch them. */
    private List<SimEnemy> within(TdSimulation sim, double cx, double cz, double radius, Attack a, Context c, boolean blast) {
        List<SimEnemy> found = new ArrayList<>();
        for (SimEnemy enemy : sim.enemies()) {
            if (enemy.flatDistanceSqr(cx, cz) <= Math.pow(radius + enemy.def().radius() * 0.5, 2) && usable(enemy, a, c, blast)) {
                found.add(enemy);
            }
        }
        found.sort(Comparator.comparingDouble(enemy -> enemy.flatDistanceSqr(cx, cz)));
        return found;
    }

    /** The enemies a projectile flying from a place in this direction touches, nearest first, at most {@code pierce}. */
    private List<SimEnemy> rayHits(TdSimulation sim, Attack attack, double ox, double oz, double angle, double radius, int pierce, Context c) {
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);
        List<SimEnemy> candidates = new ArrayList<>();
        List<Double> along = new ArrayList<>();
        for (SimEnemy enemy : sim.enemies()) {
            if (!usable(enemy, attack, c)) {
                continue;
            }
            double vx = enemy.x() - ox;
            double vz = enemy.z() - oz;
            double t = vx * dx + vz * dz;
            double reach = enemy.def().radius();
            if (t < -reach || t > attack.length + reach) {
                continue;
            }
            double perpendicular = Math.abs(vx * dz - vz * dx);
            if (perpendicular <= radius + reach) {
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
        for (int i = 0; i < order.size() && hits.size() < pierce; i++) {
            hits.add(candidates.get(order.get(i)));
        }
        return hits;
    }

    /** Seconds a maim immobilises a MOAB-class enemy (Maim MOAB / Cripple MOAB). */
    static double maimSeconds(int tier, String enemyId) {
        double[] normal = {3, 1.5, 0.75, 0.75, 0};
        double[] crippled = {7, 6, 3, 4, 0.75};
        int index = switch (enemyId) {
            case "brood_mother" -> 0;
            case "behemoth" -> 1;
            case "colossus" -> 2;
            case "shadow_hunter" -> 3;
            default -> 4;
        };
        return (tier >= 5 ? crippled : normal)[index];
    }

    /** Damage and effects on one enemy. */
    private void hit(TdSimulation sim, Attack attack, SimEnemy enemy, Context c) {
        double damage = (c.crit && attack.critDamage > 0 ? attack.critDamage : attack.damage) + c.buffs.damage();
        double bonus = attack.damageAgainst(enemy) - attack.damage;
        damage += bonus;
        if (ammoFactor != 1 && damage > 0) {
            damage = Math.max(1, Math.round(damage * ammoFactor));
        }
        DamageKind kind = c.buffs.kind() != null ? c.buffs.kind() : ammoKind != null ? ammoKind : attack.damageKind;
        EnemyDef def = enemy.def();
        boolean immune = def.isImmune(kind) || enemy.frozen() && kind == DamageKind.SHARP;
        c.hits.add(enemy);
        if (damage > 0) {
            sim.hit(enemy, damage, kind, attack.soak);
        }
        if (!enemy.alive()) {
            // the enemy popped: its children are new enemies, effects below apply to the survivor only
        }
        if (attack.coinsPerHit > 0) {
            coins += attack.coinsPerHit;
        }
        if (immune && attack.damage > 0) {
            return;
        }
        if (attack.maim > 0 && enemy.alive() && def.boss()) {
            sim.stun(enemy, 0, maimSeconds(attack.maim, def.id()));
        }
        if (attack.stun > 0 && enemy.alive()) {
            sim.stun(enemy, attack.stun, attack.stunBoss);
        }
        if (attack.freeze > 0 && enemy.alive()) {
            sim.freeze(enemy, attack.freeze);
        }
        if (attack.glue > 0 && enemy.alive()) {
            if (!def.boss()) {
                sim.slow(enemy, attack.glue, attack.glueDuration, false, attack.glueLevel);
            } else if (attack.glueBoss > 0) {
                sim.slow(enemy, attack.glueBoss, attack.glueBossSeconds, true, attack.glueLevel);
            }
        }
        if (attack.brittle > 0 && enemy.alive()) {
            sim.makeBrittle(enemy, attack.brittle, attack.brittleSeconds);
        }
        if (attack.burn > 0 && enemy.alive()) {
            sim.burn(enemy, attack.burn, attack.burnSeconds);
        }
        if (attack.pushback != 0 && enemy.alive()) {
            sim.push(enemy, attack.pushback * (def.boss() ? attack.pushbackBoss : 1));
        }
        if (attack.stripCamo && enemy.alive() && !def.boss()) {
            sim.stripModifiers(enemy);
        }
    }
}
