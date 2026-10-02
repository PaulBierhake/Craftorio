package de.craftorio.world.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.IntBinaryOperator;

/**
 * The finishing touches after the vanilla decoration of a chunk (rules in {@link EdgeRules}): trees that stand on cliff
 * edges, on shores or around the spawn are removed, shores get sand, lake and river beds sand and gravel.
 */
final class FactoryFinish {
    /** A tree never has more blocks than this; the search stays near the chunk so the writes stay inside the generation region. */
    private static final int MAX_TREE_BLOCKS = 400;
    private static final int REACH = 8;

    private FactoryFinish() {
    }

    static void apply(WorldGenLevel level, ChunkAccess chunk) {
        IntBinaryOperator height = (x, z) -> level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        ChunkPos pos = chunk.getPos();
        for (int x = pos.getMinBlockX(); x <= pos.getMaxBlockX(); x++) {
            for (int z = pos.getMinBlockZ(); z <= pos.getMaxBlockZ(); z++) {
                int ground = height.applyAsInt(x, z);
                BlockPos top = new BlockPos(x, ground - 1, z);
                switch (EdgeRules.shore(height, x, z)) {
                    case BED -> bed(level, top);
                    case SHORE -> shore(level, top);
                    case NONE -> {
                    }
                }
                if (level.getBlockState(new BlockPos(x, ground, z)).is(BlockTags.LOGS) && EdgeRules.treeFree(height, x, z)) {
                    removeTree(level, new BlockPos(x, ground, z), pos);
                }
            }
        }
    }

    private static void bed(WorldGenLevel level, BlockPos top) {
        if (level.getBlockState(top).is(BlockTags.DIRT) || level.getBlockState(top).is(BlockTags.SAND)
                || level.getBlockState(top).is(Blocks.GRAVEL) || level.getBlockState(top).is(Blocks.STONE)) {
            level.setBlock(top, (EdgeRules.gravel(top.getX(), top.getZ()) ? Blocks.GRAVEL : Blocks.SAND).defaultBlockState(), 2);
        }
    }

    private static void shore(WorldGenLevel level, BlockPos top) {
        BlockState state = level.getBlockState(top);
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.COARSE_DIRT)) {
            level.setBlock(top, Blocks.SAND.defaultBlockState(), 2);
        }
    }

    /** Removes the connected logs and leaves of the tree whose trunk stands at {@code trunk}. */
    private static void removeTree(WorldGenLevel level, BlockPos trunk, ChunkPos chunk) {
        Set<BlockPos> tree = new HashSet<>();
        Queue<BlockPos> open = new ArrayDeque<>();
        tree.add(trunk);
        open.add(trunk);
        while (!open.isEmpty() && tree.size() < MAX_TREE_BLOCKS) {
            BlockPos at = open.remove();
            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                BlockPos next = at.relative(direction);
                if (next.getX() < chunk.getMinBlockX() - REACH || next.getX() > chunk.getMaxBlockX() + REACH
                        || next.getZ() < chunk.getMinBlockZ() - REACH || next.getZ() > chunk.getMaxBlockZ() + REACH) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if ((state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) && tree.add(next)) {
                    open.add(next);
                }
            }
        }
        for (BlockPos block : tree) {
            level.setBlock(block, Blocks.AIR.defaultBlockState(), 2);
        }
    }
}
