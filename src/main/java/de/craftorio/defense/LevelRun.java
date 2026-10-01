package de.craftorio.defense;

import de.craftorio.defense.arena.ArenaLayout;
import de.craftorio.defense.arena.Mutator;
import de.craftorio.defense.sim.Buffs;
import de.craftorio.defense.sim.RoundDef;
import de.craftorio.defense.sim.RoundRules;
import de.craftorio.defense.sim.TdPath;
import de.craftorio.defense.sim.TdSimulation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * One level in progress: sends out the two rounds, tracks lives and coins and runs the {@link TdSimulation} of the
 * enemies. Not persisted – a restart aborts it.
 */
public final class LevelRun {
    public enum Outcome {
        RUNNING, WON, LOST
    }

    /** All runs that are going on; towers look for their targets here. */
    private static final List<LevelRun> ACTIVE = new CopyOnWriteArrayList<>();
    /** A tower outside an arena fights on a run whose path comes this close (blocks). */
    private static final double NEAR_PATH = 8;

    /** Where the level is fought: arena slot, map and mutator. Null in tests that build their own track. */
    public record Arena(int slot, ArenaLayout layout, Mutator mutator) {
    }

    /** A round that has started: its enemies enter one after the other. */
    private static final class Started {
        final RoundDef def;
        final List<RoundDef.Spawn> spawns;
        int next;
        int ticks;
        boolean paid;

        Started(RoundDef def) {
            this.def = def;
            this.spawns = def.spawns();
        }

        boolean spawning() {
            return next < spawns.size();
        }
    }

    private final LevelPlan plan;
    private final Difficulty difficulty;
    private final List<Vec3> points;
    private final BlockPos core;
    private final @Nullable Arena arena;
    private final TdSimulation sim;
    private final Set<Long> forcedChunks = new HashSet<>();
    private final List<Started> started = new ArrayList<>();
    /** Towers that fight on this run (they register while they tick); auras and abilities look them up. */
    private final Map<BlockPos, TowerBlockEntity> towers = new HashMap<>();
    private @Nullable ServerLevel registeredIn;
    /** Index of the next round to start. */
    private int nextRound;
    private int cooldown;
    /** The last started round has sent all its enemies and the rest before the next one is running. */
    private boolean resting;
    private int lives;
    private double coins;
    private boolean finished;

    LevelRun(LevelPlan plan, List<Vec3> path, BlockPos core) {
        this(plan, path, core, null, Difficulty.MEDIUM);
    }

    LevelRun(LevelPlan plan, List<Vec3> path, BlockPos core, @Nullable Arena arena, Difficulty difficulty) {
        this.plan = plan;
        this.difficulty = difficulty;
        this.points = List.copyOf(path);
        this.core = core;
        this.arena = arena;
        this.lives = difficulty.lives();
        List<double[]> walk = new ArrayList<>(path.size());
        path.forEach(point -> walk.add(new double[]{point.x, point.y, point.z}));
        this.sim = new TdSimulation(new TdPath(walk));
        sim.setExternalSpeed(difficulty.speedFactor() * (arena != null ? arena.mutator().speedFactor() : 1));
    }

    /** Coins from outside the simulation (supply drops, depots). */
    public void addCoins(double amount) {
        coins += amount;
    }

    public void register(TowerBlockEntity tower) {
        towers.put(tower.getBlockPos(), tower);
    }

    /** The towers of this run that are still standing. */
    public java.util.Collection<TowerBlockEntity> towers() {
        towers.values().removeIf(TowerBlockEntity::isRemoved);
        return towers.values();
    }

    /** What the auras of all towers on this run give {@code tower} (horizontal distance between block centres). */
    public Buffs aurasFor(TowerBlockEntity tower) {
        Buffs total = Buffs.NONE;
        for (TowerBlockEntity other : towers()) {
            for (var aura : other.profile().auras) {
                if (other == tower && !aura.self() || !aura.towers().isEmpty() && !aura.towers().equals(tower.type().defId())) {
                    continue;
                }
                double dx = other.getBlockPos().getX() - tower.getBlockPos().getX();
                double dz = other.getBlockPos().getZ() - tower.getBlockPos().getZ();
                if (aura.radius() < 0 || dx * dx + dz * dz <= aura.radius() * aura.radius()) {
                    total = total.merge(aura.buff());
                }
            }
        }
        return total;
    }

    public @Nullable Arena arena() {
        return arena;
    }

    public LevelPlan plan() {
        return plan;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    public TdSimulation simulation() {
        return sim;
    }

    /** The walk points of the path in world coordinates. */
    public List<Vec3> points() {
        return points;
    }

    /** The round of the level that is running or comes next, from 1. */
    public int wave() {
        return Math.min(nextRound + (spawningNow() ? 0 : 1), plan.rounds().size());
    }

    private boolean spawningNow() {
        return !started.isEmpty() && started.get(started.size() - 1).spawning();
    }

    public int lives() {
        return lives;
    }

    public int maxLives() {
        return difficulty.lives();
    }

    public int enemiesLeft() {
        int unspawned = 0;
        for (int r = nextRound; r < plan.rounds().size(); r++) {
            unspawned += plan.rounds().get(r).enemyCount();
        }
        for (Started round : started) {
            unspawned += round.spawns.size() - round.next;
        }
        return unspawned + sim.count();
    }

    public boolean isFinished() {
        return finished;
    }

    /** Coins earned since the last call: pops and round bonuses (with fractions). */
    double takeCoins() {
        double earned = coins + sim.takeCoins();
        coins = 0;
        return earned;
    }

    /** Makes the run known to the towers, without claiming chunks (tests with their own track). */
    void register(ServerLevel level) {
        registeredIn = level;
        ACTIVE.add(this);
    }

    /** The runs going on in a level. */
    static List<LevelRun> activeIn(ServerLevel level) {
        List<LevelRun> runs = new ArrayList<>(2);
        for (LevelRun run : ACTIVE) {
            if (run.registeredIn == level && !run.finished) {
                runs.add(run);
            }
        }
        return runs;
    }

    /** The run whose path a tower at this position fights on: in the arena its own slot's run, elsewhere the nearest one. */
    static @Nullable LevelRun runFor(ServerLevel level, BlockPos tower) {
        LevelRun best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LevelRun run : ACTIVE) {
            if (run.registeredIn != level || run.finished) {
                continue;
            }
            if (run.arena != null) {
                if (run.arena.slot() == de.craftorio.defense.arena.Arenas.slotAt(tower)) {
                    return run;
                }
                continue;
            }
            double distance = Double.MAX_VALUE;
            for (Vec3 point : run.points) {
                distance = Math.min(distance, point.distanceToSqr(tower.getX(), point.y, tower.getZ()));
            }
            // only a run whose path passes close by: other tests and arenas are far away
            if (distance < bestDistance && distance <= NEAR_PATH * NEAR_PATH) {
                bestDistance = distance;
                best = run;
            }
        }
        return best;
    }

    void begin(ServerLevel level) {
        register(level);
        ChunkPos min = new ChunkPos(towerAreaMin());
        ChunkPos max = new ChunkPos(towerAreaMax());
        for (int x = min.x; x <= max.x; x++) {
            for (int z = min.z; z <= max.z; z++) {
                long key = ChunkPos.asLong(x, z);
                if (!level.getForcedChunks().contains(key)) {
                    level.setChunkForced(x, z, true);
                    forcedChunks.add(key);
                }
            }
        }
    }

    private BlockPos towerAreaMin() {
        return arena != null ? TowerDefense.arenaMin(arena.slot()) : core.offset(-32, 0, -32);
    }

    private BlockPos towerAreaMax() {
        return arena != null ? TowerDefense.arenaMax(arena.slot()) : core.offset(32, 0, 32);
    }

    Outcome tick(ServerLevel level) {
        tickRounds();
        sim.tick();
        int leaked = sim.takeLeaked();
        lives = Math.max(0, lives - leaked);
        if (leaked > 0) {
            towers().forEach(TowerBlockEntity::lifeLost);
        }
        payRoundBonuses();
        if (lives <= 0) {
            return Outcome.LOST;
        }
        boolean allSent = nextRound >= plan.rounds().size() && !spawningNow();
        return allSent && sim.count() == 0 ? Outcome.WON : Outcome.RUNNING;
    }

    /** Starts the next round when the rest is over and lets the enemies of every started round in, one after the other. */
    private void tickRounds() {
        if (nextRound < plan.rounds().size() && !spawningNow()) {
            if (started.isEmpty()) {
                startNextRound();
            } else {
                if (!resting) { // the last round has just sent its final enemy: rest before the next one
                    resting = true;
                    cooldown = LevelPlan.ROUND_DELAY;
                }
                if (cooldown > 0) {
                    cooldown--;
                } else {
                    startNextRound();
                }
            }
        }
        for (Started round : started) {
            while (round.spawning() && round.spawns.get(round.next).tick() <= round.ticks) {
                RoundDef.Group group = round.spawns.get(round.next++).group();
                sim.setRound(round.def.round());
                sim.spawn(group.enemy(), group.camo(), group.regrow(), group.fortified());
            }
            round.ticks++;
        }
    }

    private void startNextRound() {
        started.add(new Started(plan.rounds().get(nextRound++)));
        resting = false;
        cooldown = 0;
    }

    /** The end of a round pays 100 plus its number once all its enemies are gone. */
    private void payRoundBonuses() {
        for (Started round : started) {
            if (!round.paid && !round.spawning() && sim.aliveOfRound(round.def.round()) == 0) {
                round.paid = true;
                coins += RoundRules.roundBonus(round.def.round());
                towers().forEach(tower -> tower.roundEnded(this));
            }
        }
    }

    /** The next round can be started at any time, even while the last one is still going. */
    public boolean canCallWave() {
        return nextRound < plan.rounds().size() && !finished;
    }

    /** Starts the next round now; returns the ticks of waiting time skipped. */
    int callNextWave() {
        if (!canCallWave()) {
            return 0;
        }
        int skipped = cooldown;
        startNextRound();
        return Math.max(1, skipped);
    }

    /** The round that spawns next (or is spawning now), from 0. */
    public int upcomingWave() {
        return nextRound;
    }

    /** Releases forced chunks; towers no longer see the run. */
    void end(ServerLevel level) {
        finished = true;
        ACTIVE.remove(this);
        for (long key : forcedChunks) {
            level.setChunkForced(ChunkPos.getX(key), ChunkPos.getZ(key), false);
        }
        forcedChunks.clear();
    }
}
