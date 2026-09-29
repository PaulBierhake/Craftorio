package de.craftorio.heat;

import com.mojang.serialization.MapCodec;
import de.craftorio.fluid.FluidConnector;
import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Turns water into hot steam with the heat of the reactor: heat connects on every side, water and steam through pipes. */
public final class HeatExchangerBlock extends MachineBaseBlock implements HeatNode.Connector, FluidConnector {
    public static final MapCodec<HeatExchangerBlock> CODEC = simpleCodec(HeatExchangerBlock::new);

    public HeatExchangerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean connectsHeat(BlockState state, Direction face) {
        return true;
    }

    @Override
    public boolean connectsFluid(BlockState state, Direction face) {
        return true;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeatExchangerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.HEAT_EXCHANGER.get(), HeatExchangerBlockEntity::serverTick);
    }

    /** No GUI: right-click shows the temperature and what the exchanger holds. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof HeatExchangerBlockEntity exchanger) {
            serverPlayer.displayClientMessage(Component.translatable("craftorio.heat_exchanger.status", Math.round(exchanger.temperature()),
                    exchanger.water().getFluidAmount(), exchanger.steam().getFluidAmount()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
