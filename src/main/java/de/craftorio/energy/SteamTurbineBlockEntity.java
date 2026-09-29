package de.craftorio.energy;

import de.craftorio.fluid.FluidBuffer;
import de.craftorio.heat.HeatLogic;
import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 5.82 MW of grid power for 60 hot steam per second (3 per tick). Like the steam engine it only uses steam while
 * something draws its power: the output buffer is two ticks of production.
 */
public final class SteamTurbineBlockEntity extends BlockEntity implements PowerSource {
    public static final int POWER = HeatLogic.TURBINE_POWER;
    public static final int STEAM_TANK = 100;

    private final EnergyBuffer energy = new EnergyBuffer(2 * POWER, 0, 2 * POWER, this::setChanged);
    private final FluidBuffer steam = new FluidBuffer(STEAM_TANK, FluidBuffer.only(ModFluids.HOT_STEAM.get()), this::setChanged);
    private int status = SteamEngineBlockEntity.NO_STEAM;

    public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_TURBINE.get(), pos, state);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public FluidBuffer steam() {
        return steam;
    }

    /** Same numbers as {@link SteamEngineBlockEntity}: running, no steam, or output buffer full. */
    public int status() {
        return status;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamTurbineBlockEntity turbine) {
        boolean running = false;
        if (turbine.energy.freeSpace() < POWER) {
            turbine.status = turbine.steam.isEmpty() ? SteamEngineBlockEntity.NO_STEAM : SteamEngineBlockEntity.BUFFER_FULL;
        } else if (turbine.steam.getFluidAmount() >= HeatLogic.TURBINE_STEAM_PER_TICK) {
            turbine.steam.use(HeatLogic.TURBINE_STEAM_PER_TICK);
            turbine.energy.generate(POWER);
            turbine.status = SteamEngineBlockEntity.RUNNING;
            running = true;
        } else {
            turbine.status = SteamEngineBlockEntity.NO_STEAM;
        }
        MachineBaseBlock.setActive(level, pos, state, running);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy.getEnergyStored());
        steam.writeToNBT(registries, tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("energy"));
        steam.readFromNBT(registries, tag);
    }
}
