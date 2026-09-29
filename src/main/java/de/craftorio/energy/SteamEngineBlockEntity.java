package de.craftorio.energy;

import de.craftorio.fluid.FluidBuffer;
import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.registry.ModFluids;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 900 kW of grid power for 30 steam per second (1.5 per tick); the output buffer is two ticks of production, so the
 * engine only uses steam while something draws its power.
 */
public final class SteamEngineBlockEntity extends BlockEntity implements PowerSource {
    public static final int POWER = Energy.STEAM_ENGINE_KW;

    /** Steam a running engine uses per tick, times two (it alternates between one and two units). */
    public static final int STEAM_PER_TICK_X2 = 3;
    public static final int STEAM_TANK = 100;

    /** Why the engine runs or not, for the status message. */
    public static final int RUNNING = 0;
    /** Unused since steam comes through the fluid network; kept so the status numbers stay the same. */
    public static final int NO_BOILER = 1;
    public static final int NO_STEAM = 2;
    /** Steam is there, but the output buffer is full: nothing draws power from the engine (no pole or no consumer). */
    public static final int BUFFER_FULL = 3;

    private final EnergyBuffer energy = new EnergyBuffer(2 * POWER, 0, 2 * POWER, this::setChanged);
    private final FluidBuffer steam = new FluidBuffer(STEAM_TANK, FluidBuffer.only(ModFluids.STEAM.get()), this::setChanged);
    private int status = NO_STEAM;
    private int phase;

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_ENGINE.get(), pos, state);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public FluidBuffer steam() {
        return steam;
    }

    public int status() {
        return status;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamEngineBlockEntity engine) {
        boolean running = false;
        if (engine.energy.freeSpace() < POWER) {
            engine.status = engine.steam.isEmpty() ? NO_STEAM : BUFFER_FULL;
        } else {
            // 1.5 steam per tick: one unit, then two.
            int need = engine.phase++ % 2 == 0 ? 1 : 2;
            if (engine.steam.getFluidAmount() >= need) {
                engine.steam.use(need);
                engine.energy.generate(POWER);
                engine.status = RUNNING;
                running = true;
            } else {
                engine.status = NO_STEAM;
            }
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
