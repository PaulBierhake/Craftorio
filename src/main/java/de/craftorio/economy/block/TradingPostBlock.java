package de.craftorio.economy.block;

import com.mojang.serialization.MapCodec;
import de.craftorio.economy.Credits;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import de.craftorio.menu.TradingPostMenu;
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

/** Sells sellable items for the owning team: from hoppers and conveyors directly, by hand through the counter GUI. */
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

    /**
     * Right-click opens the counter: items put in are only sold with the "Sell" button, so nothing is sold by accident.
     * Hoppers and belts still sell directly.
     */
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
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new TradingPostMenu(id, inventory, post, owner),
                    state.getBlock().getName()), buf -> {
                buf.writeBlockPos(pos);
                buf.writeUtf(owner);
            });
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TradingPostBlockEntity post) {
            post.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** The GUI's "Sell" button. */
    public static void sellCounter(TradingPostBlockEntity post, ServerPlayer player) {
        claimIfUnowned(post, player);
        long earned = post.sellCounter();
        if (earned > 0) {
            player.displayClientMessage(Component.translatable("craftorio.trading_post.sold", Credits.format(earned))
                    .withStyle(ChatFormatting.GOLD), true);
        }
    }

    /** Posts placed without an owner (e.g. by /setblock, or whose team was dissolved) belong to the first team that uses them. */
    private static void claimIfUnowned(TradingPostBlockEntity post, ServerPlayer player) {
        boolean ownerExists = post.owner().flatMap(id -> TeamData.registry(player.server).team(id)).isPresent();
        if (!ownerExists) {
            TeamData.registry(player.server).teamOf(player.getUUID()).ifPresent(team -> post.setOwner(team.id()));
        }
    }
}
