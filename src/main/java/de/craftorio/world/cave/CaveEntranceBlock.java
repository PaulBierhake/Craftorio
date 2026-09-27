package de.craftorio.world.cave;

import com.mojang.serialization.MapCodec;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Cave entrance construction site: deliver materials (by hand or automation), then it drills down on grid power.
 * When finished it opens a shaft with scaffolding and unlocks the cave area around it.
 */
public final class CaveEntranceBlock extends BaseEntityBlock {
    public static final MapCodec<CaveEntranceBlock> CODEC = simpleCodec(CaveEntranceBlock::new);
    public static final int STAGE_MATERIALS = 0;
    public static final int STAGE_DRILLING = 1;
    public static final int STAGE_OPEN = 2;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 2);
    private static final VoxelShape FRAME = Block.box(0, 0, 0, 16, 2, 16);

    public CaveEntranceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, STAGE_MATERIALS));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return defaultBlockState();
        }
        BlockPos pos = context.getClickedPos();
        String error = null;
        if (level.dimension() != Level.OVERWORLD) {
            error = "craftorio.cave.error.overworld_only";
        } else if (pos.getY() < CaveLayers.SURFACE_BOTTOM) {
            error = "craftorio.cave.error.too_deep";
        } else if (!hasCaveLayer(level, pos)) {
            error = "craftorio.cave.error.no_layer";
        }
        if (error != null) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable(error).withStyle(ChatFormatting.RED), true);
            }
            return null;
        }
        return defaultBlockState();
    }

    /** Worlds created before M6 have no layers where their chunks were already generated. */
    private static boolean hasCaveLayer(Level level, BlockPos pos) {
        BlockPos probe = pos.atY(CaveLayers.CAP_ONE_BOTTOM - 1);
        BlockState state = level.getBlockState(probe);
        return state.is(ModBlocks.CAVE_RUBBLE.get()) || level.getBlockState(pos.atY(CaveLayers.CAP_ONE_BOTTOM)).is(ModBlocks.CAP_ROCK.get());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(STAGE) == STAGE_OPEN ? FRAME : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // The finished entrance can be walked through onto the scaffolding in the shaft.
        return state.getValue(STAGE) == STAGE_OPEN ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CaveEntranceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CAVE_ENTRANCE.get(), CaveEntranceBlockEntity::serverTick);
    }

    /** Right-click with a required material delivers it. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CaveEntranceBlockEntity site) || !site.needs(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            player.setItemInHand(hand, site.materials().insertItem(0, stack, false));
            player.displayClientMessage(site.status(), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CaveEntranceBlockEntity site) {
            player.displayClientMessage(site.status(), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
