package de.craftorio.world.cave;

import de.craftorio.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

/**
 * Dresses a freshly carved chunk of a cave or mine hall: dripstone, glow lichen and mushrooms for a little light, moss
 * and cobwebs, and in the mines timber supports. Deterministic per chunk; only air above plain rock floors is used, so
 * ore fields and oil wells stay as they are.
 */
public final class CaveDecorator {
    private CaveDecorator() {
    }

    /** Decorates the chunk and returns how many blocks were placed. */
    public static int decorate(Level level, Layer layer, ChunkPos chunk, CaveShape shape) {
        RandomSource random = RandomSource.create(chunk.toLong() ^ 0xDEC0_5EEDL ^ ((long) layer.ordinal() << 40));
        boolean mines = layer == Layer.MINES;
        int placed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                if (shape.isPillar(x, z)) {
                    continue;
                }
                int floor = shape.floorY(x, z);
                int ceiling = Math.min(shape.ceilingY(x, z), layer.maxY() + 1);
                if (ceiling - floor < 4 || !layer.contains(floor)) {
                    continue;
                }
                pos.set(x, floor, z);
                if (!plainRock(level.getBlockState(pos)) || !level.getBlockState(pos.above()).isAir() || nearField(level, pos)) {
                    continue;
                }
                double roll = random.nextDouble();
                BlockPos above = pos.above();
                BlockState decoration = null;
                BlockPos at = above;
                if (roll < 0.012) {
                    decoration = dripstone(Direction.UP);
                } else if (roll < 0.020) {
                    decoration = (random.nextBoolean() ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM).defaultBlockState();
                } else if (roll < 0.045) {
                    decoration = Blocks.MOSS_CARPET.defaultBlockState();
                } else if (roll < 0.050 && !mines) {
                    decoration = Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP);
                } else if (mines && roll < 0.056 && ceiling - floor >= 4) {
                    // A timber support from the floor to the ceiling.
                    int top = Math.min(ceiling - 1, floor + 7);
                    boolean free = true;
                    for (int y = floor + 1; y <= top && free; y++) {
                        free = level.getBlockState(pos.set(x, y, z)).isAir();
                    }
                    if (free) {
                        for (int y = floor + 1; y <= top; y++) {
                            level.setBlock(pos.set(x, y, z), Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), Block.UPDATE_CLIENTS);
                            placed++;
                        }
                        pos.set(x, top + 1, z);
                        continue;
                    }
                }
                if (decoration != null && level.getBlockState(at).isAir()) {
                    level.setBlock(at, decoration, Block.UPDATE_CLIENTS);
                    placed++;
                }
                // The ceiling: stalactites, glow lichen, cobwebs.
                double ceilingRoll = random.nextDouble();
                BlockPos under = pos.set(x, ceiling - 1, z).immutable();
                BlockPos ceilingBlock = under.above();
                if (level.getBlockState(under).isAir() && !level.getBlockState(ceilingBlock).isAir() && layer.contains(under.getY())) {
                    BlockState ceilingDecoration = null;
                    if (ceilingRoll < 0.012) {
                        ceilingDecoration = dripstone(Direction.DOWN);
                    } else if (ceilingRoll < 0.030) {
                        ceilingDecoration = Blocks.GLOW_LICHEN.defaultBlockState().setValue(GlowLichenBlock.getFaceProperty(Direction.UP), true);
                    } else if (ceilingRoll < 0.036) {
                        ceilingDecoration = Blocks.COBWEB.defaultBlockState();
                    }
                    if (ceilingDecoration != null) {
                        level.setBlock(under, ceilingDecoration, Block.UPDATE_CLIENTS);
                        placed++;
                    }
                }
            }
        }
        return placed;
    }

    private static BlockState dripstone(Direction direction) {
        return Blocks.POINTED_DRIPSTONE.defaultBlockState()
                .setValue(PointedDripstoneBlock.TIP_DIRECTION, direction)
                .setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP);
    }

    private static boolean plainRock(BlockState state) {
        return state.is(Blocks.TUFF) || state.is(Blocks.SMOOTH_BASALT) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.STONE)
                || state.is(Blocks.BLACKSTONE);
    }

    /** Ore fields and oil wells and their surroundings stay free. */
    private static boolean nearField(Level level, BlockPos pos) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockState state = level.getBlockState(pos.offset(dx, 0, dz));
                if (state.is(ModBlocks.OIL_WELL.get()) || state.is(ModBlocks.URANIUM_ORE_FIELD.get())) {
                    return true;
                }
            }
        }
        return false;
    }
}
