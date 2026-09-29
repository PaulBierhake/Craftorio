package de.craftorio.fluid;

import com.mojang.serialization.MapCodec;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Half of a pipe tunnel: open at its front, joined through the ground to the second half up to {@link #REACH} blocks
 * away in line behind it. The second piece placed in line turns to face the other way by itself.
 */
public final class UndergroundPipeBlock extends BaseEntityBlock implements FluidConnector {
    public static final MapCodec<UndergroundPipeBlock> CODEC = simpleCodec(UndergroundPipeBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** Blocks between the two pieces at most (Factorio: 9 tiles apart, reach 10). */
    public static final int REACH = 9;

    public UndergroundPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public boolean connectsFluid(BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        // A lone piece in line with us that opens away from us is our other half: open the other way.
        for (Direction toward : Direction.Plane.HORIZONTAL) {
            BlockPos other = findPiece(level, pos, toward);
            if (other != null && level.getBlockState(other).getValue(FACING) == toward
                    && partnerOf(level, other) == null) {
                return defaultBlockState().setValue(FACING, toward.getOpposite());
            }
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** The first underground pipe within reach from {@code pos} in {@code direction}, or null. */
    static @Nullable BlockPos findPiece(net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction direction) {
        for (int i = 1; i <= REACH + 1; i++) {
            BlockPos candidate = pos.relative(direction, i);
            if (level.getBlockState(candidate).getBlock() instanceof UndergroundPipeBlock) {
                return candidate;
            }
        }
        return null;
    }

    /** The piece that {@code pos} is joined to (mutual: they face away from each other), or null. */
    public static @Nullable BlockPos partnerOf(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof UndergroundPipeBlock)) {
            return null;
        }
        Direction away = state.getValue(FACING).getOpposite();
        BlockPos other = findPiece(level, pos, away);
        if (other != null && level.getBlockState(other).getValue(FACING) == away) {
            return other;
        }
        return null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.FLUID_PIPE.get(), FluidPipeBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return FluidPipeBlockEntity.describe(level, pos, player);
    }
}
