package de.craftorio.defense;

import com.mojang.serialization.MapCodec;
import de.craftorio.economy.Credits;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** What remains of a destroyed tower. Right-click rebuilds it for credits, keeping its upgrade level. */
public final class TowerRuin extends BaseEntityBlock {
    public static final MapCodec<TowerRuin> CODEC = simpleCodec(TowerRuin::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 6, 15);

    public TowerRuin(Properties properties) {
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TowerRuinBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof TowerRuinBlockEntity ruin) {
            Team team = TeamData.registry(serverPlayer.server).ensureTeam(serverPlayer.getUUID(), serverPlayer.getGameProfile().getName());
            long cost = ruin.rebuildCost();
            if (!TeamData.maySpend(serverPlayer)) {
                return InteractionResult.sidedSuccess(false);
            }
            if (TeamData.registry(serverPlayer.server).withdraw(team.id(), cost)) {
                ruin.rebuild();
                player.displayClientMessage(Component.translatable("craftorio.tower.rebuilt", Credits.format(cost)).withStyle(ChatFormatting.GREEN), true);
            } else {
                player.displayClientMessage(Component.translatable("craftorio.td.repair.too_expensive", Credits.format(cost)).withStyle(ChatFormatting.RED), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
