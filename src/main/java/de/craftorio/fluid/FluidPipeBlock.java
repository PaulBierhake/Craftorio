package de.craftorio.fluid;

import com.mojang.serialization.MapCodec;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A pipe: draws an arm to every neighbouring fluid block and lets fluid flow through those connections
 * (see {@link FluidPipeBlockEntity}). Right-click shows what it holds.
 */
public final class FluidPipeBlock extends BaseEntityBlock implements FluidConnector {
    public static final MapCodec<FluidPipeBlock> CODEC = simpleCodec(FluidPipeBlock::new);
    public static final int CAPACITY = 100;
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape[] ARMS = {
            Block.box(5, 0, 5, 11, 5, 11), Block.box(5, 11, 5, 11, 16, 11), Block.box(5, 5, 0, 11, 11, 5),
            Block.box(5, 5, 11, 11, 11, 16), Block.box(0, 5, 5, 5, 11, 11), Block.box(11, 5, 5, 16, 11, 11)};

    public FluidPipeBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (Direction direction : Direction.values()) {
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        PipeBlock.PROPERTY_BY_DIRECTION.values().forEach(builder::add);
    }

    @Override
    public boolean connectsFluid(BlockState state, Direction face) {
        return true;
    }

    /** Does the block at {@code pos + side} want a pipe connection at its face towards us? */
    public static boolean connectsTo(BlockGetter level, BlockPos pos, Direction side) {
        BlockState other = level.getBlockState(pos.relative(side));
        return other.getBlock() instanceof FluidConnector connector && connector.connectsFluid(other, side.getOpposite());
    }

    private BlockState withConnections(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), connectsTo(level, pos, direction));
        }
        return state;
    }

    /** The state a pipe gets when placed at {@code pos}: an arm to every neighbour that connects. */
    public BlockState connectedState(BlockGetter level, BlockPos pos) {
        return withConnections(level, pos, defaultBlockState());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        boolean connected = neighborState.getBlock() instanceof FluidConnector connector
                && connector.connectsFluid(neighborState, direction.getOpposite());
        return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), connected);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction direction : Direction.values()) {
            if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                shape = Shapes.or(shape, ARMS[direction.ordinal()]);
            }
        }
        return shape;
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
