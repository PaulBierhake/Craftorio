package de.craftorio.defense;

import de.craftorio.defense.arena.ArenaLayout;
import de.craftorio.defense.arena.ArenaTheme;
import de.craftorio.defense.arena.Arenas;
import de.craftorio.defense.arena.Mutator;
import de.craftorio.defense.arena.Tile;
import de.craftorio.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** One level in progress: spawns the waves, tracks lives and living enemies. Not persisted – a restart aborts it. */
public final class LevelRun {
    public enum Outcome {
        RUNNING, WON, LOST
    }

    private static final int TOWER_REFRESH = 100;
    private static final int VENT_INTERVAL = 60;

    /** Where the level is fought: arena slot, map and mutator. Null in tests that build their own track. */
    public record Arena(int slot, ArenaLayout layout, Mutator mutator) {
    }

    private final LevelPlan plan;
    private final List<Vec3> path;
    private final BlockPos core;
    private final @Nullable Arena arena;
    private final List<TdEnemy> alive = new ArrayList<>();
    private final Set<Long> forcedChunks = new HashSet<>();
    private List<BlockPos> towers = List.of();
    private int towerRefreshIn;
    private int wave;
    private int spawnedInWave;
    private int cooldown;
    private int lives = LevelPlan.LIVES;
    private boolean finished;
    private int ventIn = VENT_INTERVAL;

    LevelRun(LevelPlan plan, List<Vec3> path, BlockPos core) {
        this(plan, path, core, null);
    }

    LevelRun(LevelPlan plan, List<Vec3> path, BlockPos core, @Nullable Arena arena) {
        this.plan = plan;
        this.path = path;
        this.core = core;
        this.arena = arena;
    }

    public @Nullable Arena arena() {
        return arena;
    }

    public LevelPlan plan() {
        return plan;
    }

    public int wave() {
        return Math.min(wave + 1, plan.waves().size());
    }

    public int lives() {
        return lives;
    }

    public int enemiesLeft() {
        int unspawned = 0;
        for (int w = wave; w < plan.waves().size(); w++) {
            unspawned += plan.waves().get(w).size() - (w == wave ? spawnedInWave : 0);
        }
        return unspawned + alive.size();
    }

    public boolean isFinished() {
        return finished;
    }

    void begin(ServerLevel level) {
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
        if (--towerRefreshIn <= 0) {
            towerRefreshIn = TOWER_REFRESH;
            towers = TowerDefense.findTowers(level, towerAreaMin(), towerAreaMax());
        }
        if (arena != null && arena.layout().theme() == ArenaTheme.FIRE && --ventIn <= 0) {
            ventIn = VENT_INTERVAL;
            erupt(level);
        }
        if (wave < plan.waves().size()) {
            if (cooldown > 0) {
                cooldown--;
            } else {
                List<EnemyType> enemies = plan.waves().get(wave);
                spawn(level, enemies.get(spawnedInWave++));
                cooldown = LevelPlan.SPAWN_INTERVAL;
                if (spawnedInWave >= enemies.size()) {
                    wave++;
                    spawnedInWave = 0;
                    cooldown = LevelPlan.WAVE_DELAY;
                }
            }
        }
        alive.removeIf(TdEnemy::isRemoved);
        if (lives <= 0) {
            return Outcome.LOST;
        }
        return wave >= plan.waves().size() && alive.isEmpty() ? Outcome.WON : Outcome.RUNNING;
    }

    private void spawn(ServerLevel level, EnemyType type) {
        TdEnemy enemy = ModEntities.entityType(type).create(level);
        if (enemy != null) {
            enemy.start(this, path, plan.healthMultiplier() * (arena != null ? arena.mutator().healthFactor() : 1));
            level.addFreshEntity(enemy);
            alive.add(enemy);
        }
    }

    /** The brood the swarm queen calls when she enters a new phase; they join at her place of the path. */
    void summon(ServerLevel level, TdEnemy queen, int phase) {
        List<EnemyType> brood = new ArrayList<>();
        int crawlers = 8 + 4 * (phase - 1);
        for (int i = 0; i < crawlers; i++) {
            brood.add(EnemyType.CRAWLER);
        }
        if (phase >= 2) {
            for (int i = 0; i < 4; i++) {
                brood.add(EnemyType.BREAKER);
            }
        }
        if (phase >= 3) {
            brood.add(EnemyType.BEHEMOTH);
            brood.add(EnemyType.BEHEMOTH);
            for (int i = 0; i < 4; i++) {
                brood.add(EnemyType.SPITTER);
            }
        }
        double health = plan.healthMultiplier() * (arena != null ? arena.mutator().healthFactor() : 1);
        for (EnemyType type : brood) {
            TdEnemy enemy = ModEntities.entityType(type).create(level);
            if (enemy != null) {
                enemy.startAt(this, path, queen.pathIndex(), queen.position(), health);
                level.addFreshEntity(enemy);
                alive.add(enemy);
            }
        }
    }

    /** The wave that spawns next (or is spawning now). */
    public int upcomingWave() {
        return wave;
    }

    /** Between two waves the next one can be called early. */
    public boolean canCallWave() {
        return wave > 0 && wave < plan.waves().size() && spawnedInWave == 0 && cooldown > 0;
    }

    /** Starts the next wave now; returns the ticks of waiting time skipped. */
    int callNextWave() {
        if (!canCallWave()) {
            return 0;
        }
        int skipped = cooldown;
        cooldown = 0;
        return skipped;
    }

    // --- terrain of the arena map

    private @Nullable Tile tileAt(Vec3 position) {
        if (arena == null) {
            return null;
        }
        int[] tile = Arenas.tileAt(BlockPos.containing(position));
        return tile == null ? null : arena.layout().tile(tile[0], tile[1]);
    }

    /** Shallow water and scree slow enemies down; the haste mutator speeds them up. */
    double speedFactor(Vec3 position) {
        if (arena == null) {
            return 1;
        }
        double factor = arena.mutator().speedFactor();
        if (tileAt(position) == Tile.ROUGH) {
            factor *= switch (arena.layout().theme()) {
                case WATER -> 0.55;
                case MOUNTAIN -> 0.8;
                default -> 1.0;
            };
        }
        return factor;
    }

    /** In the forest thicket enemies are hidden until towers are close. */
    boolean camouflaged(Vec3 position) {
        return arena != null && arena.layout().theme() == ArenaTheme.FOREST && tileAt(position) == Tile.ROUGH;
    }

    /** Lava vents burn every enemy walking over them. */
    private void erupt(ServerLevel level) {
        float damage = 6 + plan.level();
        for (TdEnemy enemy : alive) {
            if (!enemy.isRemoved() && tileAt(enemy.position()) == Tile.ROUGH) {
                enemy.invulnerableTime = 0;
                enemy.hurt(level.damageSources().magic(), damage);
                level.sendParticles(ParticleTypes.LAVA, enemy.getX(), enemy.getY() + 0.3, enemy.getZ(), 6, 0.3, 0.2, 0.3, 0);
                level.sendParticles(ParticleTypes.FLAME, enemy.getX(), enemy.getY() + 0.5, enemy.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
            }
        }
    }

    void leak(TdEnemy enemy) {
        lives = Math.max(0, lives - enemy.enemyType().leakCost());
    }

    /** Removes remaining enemies and releases forced chunks. */
    void end(ServerLevel level) {
        finished = true;
        alive.forEach(TdEnemy::discard);
        alive.clear();
        for (long key : forcedChunks) {
            level.setChunkForced(ChunkPos.getX(key), ChunkPos.getZ(key), false);
        }
        forcedChunks.clear();
    }

    @Nullable
    TowerBlockEntity nearestTower(ServerLevel level, Vec3 from, double reach) {
        TowerBlockEntity nearest = null;
        double best = reach * reach;
        for (BlockPos pos : towers) {
            double distance = pos.getCenter().distanceToSqr(from);
            if (distance <= best) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof TowerBlockEntity tower) {
                    nearest = tower;
                    best = distance;
                }
            }
        }
        return nearest;
    }
}
