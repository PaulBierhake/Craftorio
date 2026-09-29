package de.craftorio.logistics;

import com.mojang.serialization.MapCodec;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Moves single items from the block behind it to the block in front ({@link #FACING}). */
public final class InserterBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 11, 13);

    private final InserterType type;
    private final MapCodec<InserterBlock> codec;

    public InserterBlock(InserterType type, Properties properties) {
        super(properties);
        this.type = type;
        this.codec = simpleCodec(p -> new InserterBlock(type, p));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public InserterType type() {
        return type;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    /** A filter inserter takes its filter from the item in hand; empty hand shows it, sneaking clears it. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (type.hasFilter() && !stack.isEmpty() && level.getBlockEntity(pos) instanceof InserterBlockEntity inserter) {
            if (!level.isClientSide) {
                inserter.setFilter(stack);
                player.displayClientMessage(Component.translatable("craftorio.inserter.filter_set", stack.getHoverName()), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (type.hasFilter() && level.getBlockEntity(pos) instanceof InserterBlockEntity inserter) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    inserter.setFilter(ItemStack.EMPTY);
                    player.displayClientMessage(Component.translatable("craftorio.inserter.filter_cleared"), true);
                } else {
                    player.displayClientMessage(inserter.filter().isEmpty() ? Component.translatable("craftorio.inserter.no_filter")
                            : Component.translatable("craftorio.inserter.filter_set", inserter.filter().getHoverName()), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InserterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.INSERTER.get(), InserterBlockEntity::serverTick);
    }
}
