package de.craftorio.logistics;

import com.mojang.serialization.MapCodec;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Item elevator: sends inserted items to the next elevator straight above or below (same X/Z, through any
 * blocks, up to {@link ElevatorBlockEntity#MAX_DISTANCE}). Receivers push arriving items out of their front.
 * Right-click cycles the mode.
 */
public final class ElevatorBlock extends BaseEntityBlock {
    public static final MapCodec<ElevatorBlock> CODEC = simpleCodec(ElevatorBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    public enum Mode implements StringRepresentable {
        RECEIVE, SEND_UP, SEND_DOWN;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }

        Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public ElevatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MODE, Mode.RECEIVE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElevatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ELEVATOR.get(), ElevatorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockState next = state.setValue(MODE, state.getValue(MODE).next());
            level.setBlock(pos, next, Block.UPDATE_ALL);
            Component target = ElevatorBlockEntity.findPartner(level, pos, next.getValue(MODE))
                    .map(partner -> Component.translatable("craftorio.elevator.target", Math.abs(partner.getY() - pos.getY())))
                    .orElse(next.getValue(MODE) == Mode.RECEIVE ? Component.empty()
                            : Component.translatable("craftorio.elevator.no_target").withStyle(ChatFormatting.RED));
            player.displayClientMessage(Component.translatable("craftorio.elevator.mode." + next.getValue(MODE).getSerializedName())
                    .append(" ").append(target), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ElevatorBlockEntity elevator) {
            elevator.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
