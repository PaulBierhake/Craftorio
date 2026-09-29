package de.craftorio.energy;

import com.mojang.serialization.MapCodec;
import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
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

/** Turns hot steam (from a heat exchanger or a pipe) into 5.82 MW for the power grid. */
public final class SteamTurbineBlock extends MachineBaseBlock implements de.craftorio.fluid.FluidConnector {
    public static final MapCodec<SteamTurbineBlock> CODEC = simpleCodec(SteamTurbineBlock::new);

    public SteamTurbineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean connectsFluid(BlockState state, net.minecraft.core.Direction face) {
        return true;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamTurbineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.STEAM_TURBINE.get(), SteamTurbineBlockEntity::serverTick);
    }

    /** No GUI: right-click tells why the turbine does not run. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof SteamTurbineBlockEntity turbine) {
            serverPlayer.displayClientMessage(Component.translatable("craftorio.steam_turbine.status." + turbine.status()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
