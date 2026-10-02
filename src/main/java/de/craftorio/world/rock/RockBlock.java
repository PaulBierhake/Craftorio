package de.craftorio.world.rock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * A boulder of the factory world (the rocks of Factorio). Mined with a pickaxe, the whole boulder disappears and gives
 * stone (and coal for the big ones); without a pickaxe only the block breaks and gives nothing.
 */
public final class RockBlock extends Block {
    /** A boulder never has more blocks than this. */
    private static final int MAX_BLOCKS = 40;

    private final boolean big;

    public RockBlock(boolean big, Properties properties) {
        super(properties);
        this.big = big;
    }

    public boolean big() {
        return big;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (player.isCreative() || !willHarvest) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }
        if (!level.isClientSide) {
            for (BlockPos part : boulder(level, pos)) {
                level.setBlock(part, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            int stone = RockLoot.stone(big, level.random.nextInt(1_000));
            int coal = RockLoot.coal(big, level.random.nextInt(1_000));
            drop(level, pos, Items.COBBLESTONE, stone);
            drop(level, pos, Items.COAL, coal);
        }
        return true;
    }

    private static void drop(Level level, BlockPos pos, net.minecraft.world.item.Item item, int count) {
        while (count > 0) {
            int part = Math.min(count, 64);
            popResource(level, pos, new ItemStack(item, part));
            count -= part;
        }
    }

    /** The connected blocks of this boulder (rocks of the same kind that touch each other, diagonals included). */
    private Set<BlockPos> boulder(Level level, BlockPos start) {
        Set<BlockPos> found = new HashSet<>();
        Queue<BlockPos> open = new ArrayDeque<>();
        found.add(start);
        open.add(start);
        while (!open.isEmpty() && found.size() < MAX_BLOCKS) {
            BlockPos at = open.remove();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = at.offset(dx, dy, dz);
                        if (!found.contains(next) && level.getBlockState(next).is(this) && found.size() < MAX_BLOCKS) {
                            found.add(next);
                            open.add(next);
                        }
                    }
                }
            }
        }
        return found;
    }
}
