package de.craftorio.economy.block;

import com.mojang.serialization.MapCodec;
import de.craftorio.economy.Credits;
import de.craftorio.economy.Economy;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Sells every sellable item inserted by hand, hopper or conveyor and credits the owning team. */
public final class TradingPostBlock extends BaseEntityBlock {
    public static final MapCodec<TradingPostBlock> CODEC = simpleCodec(TradingPostBlock::new);

    public TradingPostBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TradingPostBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof TradingPostBlockEntity post) {
            TeamData.registry(player.server).teamOf(player.getUUID()).ifPresent(team -> post.setOwner(team.id()));
        }
    }

    /** Right-click with an item sells the held stack. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!Economy.isSellable(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof TradingPostBlockEntity post) {
            claimIfUnowned(post, serverPlayer);
            long earned = post.sell(stack);
            if (earned > 0) {
                stack.setCount(0);
                serverPlayer.displayClientMessage(Component.translatable("craftorio.trading_post.sold", Credits.format(earned))
                        .withStyle(ChatFormatting.GOLD), true);
            }
        }
        return ItemInteractionResult.CONSUME;
    }

    /** Right-click with an empty hand (or an unsellable item) shows owner and turnover. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof TradingPostBlockEntity post) {
            claimIfUnowned(post, serverPlayer);
            String owner = post.owner()
                    .flatMap(id -> TeamData.registry(serverPlayer.server).team(id))
                    .map(Team::name)
                    .orElse("-");
            serverPlayer.sendSystemMessage(Component.translatable("craftorio.trading_post.info", owner, Credits.format(post.totalEarned())));
        }
        return InteractionResult.CONSUME;
    }

    /** Posts placed without an owner (e.g. by /setblock, or whose team was dissolved) belong to the first team that uses them. */
    private static void claimIfUnowned(TradingPostBlockEntity post, ServerPlayer player) {
        boolean ownerExists = post.owner().flatMap(id -> TeamData.registry(player.server).team(id)).isPresent();
        if (!ownerExists) {
            TeamData.registry(player.server).teamOf(player.getUUID()).ifPresent(team -> post.setOwner(team.id()));
        }
    }
}
