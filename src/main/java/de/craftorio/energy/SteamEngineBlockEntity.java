package de.craftorio.energy;

import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 900 kW of grid power while an adjacent boiler gives steam; the output buffer is two ticks of production. */
public final class SteamEngineBlockEntity extends BlockEntity implements PowerSource {
    public static final int POWER = Energy.STEAM_ENGINE_KW;

    /** Why the engine runs or not, for the status message. */
    public static final int RUNNING = 0;
    public static final int NO_BOILER = 1;
    public static final int NO_STEAM = 2;
    /** Steam is there, but the output buffer is full: nothing draws power from the engine (no pole or no consumer). */
    public static final int BUFFER_FULL = 3;

    private final EnergyBuffer energy = new EnergyBuffer(2 * POWER, 0, 2 * POWER, this::setChanged);
    private int status = NO_BOILER;

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_ENGINE.get(), pos, state);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public int status() {
        return status;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamEngineBlockEntity engine) {
        boolean running = false;
        engine.status = NO_BOILER;
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof BoilerBlockEntity boiler) {
                engine.status = engine.energy.freeSpace() < POWER && boiler.hasWater() ? BUFFER_FULL : NO_STEAM;
                if (engine.energy.freeSpace() >= POWER && boiler.tryServe(pos, level.getGameTime())) {
                    engine.energy.generate(POWER);
                    engine.status = RUNNING;
                    running = true;
                    break;
                }
            }
        }
        MachineBaseBlock.setActive(level, pos, state, running);
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
