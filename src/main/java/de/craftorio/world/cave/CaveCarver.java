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

/** Turns the sealed fill of an unlocked chunk into rock and open halls, and seeds the layer's ore fields. */
public final class CaveCarver {
    /** Chance per carved chunk to hold an ore field. */
    private static final double FIELD_CHANCE = 0.45;
    private static final int FIELD_MIN_SIZE = 20;
    private static final int FIELD_MAX_SIZE = 60;

    private CaveCarver() {
    }

    public static void carveChunk(Level level, Layer layer, ChunkPos chunk, CaveShape shape) {
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                carveColumn(level, layer, x, z, shape);
            }
        }
        RandomSource random = RandomSource.create(chunk.toLong() ^ 0x5EED_CAFEL ^ layer.ordinal());
        if (random.nextDouble() < FIELD_CHANCE) {
            int x = chunk.getMiddleBlockX() + random.nextInt(9) - 4;
            int z = chunk.getMiddleBlockZ() + random.nextInt(9) - 4;
            Block field = randomField(layer, random);
            placeField(level, layer, new BlockPos(x, shape.floorY(x, z), z), field.defaultBlockState(),
                    FIELD_MIN_SIZE + random.nextInt(FIELD_MAX_SIZE - FIELD_MIN_SIZE + 1), random);
        }
    }

    public static Block fill(Layer layer) {
        return layer == Layer.CAVES ? ModBlocks.CAVE_RUBBLE.get() : ModBlocks.MINE_RUBBLE.get();
    }

    /** Only the layer's fill is touched, so shafts and anything players built stay as they are. */
    public static void carveColumn(Level level, Layer layer, int x, int z, CaveShape shape) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int floor = shape.floorY(x, z);
        BlockState rubble = fill(layer).defaultBlockState();
        boolean mines = layer == Layer.MINES;
        for (int y = layer.minY(); y <= layer.maxY(); y++) {
            pos.set(x, y, z);
            if (level.getBlockState(pos) != rubble) {
                continue;
            }
            BlockState target;
            if (shape.isOpen(x, y, z)) {
                target = Blocks.AIR.defaultBlockState();
            } else if (y == floor) {
                target = mines ? Blocks.SMOOTH_BASALT.defaultBlockState() : Blocks.TUFF.defaultBlockState();
            } else if (y < floor) {
                target = mines ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState();
            } else {
                target = mines ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
            }
            level.setBlock(pos, target, Block.UPDATE_CLIENTS);
        }
    }

    private static Block randomField(Layer layer, RandomSource random) {
        if (layer == Layer.MINES) {
            return switch (random.nextInt(4)) {
                case 0 -> ModBlocks.DIAMOND_FIELD.get();
                case 1 -> ModBlocks.TITANIUM_ORE_FIELD.get();
                case 2 -> ModBlocks.URANIUM_ORE_FIELD.get();
                default -> ModBlocks.CRYSTAL_FIELD.get();
            };
        }
        return switch (random.nextInt(5)) {
            case 0 -> ModBlocks.TIN_ORE_FIELD.get();
            case 1 -> ModBlocks.LEAD_ORE_FIELD.get();
            case 2 -> ModBlocks.SULFUR_FIELD.get();
            case 3 -> ModBlocks.GOLD_ORE_FIELD.get();
            default -> ModBlocks.QUARTZ_FIELD.get();
        };
    }

    /** Grows a field over the cave floor: each column's floor is the solid block with air above near the start height. */
    public static int placeField(Level level, Layer layer, BlockPos origin, BlockState state, int size, RandomSource random) {
        List<BlockPos> frontier = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        frontier.add(origin);
        visited.add(BlockPos.asLong(origin.getX(), 0, origin.getZ()));
        int placed = 0;
        while (placed < size && !frontier.isEmpty()) {
            BlockPos column = frontier.remove(random.nextInt(frontier.size()));
            BlockPos floor = findFloor(level, layer, column);
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

    private static BlockPos findFloor(Level level, Layer layer, BlockPos near) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos pos = near.atY(near.getY() + dy);
            if (!layer.contains(pos.getY())) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isSolidRender(level, pos) && !state.is(fill(layer)) && level.getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }
        return null;
    }
}
