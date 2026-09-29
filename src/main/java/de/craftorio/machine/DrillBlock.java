package de.craftorio.machine;

import com.mojang.serialization.MapCodec;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Mines the ore field blocks below it and pushes the output into the block in front. The burner tier burns furnace
 * fuel, higher tiers run on grid power (see {@link DrillTier}).
 */
public final class DrillBlock extends BaseEntityBlock implements de.craftorio.fluid.FluidConnector {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final DrillTier tier;
    private final MapCodec<DrillBlock> codec;

    public DrillBlock(DrillTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        this.codec = simpleCodec(p -> new DrillBlock(tier, p));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    public DrillTier tier() {
        return tier;
    }

    /** Electric and deep drills take sulfuric acid through a pipe on any side but the front. */
    @Override
    public boolean connectsFluid(BlockState state, Direction face) {
        return !tier.usesFuel() && face != state.getValue(FACING);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        if (findOverlappingDrill(context.getLevel(), pos, tier) != null) {
            if (context.getPlayer() != null && !context.getLevel().isClientSide) {
                context.getPlayer().displayClientMessage(Component.translatable("craftorio.drill.overlap").withStyle(ChatFormatting.RED), true);
            }
            return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    /** Another drill whose mining area would share field blocks with a drill of {@code tier} at {@code pos}, or null. */
    public static @Nullable BlockPos findOverlappingDrill(Level level, BlockPos pos, DrillTier tier) {
        int reach = tier.radius() + DrillTier.MAX_RADIUS;
        for (BlockPos other : BlockPos.betweenClosed(pos.offset(-reach, 0, -reach), pos.offset(reach, 0, reach))) {
            if (!other.equals(pos) && level.getBlockState(other).getBlock() instanceof DrillBlock drill
                    && DrillArea.overlaps(pos.getX(), pos.getY(), pos.getZ(), tier.radius(),
                    other.getX(), other.getY(), other.getZ(), drill.tier().radius())) {
                return other.immutable();
            }
        }
        return null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DrillBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.DRILL.get(), DrillBlockEntity::serverTick);
    }

    /** Right-click opens the drill: fuel in, mined resources out – like a furnace. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof DrillBlockEntity drill) {
            serverPlayer.openMenu(drill, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DrillBlockEntity drill) {
            drill.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
