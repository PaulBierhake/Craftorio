package de.craftorio.energy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Buffers power for the grid; right-click shows the charge. */
public final class AccumulatorBlock extends BaseEntityBlock {
    public static final MapCodec<AccumulatorBlock> CODEC = simpleCodec(AccumulatorBlock::new);

    public AccumulatorBlock(Properties properties) {
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
        return new AccumulatorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof AccumulatorBlockEntity accumulator) {
            serverPlayer.displayClientMessage(Component.translatable("craftorio.accumulator.charge",
                    accumulator.energy().getEnergyStored() / Energy.FE_PER_KJ, AccumulatorBlockEntity.CAPACITY / Energy.FE_PER_KJ), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
