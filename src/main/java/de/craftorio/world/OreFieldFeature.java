package de.craftorio.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Grows a patch of ore field blocks across the terrain surface. */
public final class OreFieldFeature extends Feature<OreFieldConfiguration> {
    public static final int MAX_SIZE = 256;
    /** Fields reach their maximum size this far from the world origin. */
    public static final int FULL_SIZE_DISTANCE = 1500;
    // Worldgen may only write about one chunk beyond the one being decorated.
    private static final int MAX_RADIUS = 12;

    public OreFieldFeature() {
        super(OreFieldConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<OreFieldConfiguration> context) {
        OreFieldConfiguration config = context.config();
        BlockPos origin = context.origin();
        int size = sizeFor(origin, config.minSize(), config.maxSize(), context.random());
        return placeField(context.level(), origin, config.state(), size, context.random()) > 0;
    }

    public static int sizeFor(BlockPos origin, int minSize, int maxSize, RandomSource random) {
        double distance = Math.sqrt((double) origin.getX() * origin.getX() + (double) origin.getZ() * origin.getZ());
        double t = Math.min(1.0, distance / FULL_SIZE_DISTANCE);
        double size = Mth.lerp(t, minSize, maxSize) * (0.8 + random.nextDouble() * 0.4);
        return Mth.clamp((int) size, minSize, maxSize);
    }

    /** Converts up to {@code size} surface blocks around {@code origin}; returns how many were placed. */
    public static int placeField(WorldGenLevel level, BlockPos origin, BlockState state, int size, RandomSource random) {
        List<BlockPos> frontier = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        frontier.add(new BlockPos(origin.getX(), 0, origin.getZ()));
        visited.add(columnKey(origin.getX(), origin.getZ()));
        int placed = 0;
        while (placed < size && !frontier.isEmpty()) {
            BlockPos column = frontier.remove(random.nextInt(frontier.size()));
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ()) - 1;
            BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
            if (!canReplace(level.getBlockState(pos))) {
                continue;
            }
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            BlockState above = level.getBlockState(pos.above());
            if (!above.isAir() && above.canBeReplaced() && above.getFluidState().isEmpty()) {
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            placed++;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = column.relative(direction);
                if (Math.abs(next.getX() - origin.getX()) <= MAX_RADIUS && Math.abs(next.getZ() - origin.getZ()) <= MAX_RADIUS
                        && visited.add(columnKey(next.getX(), next.getZ()))) {
                    frontier.add(next);
                }
            }
        }
        return placed;
    }

    private static boolean canReplace(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK);
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
