package de.craftorio.oil;

import com.mojang.serialization.MapCodec;
import de.craftorio.fluid.FluidConnector;
import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Stands on an {@link OilWellBlock} and pumps its crude oil into pipes and tanks next to it; needs power. */
public final class PumpjackBlock extends MachineBaseBlock implements FluidConnector {
    public static final MapCodec<PumpjackBlock> CODEC = simpleCodec(PumpjackBlock::new);

    public PumpjackBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** Only on a well. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!context.getLevel().getBlockState(context.getClickedPos().below()).is(ModBlocks.OIL_WELL.get())) {
            return null;
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public boolean connectsFluid(BlockState state, Direction face) {
        return face != Direction.DOWN;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PumpjackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.PUMPJACK.get(), PumpjackBlockEntity::serverTick);
    }
}
