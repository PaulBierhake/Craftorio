package de.craftorio.heat;

import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds heat (1 MJ per °C in Factorio) and shares it with its neighbours every tick. */
public final class HeatPipeBlockEntity extends BlockEntity implements HeatNode {
    private double temperature = HeatLogic.AMBIENT;

    public HeatPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEAT_PIPE.get(), pos, state);
    }

    @Override
    public double temperature() {
        return temperature;
    }

    @Override
    public void setTemperature(double temperature) {
        this.temperature = temperature;
        setChanged();
    }

    @Override
    public double capacity() {
        return HeatLogic.PIPE_CAPACITY;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HeatPipeBlockEntity pipe) {
        HeatNode.conduct(level, pos, pipe);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("temperature", temperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("temperature") ? tag.getDouble("temperature") : HeatLogic.AMBIENT;
    }
}
