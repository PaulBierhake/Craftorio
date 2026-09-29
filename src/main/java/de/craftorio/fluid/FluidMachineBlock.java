package de.craftorio.fluid;

import com.mojang.serialization.MapCodec;
import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Chemical plant or oil refinery: a machine with a GUI whose fluids come and go through pipes on any side. */
public final class FluidMachineBlock extends MachineBaseBlock implements FluidConnector {
    private final FluidMachineType type;

    public FluidMachineBlock(FluidMachineType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public FluidMachineType machineType() {
        return type;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new FluidMachineBlock(type, properties));
    }

    @Override
    public boolean connectsFluid(BlockState state, Direction face) {
        return type.hasFluids();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidMachineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, ModBlockEntities.FLUID_MACHINE.get(), FluidMachineBlockEntity::serverTick);
    }
}
