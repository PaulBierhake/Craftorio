package de.craftorio.defense.sim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The enemies of one arena, without any Minecraft entities: they are numbers walking a {@link TdPath}. Pure logic
 * so thousands of enemies cost next to nothing and every rule can be unit tested.
 */
public final class TdSimulation {
    /** From this round on the late-game rules of Bloons TD 6 apply. */
    public static final int LATE_GAME_ROUND = 81;
    /** A regrowing enemy heals one layer this often (3 s). */
    public static final int REGROW_TICKS = 3 * TdUnits.TICKS_PER_SECOND;
    private static final double EPSILON = 1e-9;

    private final TdPath path;
    private final List<SimEnemy> enemies = new ArrayList<>();
    private int nextId = 1;
    private int round = 1;
    private boolean late;
    private double hpFactor = 1;
    private double speedFactor = 1;
    private double externalSpeed = 1;
    private boolean dirty;
    private int leaked;
    private int pops;
    private int paidPops;
    private double coins;
    /** Living enemies by the round they came from. */
    private final java.util.Map<Integer, Integer> aliveByRound = new java.util.HashMap<>();

    public TdSimulation(TdPath path) {
        this.path = path;
    }

    public TdPath path() {
        return path;
    }

    // --- round rules

    /** Sets the round whose rules apply to enemies spawned or popped from now on. */
    public void setRound(int round) {
        this.round = round;
        this.late = round >= LATE_GAME_ROUND;
        this.hpFactor = lateHpFactor(round);
        this.speedFactor = lateSpeedFactor(round);
    }

    public int round() {
        return round;
    }

    /** The speed factor all enemies walk with now: late game and mutators. */
    public double speed() {
        return speedFactor * externalSpeed;
    }

    public boolean lateGame() {
        return late;
    }

    public double hpFactor() {
        return hpFactor;
    }

    /** Multiplies every enemy's speed (mutators and the like); 1 is normal. */
    public void setExternalSpeed(double factor) {
        this.externalSpeed = factor;
    }

    private record Band(int from, double at, int to, double end) {
    }

    /** Hit points of MOAB-class enemies by round (bloons wiki, Freeplay): multiplier at the first and last round of each band. */
    private static final Band[] HP_BANDS = {
            new Band(81, 1.02, 100, 1.40), new Band(101, 1.45, 125, 2.75), new Band(126, 2.90, 150, 6.50),
            new Band(151, 6.85, 250, 41.50), new Band(251, 42.50, 300, 91.50), new Band(301, 93.0, 400, 241.50),
            new Band(401, 244.0, 500, 491.50)};
    /** Speed of all enemies by round. */
    private static final Band[] SPEED_BANDS = {
            new Band(81, 1.02, 100, 1.40), new Band(101, 1.60, 150, 2.58), new Band(151, 3.00, 200, 3.98),
            new Band(201, 4.50, 251, 5.50)};

    public static double lateHpFactor(int round) {
        if (round < LATE_GAME_ROUND) {
            return 1;
        }
        double value = interpolate(HP_BANDS, round);
        // From round 501 on +500 % of the base value per round.
        return value >= 0 ? value : 496.5 + 5.0 * (round - 501);
    }

    public static double lateSpeedFactor(int round) {
        if (round < LATE_GAME_ROUND) {
            return 1;
        }
        double value = interpolate(SPEED_BANDS, round);
        return value >= 0 ? value : 6.0 + 0.02 * (round - 252);
    }

    private static double interpolate(Band[] bands, int round) {
        for (Band band : bands) {
            if (round >= band.from && round <= band.to) {
                return band.at + (band.end - band.at) * (round - band.from) / (double) (band.to - band.from);
            }
        }
        return -1;
    }

    // --- spawning

    /** Spawns an enemy at the start of the path. Modifiers that do not apply (fortified imps, regrowing bosses) are ignored. */
    public SimEnemy spawn(String defId, boolean camo, boolean regrow, boolean fortified) {
        EnemyDef def = EnemyDefs.get(defId);
        SimEnemy enemy = new SimEnemy(nextId++);
        enemy.def = def;
        enemy.camo = camo || def.camo();
        enemy.regrow = regrow && !def.boss();
        enemy.fortified = fortified && def.canBeFortified();
        enemy.regrowTop = def.id();
        enemy.spawnRound = round;
        aliveByRound.merge(round, 1, Integer::sum);
        enemy.hp = layerHp(def, enemy.fortified);
        place(enemy, 0, 0);
        refreshRbe(enemy);
        enemies.add(enemy);
        return enemy;
    }

    public SimEnemy spawn(String defId) {
        return spawn(defId, false, false, false);
    }

    private double layerHp(EnemyDef def, boolean fortified) {
        return def.hp(late, fortified) * (def.boss() ? hpFactor : 1);
    }

    private void place(SimEnemy enemy, double distance, int segment) {
        enemy.distance = distance;
        double[] out = new double[3];
        enemy.segment = path.position(distance, segment, out);
        enemy.x = out[0];
        enemy.y = out[1];
        enemy.z = out[2];
    }

    private void refreshRbe(SimEnemy enemy) {
        double children = 0;
        boolean fort = enemy.fortified && enemy.def.boss();
        for (String child : enemy.def.children(late)) {
            children += EnemyDefs.rbe(EnemyDefs.get(child), late, fort, hpFactor);
        }
        enemy.rbe = enemy.hp + children;
    }

    // --- time

    /** Moves everything one tick: walking, regrowing; enemies at the end of the path leak. */
    public void tick() {
        compact();
        double factor = speedFactor * externalSpeed;
        boolean removed = false;
        for (int i = 0, n = enemies.size(); i < n; i++) {
            SimEnemy enemy = enemies.get(i);
            if (enemy.regrow && !enemy.def.boss() && !enemy.def.id().equals(enemy.regrowTop)) {
                if (++enemy.regrowTimer >= REGROW_TICKS) {
                    regrow(enemy);
                }
            }
            enemy.distance += enemy.def.blocksPerTick() * enemy.speedFactor * factor;
            if (enemy.distance >= path.length()) {
                leaked += lifeCost(enemy);
                retire(enemy);
                removed = true;
                continue;
            }
            double[] out = scratch;
            enemy.segment = path.position(enemy.distance, enemy.segment, out);
            enemy.x = out[0];
            enemy.y = out[1];
            enemy.z = out[2];
        }
        dirty |= removed;
        compact();
    }

    private final double[] scratch = new double[3];

    private void regrow(SimEnemy enemy) {
        String next = EnemyDefs.regrowStep(enemy.regrowTop, enemy.def.id());
        enemy.regrowTimer = 0;
        if (next == null) {
            enemy.regrowTop = enemy.def.id();
            return;
        }
        enemy.def = EnemyDefs.get(next);
        enemy.fortified = false;
        enemy.hp = layerHp(enemy.def, false);
        enemy.freeLayers++;
        refreshRbe(enemy);
    }

    /** Lives a leaking enemy costs: what is left of it with all its children (at least 1); see {@link EnemyDefs#leak}. */
    public int lifeCost(SimEnemy enemy) {
        boolean fortified = enemy.fortified && enemy.def.canBeFortified();
        double total = EnemyDefs.leak(enemy.def, late, fortified);
        double missing = (layerHp(enemy.def, fortified) - enemy.hp) / (enemy.def.boss() ? hpFactor : 1);
        return (int) Math.max(1, Math.round(total - missing));
    }

    /** The enemy is gone (popped or leaked). */
    private void retire(SimEnemy enemy) {
        enemy.alive = false;
        aliveByRound.merge(enemy.spawnRound, -1, Integer::sum);
    }

    /** Enemies still alive that entered in this round or (with their children) came from it. */
    public int aliveOfRound(int round) {
        return Math.max(0, aliveByRound.getOrDefault(round, 0));
    }

    private void compact() {
        if (dirty) {
            enemies.removeIf(enemy -> !enemy.alive);
            dirty = false;
        }
    }

    // --- damage

    private record Work(SimEnemy enemy, double damage) {
    }

    /**
     * Deals damage: layers are popped while damage is left over and each pop releases the enemy's children; the excess
     * goes on to the children of ordinary enemies (not of MOAB-class ones).
     *
     * @return the layers that paid a coin (regrown layers do not)
     */
    public int hit(SimEnemy target, double damage, DamageKind kind) {
        if (!target.alive || damage <= 0) {
            return 0;
        }
        int paid = 0;
        ArrayList<Work> work = new ArrayList<>(4);
        work.add(new Work(target, damage));
        while (!work.isEmpty()) {
            Work item = work.remove(work.size() - 1);
            SimEnemy enemy = item.enemy;
            if (!enemy.alive || enemy.def.isImmune(kind)) {
                continue;
            }
            enemy.hp -= item.damage;
            if (enemy.hp > EPSILON) {
                refreshRbe(enemy);
                continue;
            }
            double excess = -enemy.hp;
            boolean free = enemy.freeLayers > 0;
            List<SimEnemy> children = pop(enemy);
            pops++;
            if (!free) {
                paid++;
                paidPops++;
                coins += enemy.def.popCash(late) * RoundRules.incomeFactor(enemy.spawnRound);
            }
            if (excess > EPSILON && !enemy.def.boss()) {
                for (SimEnemy child : children) {
                    work.add(new Work(child, excess));
                }
            }
        }
        return paid;
    }

    /** Destroys the enemy's current layer and releases its children in its place. */
    private List<SimEnemy> pop(SimEnemy enemy) {
        retire(enemy);
        dirty = true;
        List<String> kinds = enemy.def.children(late);
        if (kinds.isEmpty()) {
            return List.of();
        }
        List<SimEnemy> children = new ArrayList<>(kinds.size());
        boolean boss = enemy.def.boss();
        for (String kind : kinds) {
            EnemyDef def = EnemyDefs.get(kind);
            SimEnemy child = new SimEnemy(nextId++);
            child.def = def;
            child.fortified = enemy.fortified && def.canBeFortified();
            child.camo = enemy.camo || def.camo() || enemy.def.childMods().contains("camo");
            child.regrow = !def.boss() && (enemy.regrow || enemy.def.childMods().contains("regrow"));
            child.regrowTop = boss ? def.id() : enemy.regrowTop;
            child.freeLayers = Math.max(0, enemy.freeLayers - 1);
            child.spawnRound = enemy.spawnRound;
            aliveByRound.merge(child.spawnRound, 1, Integer::sum);
            child.hp = layerHp(def, child.fortified);
            child.speedFactor = enemy.speedFactor;
            place(child, enemy.distance, enemy.segment);
            refreshRbe(child);
            enemies.add(child);
            children.add(child);
        }
        return children;
    }

    // --- queries

    /** The living enemies, in the order they were created. */
    public List<SimEnemy> enemies() {
        compact();
        return java.util.Collections.unmodifiableList(enemies);
    }

    public int count() {
        compact();
        return enemies.size();
    }

    /** Total layers popped so far. */
    public int pops() {
        return pops;
    }

    /** Layers popped so far that paid a coin. */
    public int paidPops() {
        return paidPops;
    }

    /** Coins earned by pops since the last call (fractions are kept). */
    public double takeCoins() {
        double earned = coins;
        coins = 0;
        return earned;
    }

    /** Lives lost to leaks since the last call. */
    public int takeLeaked() {
        int lives = leaked;
        leaked = 0;
        return lives;
    }

    /** Sum of the remaining RBE of all enemies. */
    public double totalRbe() {
        compact();
        double sum = 0;
        for (SimEnemy enemy : enemies) {
            sum += enemy.rbe;
        }
        return sum;
    }

    /** The enemies within range of a point, best first by {@code order}, at most {@code limit}; camouflaged ones only if the tower detects them. */
    public List<SimEnemy> targets(double cx, double cy, double cz, double range, boolean detectsCamo, Comparator<SimEnemy> order, int limit) {
        compact();
        double rangeSqr = range * range;
        List<SimEnemy> found = new ArrayList<>();
        for (int i = 0, n = enemies.size(); i < n; i++) {
            SimEnemy enemy = enemies.get(i);
            if ((detectsCamo || !enemy.camo) && enemy.distanceSqr(cx, cy, cz) <= rangeSqr) {
                found.add(enemy);
            }
        }
        if (found.size() > 1) {
            found.sort(order);
        }
        return found.size() > limit ? new ArrayList<>(found.subList(0, limit)) : found;
    }
}
