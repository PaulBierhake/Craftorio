package de.craftorio.defense;

import de.craftorio.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tower defense enemy. Walks the traced path from portal to core (no AI, no physics), attacks towers in reach
 * and costs lives when it gets through. Only towers can hurt it; it never attacks players or blocks.
 */
public class TdEnemy extends PathfinderMob implements HoglinBase {
    private final EnemyType enemyType;
    private @Nullable LevelRun run;
    private List<Vec3> path = List.of();
    private int pathIndex;
    private int attackCooldown;
    private int attackAnimation;

    public TdEnemy(EntityType<? extends TdEnemy> type, Level level) {
        super(type, level);
        this.enemyType = ModEntities.enemyType(type);
        this.noPhysics = true;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    public EnemyType enemyType() {
        return enemyType;
    }

    void start(LevelRun run, List<Vec3> path, double healthMultiplier) {
        this.run = run;
        this.path = path;
        this.pathIndex = 0;
        double health = enemyType.health() * healthMultiplier;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        setHealth((float) health);
        Vec3 start = path.get(0);
        moveTo(start.x, start.y, start.z, 0, 0);
    }

    /** How far along the path; towers shoot the enemy closest to the core first. */
    public double progress() {
        return pathIndex + (path.isEmpty() || pathIndex + 1 >= path.size() ? 0 : 1 - position().distanceTo(path.get(pathIndex + 1)));
    }

    @Override
    public void tick() {
        super.tick();
        if (attackAnimation > 0) {
            attackAnimation--;
        }
        if (level().isClientSide) {
            calculateEntityAnimation(false);
            return;
        }
        if (run == null || run.isFinished()) {
            discard(); // left over from an aborted level or a restart
            return;
        }
        boolean engaged = attack((ServerLevel) level());
        if (!engaged || !enemyType.stopsToAttack()) {
            walk();
        }
    }

    /** Hits the nearest tower in reach when the cooldown allows; returns whether a tower is in reach. */
    private boolean attack(ServerLevel level) {
        TowerBlockEntity tower = run.nearestTower(level, position(), enemyType.reach());
        if (tower == null) {
            return false;
        }
        if (--attackCooldown <= 0) {
            attackCooldown = enemyType.attackInterval();
            tower.damage(enemyType.damage());
            Vec3 target = tower.getBlockPos().getCenter();
            if (enemyType == EnemyType.SPITTER) {
                Vec3 from = position().add(0, 1.2, 0);
                for (int i = 0; i <= 8; i++) {
                    Vec3 point = from.lerp(target, i / 8.0);
                    level.sendParticles(ParticleTypes.SPIT, point.x, point.y, point.z, 1, 0, 0, 0, 0);
                }
            } else {
                level.broadcastEntityEvent(this, (byte) 4);
                attackAnimation = 10;
            }
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.x, target.y + 0.5, target.z, 3, 0.3, 0.3, 0.3, 0.1);
        }
        return true;
    }

    private void walk() {
        double remaining = enemyType.speed();
        Vec3 position = position();
        while (remaining > 0 && pathIndex + 1 < path.size()) {
            Vec3 target = path.get(pathIndex + 1);
            Vec3 delta = target.subtract(position);
            double distance = delta.length();
            if (distance <= remaining) {
                position = target;
                pathIndex++;
                remaining -= distance;
            } else {
                position = position.add(delta.scale(remaining / distance));
                remaining = 0;
            }
            if (delta.horizontalDistanceSqr() > 1e-6) {
                float yaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90F;
                setYRot(yaw);
                setYHeadRot(yaw);
                yBodyRot = yaw;
            }
        }
        setPos(position.x, position.y, position.z);
        if (pathIndex + 1 >= path.size()) {
            run.leak(this);
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Only towers (generic = ammunition, magic = energy weapons) and /kill may hurt enemies; players do not fight.
        if (source.is(DamageTypes.GENERIC)) {
            return super.hurt(source, (float) enemyType.damageTaken(amount, false));
        }
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            attackAnimation = 10;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public int getAttackAnimationRemainingTicks() {
        return attackAnimation;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }
}
