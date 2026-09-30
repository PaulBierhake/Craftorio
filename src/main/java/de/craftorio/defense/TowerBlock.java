package de.craftorio.defense;

import com.mojang.serialization.MapCodec;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** A defense tower; may only be placed in the team's arena. Right-click opens its GUI (ammo, upgrades, target). */
public final class TowerBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 24, 14);

    private final TowerType towerType;
    private final MapCodec<TowerBlock> codec;

    public TowerBlock(TowerType towerType, Properties properties) {
        super(properties);
        this.towerType = towerType;
        this.codec = simpleCodec(p -> new TowerBlock(towerType, p));
    }

    public TowerType towerType() {
        return towerType;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    /** Towers stand in the team's arena, on open ground or on a plateau. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getLevel().isClientSide) {
            return defaultBlockState();
        }
        if (context.getPlayer() instanceof ServerPlayer player) {
            String error = TowerDefense.get(player.server).towerPlaceError(player, context.getClickedPos(), context.getItemInHand());
            if (error != null) {
                player.displayClientMessage(Component.translatable(error).withStyle(ChatFormatting.RED), true);
                return null;
            }
            return defaultBlockState();
        }
        return null;
    }

    /** A tower fresh from the factory is paid for with its base price in coins; one from the depot has been paid already. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(placer instanceof ServerPlayer player) || !(level.getBlockEntity(pos) instanceof TowerBlockEntity tower)
                || tower.paid() > 0) {
            return;
        }
        TowerDefense defense = TowerDefense.get(player.server);
        int slot = de.craftorio.defense.arena.Arenas.slotAt(pos);
        long price = defense.price(slot, towerType.def().cost());
        if (defense.spend(slot, price)) {
            tower.setPaid(price);
            player.displayClientMessage(Component.translatable("craftorio.tower.placed", price), true);
        } else {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            player.getInventory().placeItemBackInInventory(stack.copyWithCount(1));
            player.displayClientMessage(Component.translatable("craftorio.tower.place.no_coins").withStyle(ChatFormatting.RED), true);
        }
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
        return new TowerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.TOWER.get(), TowerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof TowerBlockEntity tower) {
            serverPlayer.openMenu(tower, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TowerBlockEntity tower) {
            tower.dropAmmo();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
