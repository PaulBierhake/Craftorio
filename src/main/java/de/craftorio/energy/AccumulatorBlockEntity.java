package de.craftorio.energy;

import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stores 5 MJ (100,000 FE) and charges and discharges at up to 300 kW, as in Factorio. */
public final class AccumulatorBlockEntity extends BlockEntity implements PowerStorage {
    public static final int CAPACITY = 5 * Energy.FE_PER_MJ;
    public static final int RATE = 300;

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, RATE, RATE, this::setChanged);

    public AccumulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ACCUMULATOR.get(), pos, state);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("energy"));
    }
}
