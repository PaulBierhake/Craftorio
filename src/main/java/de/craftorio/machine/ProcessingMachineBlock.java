package de.craftorio.machine;

import com.mojang.serialization.MapCodec;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Electric furnace, press or assembler; runs on grid power. */
public final class ProcessingMachineBlock extends MachineBaseBlock {
    private final MachineType machineType;
    private final MapCodec<ProcessingMachineBlock> codec;

    public ProcessingMachineBlock(MachineType machineType, Properties properties) {
        super(properties);
        this.machineType = machineType;
        this.codec = simpleCodec(p -> new ProcessingMachineBlock(machineType, p));
    }

    public MachineType machineType() {
        return machineType;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProcessingMachineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.MACHINE.get(), ProcessingMachineBlockEntity::serverTick);
    }
}
