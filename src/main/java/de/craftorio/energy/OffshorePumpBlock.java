package de.craftorio.energy;

import com.mojang.serialization.MapCodec;
import de.craftorio.fluid.FluidConnector;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Pumps water from a water block next to or below it into the pipes and machines touching it (1200 water/s, as in
 * Factorio). Only works at the shore.
 */
public final class OffshorePumpBlock extends BaseEntityBlock implements FluidConnector {
    public static final MapCodec<OffshorePumpBlock> CODEC = simpleCodec(OffshorePumpBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

    public OffshorePumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean connectsFluid(BlockState state, Direction face) {
        return true;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OffshorePumpBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.OFFSHORE_PUMP.get(), OffshorePumpBlockEntity::serverTick);
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
