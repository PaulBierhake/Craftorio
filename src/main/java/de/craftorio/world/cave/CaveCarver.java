package de.craftorio.world.cave;

import de.craftorio.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Turns the sealed cave fill of an unlocked chunk into rock and open halls, and seeds cave ore fields. */
public final class CaveCarver {
    /** Chance per carved chunk to hold an ore field. */
    private static final double FIELD_CHANCE = 0.45;
    private static final int FIELD_MIN_SIZE = 20;
    private static final int FIELD_MAX_SIZE = 60;

    private CaveCarver() {
    }

    public static void carveChunk(Level level, ChunkPos chunk, CaveShape shape) {
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                carveColumn(level, x, z, shape);
            }
        }
        RandomSource random = RandomSource.create(chunk.toLong() ^ 0x5EED_CAFEL);
        if (random.nextDouble() < FIELD_CHANCE) {
            int x = chunk.getMiddleBlockX() + random.nextInt(9) - 4;
            int z = chunk.getMiddleBlockZ() + random.nextInt(9) - 4;
            Block field = randomField(random);
            placeField(level, new BlockPos(x, shape.floorY(x, z), z), field.defaultBlockState(),
                    FIELD_MIN_SIZE + random.nextInt(FIELD_MAX_SIZE - FIELD_MIN_SIZE + 1), random);
        }
    }

    /** Only cave fill is touched, so shafts and anything players built stay as they are. */
    public static void carveColumn(Level level, int x, int z, CaveShape shape) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int floor = shape.floorY(x, z);
        BlockState rubble = ModBlocks.CAVE_RUBBLE.get().defaultBlockState();
        for (int y = CaveLayers.CAVE_BOTTOM; y < CaveLayers.CAP_ONE_BOTTOM; y++) {
            pos.set(x, y, z);
            if (level.getBlockState(pos) != rubble) {
                continue;
            }
            BlockState target;
            if (shape.isOpen(x, y, z)) {
                target = Blocks.AIR.defaultBlockState();
            } else if (y == floor) {
                target = Blocks.TUFF.defaultBlockState();
            } else {
                target = y < floor ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
            }
            level.setBlock(pos, target, Block.UPDATE_CLIENTS);
        }
    }

    private static Block randomField(RandomSource random) {
        return switch (random.nextInt(5)) {
            case 0 -> ModBlocks.TIN_ORE_FIELD.get();
            case 1 -> ModBlocks.LEAD_ORE_FIELD.get();
            case 2 -> ModBlocks.SULFUR_FIELD.get();
            case 3 -> ModBlocks.GOLD_ORE_FIELD.get();
            default -> ModBlocks.QUARTZ_FIELD.get();
        };
    }

    /** Grows a field over the cave floor: each column's floor is the solid block with air above near the start height. */
    public static int placeField(Level level, BlockPos origin, BlockState state, int size, RandomSource random) {
        List<BlockPos> frontier = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        frontier.add(origin);
        visited.add(BlockPos.asLong(origin.getX(), 0, origin.getZ()));
        int placed = 0;
        while (placed < size && !frontier.isEmpty()) {
            BlockPos column = frontier.remove(random.nextInt(frontier.size()));
            BlockPos floor = findFloor(level, column);
            if (floor == null) {
                continue;
            }
            level.setBlock(floor, state, Block.UPDATE_CLIENTS);
            placed++;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = floor.relative(direction);
                if (Math.abs(next.getX() - origin.getX()) <= 10 && Math.abs(next.getZ() - origin.getZ()) <= 10
                        && visited.add(BlockPos.asLong(next.getX(), 0, next.getZ()))) {
                    frontier.add(next);
                }
            }
        }
        return placed;
    }

    private static BlockPos findFloor(Level level, BlockPos near) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos pos = near.atY(near.getY() + dy);
            if (!CaveLayers.inCaveLayer(pos.getY())) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isSolidRender(level, pos) && !state.is(ModBlocks.CAVE_RUBBLE.get()) && level.getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }
        return null;
    }
}
