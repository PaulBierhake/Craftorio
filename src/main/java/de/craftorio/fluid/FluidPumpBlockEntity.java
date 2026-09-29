package de.craftorio.fluid;

import de.craftorio.energy.EnergyBuffer;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** Draws fluid out of the block behind it and pushes it into the one in front, {@link FluidFlow#MAX_FLOW} units per tick, for 29 kW. */
public final class FluidPumpBlockEntity extends BlockEntity {
    public static final int POWER = 29;

    private final FluidBuffer buffer = new FluidBuffer(2 * FluidFlow.MAX_FLOW, this::setChanged);
    private final EnergyBuffer energy = new EnergyBuffer(20 * POWER, 10 * POWER, 0, this::setChanged);
    private boolean running;

    public FluidPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_PUMP.get(), pos, state);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public boolean running() {
        return running;
    }

    /** Fluid goes in from behind and comes out in front; nothing is stored for long. */
    public @Nullable IFluidHandler handler(@Nullable Direction side) {
        Direction facing = getBlockState().getValue(FluidPumpBlock.FACING);
        return side == null || side.getAxis() == facing.getAxis() ? buffer : null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidPumpBlockEntity pump) {
        Direction facing = state.getValue(FluidPumpBlock.FACING);
        boolean moved = false;
        if (pump.energy.getEnergyStored() >= POWER) {
            IFluidHandler behind = FluidHelper.neighbour(level, pos, facing.getOpposite());
            if (behind != null && pump.buffer.space() > 0) {
                var drained = behind.drain(Math.min(FluidFlow.MAX_FLOW, pump.buffer.space()), IFluidHandler.FluidAction.EXECUTE);
                if (!drained.isEmpty()) {
                    pump.buffer.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                    moved = true;
                }
            }
        }
        IFluidHandler front = FluidHelper.neighbour(level, pos, facing);
        if (front != null && !pump.buffer.isEmpty() && FluidHelper.push(pump.buffer, front, FluidFlow.MAX_FLOW) > 0) {
            moved = true;
        }
        if (moved) {
            pump.energy.consume(POWER);
        }
        pump.running = moved;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        buffer.writeToNBT(registries, tag);
        tag.putInt("energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer.readFromNBT(registries, tag);
        energy.setEnergy(tag.getInt("energy"));
    }
}
