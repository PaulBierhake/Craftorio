package de.craftorio.energy;

import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Produces up to {@link SolarLogic#PEAK_KW} kW of grid power from daylight; needs open sky. The output buffer is two ticks of production. */
public final class SolarPanelBlockEntity extends BlockEntity implements PowerSource {
    private final EnergyBuffer energy = new EnergyBuffer(2 * SolarLogic.PEAK_KW, 0, 2 * SolarLogic.PEAK_KW, this::setChanged);
    private int output;
    private boolean sky;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL.get(), pos, state);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    /** Power in kW at the moment. */
    public int output() {
        return output;
    }

    /** False if the panel is shaded or in a dimension without a sky. */
    public boolean seesSky() {
        return sky;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity panel) {
        panel.sky = level.dimensionType().hasSkyLight() && level.canSeeSky(pos.above());
        panel.output = panel.sky ? SolarLogic.output(level.getDayTime(), SolarLogic.PEAK_KW) : 0;
        if (panel.output > 0 && panel.energy.freeSpace() >= panel.output) {
            panel.energy.generate(panel.output);
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
