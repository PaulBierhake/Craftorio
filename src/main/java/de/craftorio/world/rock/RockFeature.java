package de.craftorio.world.rock;

import com.mojang.serialization.Codec;
import de.craftorio.registry.ModBlocks;
import de.craftorio.world.terrain.FactoryChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A boulder on flat ground: big (3 × 3 base, 2 high) or small (1–2 blocks). Only in the factory world, never near the
 * spawn, never on an edge or in water (every block of the footprint needs ground under it and air above).
 */
public final class RockFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SPAWN_FREE_RADIUS = 64;

    public RockFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!(level.getLevel().getChunkSource().getGenerator() instanceof FactoryChunkGenerator)) {
            return false;
        }
        if ((long) origin.getX() * origin.getX() + (long) origin.getZ() * origin.getZ() < (long) SPAWN_FREE_RADIUS * SPAWN_FREE_RADIUS) {
            return false;
        }
        boolean big = random.nextInt(3) == 0;
        int radius = big ? 1 : 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!free(level, origin.offset(dx, 0, dz))) {
                    return false;
                }
            }
        }
        if (big) {
            BlockState rock = ModBlocks.ROCK.get().defaultBlockState();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean corner = dx != 0 && dz != 0;
                    if (!corner || random.nextInt(5) < 3) {
                        set(level, origin.offset(dx, 0, dz), rock);
                    }
                    boolean middle = dx == 0 && dz == 0;
                    if (middle || (!corner && random.nextBoolean())) {
                        set(level, origin.offset(dx, 1, dz), rock);
                    }
                }
            }
        } else {
            BlockState rock = ModBlocks.SMALL_ROCK.get().defaultBlockState();
            set(level, origin, rock);
            BlockPos next = origin.relative(net.minecraft.core.Direction.Plane.HORIZONTAL.getRandomDirection(random));
            if (random.nextBoolean() && free(level, next)) {
                set(level, next, rock);
            }
        }
        return true;
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    /** Ground (earth, sand or snow) under the block and air in it. */
    private static boolean free(WorldGenLevel level, BlockPos pos) {
        BlockState ground = level.getBlockState(pos.below());
        boolean solid = ground.is(BlockTags.DIRT) || ground.is(BlockTags.SAND) || ground.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK);
        BlockState here = level.getBlockState(pos);
        return solid && (here.isAir() || here.canBeReplaced() && here.getFluidState().isEmpty());
    }
}
