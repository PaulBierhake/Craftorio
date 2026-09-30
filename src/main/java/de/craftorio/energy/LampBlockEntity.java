package de.craftorio.energy;

import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Draws 5 kW from the grid and shines while it gets them. */
public final class LampBlockEntity extends BlockEntity {
    public static final int POWER = 5;

    private final EnergyBuffer energy = new EnergyBuffer(20 * POWER, 4 * POWER, 0, this::setChanged);

    public LampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAMP.get(), pos, state);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LampBlockEntity lamp) {
        boolean lit = lamp.energy.consume(POWER);
        if (state.getValue(LampBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(LampBlock.LIT, lit), Block.UPDATE_ALL);
        }
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
