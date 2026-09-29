package de.craftorio.energy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pumps water from a water block next to or below it into the boilers touching it (1200 water/s in Factorio, far more
 * than anything needs until the fluid network of a later package). Only works at the shore.
 */
public final class OffshorePumpBlock extends Block {
    public static final MapCodec<OffshorePumpBlock> CODEC = simpleCodec(OffshorePumpBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

    public OffshorePumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Is there a water source next to the pump (sides or below)? */
    public static boolean hasWater(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (direction != Direction.UP) {
                var fluid = level.getFluidState(pos.relative(direction));
                if (fluid.is(FluidTags.WATER) && fluid.isSource()) {
                    return true;
                }
            }
        }
        return false;
    }
}
