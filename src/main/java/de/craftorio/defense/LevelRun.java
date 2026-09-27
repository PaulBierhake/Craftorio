package de.craftorio.defense;

import de.craftorio.registry.ModEntities;
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

    private final LevelPlan plan;
    private final List<Vec3> path;
    private final BlockPos core;
    private final List<TdEnemy> alive = new ArrayList<>();
    private final Set<Long> forcedChunks = new HashSet<>();
    private List<BlockPos> towers = List.of();
    private int towerRefreshIn;
    private int wave;
    private int spawnedInWave;
    private int cooldown;
    private int lives = LevelPlan.LIVES;
    private boolean finished;

    LevelRun(LevelPlan plan, List<Vec3> path, BlockPos core) {
        this.plan = plan;
        this.path = path;
        this.core = core;
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
        ChunkPos center = new ChunkPos(core);
        int radius = Math.floorDiv(TowerDefense.ZONE_RADIUS, 16) + 1;
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                long key = ChunkPos.asLong(x, z);
                if (!level.getForcedChunks().contains(key)) {
                    level.setChunkForced(x, z, true);
                    forcedChunks.add(key);
                }
            }
        }
    }

    Outcome tick(ServerLevel level) {
        if (--towerRefreshIn <= 0) {
            towerRefreshIn = TOWER_REFRESH;
            towers = TowerDefense.findTowers(level, core);
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
            enemy.start(this, path, plan.healthMultiplier());
            level.addFreshEntity(enemy);
            alive.add(enemy);
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
