package de.craftorio.defense;

import de.craftorio.defense.arena.ArenaLayout;
import de.craftorio.defense.arena.Mutator;
import de.craftorio.defense.sim.RoundDef;
import de.craftorio.defense.sim.TdPath;
import de.craftorio.defense.sim.TdSimulation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * One level in progress: sends out the rounds, tracks lives and runs the {@link TdSimulation} of the enemies. Not
 * persisted – a restart aborts it.
 */
public final class LevelRun {
    public enum Outcome {
        RUNNING, WON, LOST
    }

    /** All runs that are going on; towers look for their targets here. */
    private static final List<LevelRun> ACTIVE = new CopyOnWriteArrayList<>();

    /** Where the level is fought: arena slot, map and mutator. Null in tests that build their own track. */
    public record Arena(int slot, ArenaLayout layout, Mutator mutator) {
    }

    private final LevelPlan plan;
    private final List<Vec3> points;
    private final BlockPos core;
    private final @Nullable Arena arena;
    private final TdSimulation sim;
    private final Set<Long> forcedChunks = new HashSet<>();
    private @Nullable ServerLevel registeredIn;
    /** Index of the round that is running or comes next. */
    private int round;
    private boolean roundRunning;
    private List<RoundDef.Spawn> spawns = List.of();
    private int nextSpawn;
    private int roundTicks;
    private int cooldown;
    private int lives = LevelPlan.LIVES;
    private boolean finished;

    LevelRun(LevelPlan plan, List<Vec3> path, BlockPos core) {
        this(plan, path, core, null);
    }

    LevelRun(LevelPlan plan, List<Vec3> path, BlockPos core, @Nullable Arena arena) {
        this.plan = plan;
        this.points = List.copyOf(path);
        this.core = core;
        this.arena = arena;
        List<double[]> walk = new ArrayList<>(path.size());
        path.forEach(point -> walk.add(new double[]{point.x, point.y, point.z}));
        this.sim = new TdSimulation(new TdPath(walk));
        if (arena != null) {
            sim.setExternalSpeed(arena.mutator().speedFactor());
        }
    }

    public @Nullable Arena arena() {
        return arena;
    }

    public LevelPlan plan() {
        return plan;
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
        return Math.min(round + 1, plan.rounds().size());
    }

    public int lives() {
        return lives;
    }

    public int enemiesLeft() {
        int unspawned = 0;
        for (int r = round; r < plan.rounds().size(); r++) {
            unspawned += plan.rounds().get(r).enemyCount() - (r == round && roundRunning ? nextSpawn : 0);
        }
        return unspawned + sim.count();
    }

    public boolean isFinished() {
        return finished;
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
        lives = Math.max(0, lives - sim.takeLeaked());
        if (lives <= 0) {
            return Outcome.LOST;
        }
        return round >= plan.rounds().size() && !roundRunning && sim.count() == 0 ? Outcome.WON : Outcome.RUNNING;
    }

    /** Starts the next round when the rest is over and lets its enemies in one after the other. */
    private void tickRounds() {
        if (!roundRunning && round < plan.rounds().size()) {
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            RoundDef def = plan.rounds().get(round);
            sim.setRound(def.round());
            spawns = def.spawns();
            nextSpawn = 0;
            roundTicks = 0;
            roundRunning = true;
        }
        if (roundRunning) {
            while (nextSpawn < spawns.size() && spawns.get(nextSpawn).tick() <= roundTicks) {
                RoundDef.Group group = spawns.get(nextSpawn++).group();
                sim.spawn(group.enemy(), group.camo(), group.regrow(), group.fortified());
            }
            roundTicks++;
            if (nextSpawn >= spawns.size()) {
                roundRunning = false;
                round++;
                cooldown = LevelPlan.ROUND_DELAY;
            }
        }
    }

    /** Between two rounds the next one can be called early. */
    public boolean canCallWave() {
        return round > 0 && round < plan.rounds().size() && !roundRunning && cooldown > 0;
    }

    /** Starts the next round now; returns the ticks of waiting time skipped. */
    int callNextWave() {
        if (!canCallWave()) {
            return 0;
        }
        int skipped = cooldown;
        cooldown = 0;
        return skipped;
    }

    /** The round that spawns next (or is spawning now), from 0. */
    public int upcomingWave() {
        return round;
    }

    /** Removes remaining enemies and releases forced chunks. */
    void end(ServerLevel level) {
        finished = true;
        ACTIVE.remove(this);
        for (long key : forcedChunks) {
            level.setChunkForced(ChunkPos.getX(key), ChunkPos.getZ(key), false);
        }
        forcedChunks.clear();
    }
}
