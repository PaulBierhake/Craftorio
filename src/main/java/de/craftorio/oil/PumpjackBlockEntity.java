package de.craftorio.oil;

import de.craftorio.energy.EnergyBuffer;
import de.craftorio.fluid.FluidBuffer;
import de.craftorio.fluid.FluidHelper;
import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Makes 10 units of crude oil per second at 100 % yield (0.5 per tick) for 90 kW and hands it to its neighbours. */
public final class PumpjackBlockEntity extends BlockEntity {
    public static final int POWER = 90;
    public static final int TANK = 200;
    private static final int PUSH = 20;
    /** Thousandths of a unit made per tick at 100 % yield. */
    private static final int MILLIS_PER_TICK = 500;

    private final FluidBuffer crude = new FluidBuffer(TANK, FluidBuffer.only(ModFluids.CRUDE_OIL.get()), this::setChanged);
    private final EnergyBuffer energy = new EnergyBuffer(20 * POWER, 10 * POWER, 0, this::setChanged);
    private int millis;
    private boolean onWell;

    public PumpjackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMPJACK.get(), pos, state);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public FluidBuffer tank() {
        return crude;
    }

    public boolean onWell() {
        return onWell;
    }

    public IFluidHandler handler(Direction side) {
        return side == Direction.DOWN ? null : new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return crude.getFluid();
            }

            @Override
            public int getTankCapacity(int tank) {
                return TANK;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return false;
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return crude.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return crude.drain(maxDrain, action);
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PumpjackBlockEntity pumpjack) {
        pumpjack.onWell = level.getBlockState(pos.below()).is(ModBlocks.OIL_WELL.get());
        boolean working = false;
        if (pumpjack.onWell && pumpjack.crude.space() > 0 && pumpjack.energy.consume(POWER)) {
            working = true;
            pumpjack.millis += MILLIS_PER_TICK * OilWellBlock.yieldPercent(pos.below()) / 100;
            int made = pumpjack.millis / 1000;
            if (made > 0) {
                pumpjack.millis -= made * 1000;
                pumpjack.crude.fill(new FluidStack(ModFluids.CRUDE_OIL.get(), made), IFluidHandler.FluidAction.EXECUTE);
            }
        }
        if (!pumpjack.crude.isEmpty()) {
            for (Direction direction : Direction.values()) {
                if (direction != Direction.DOWN) {
                    IFluidHandler neighbour = FluidHelper.neighbour(level, pos, direction);
                    if (neighbour != null) {
                        FluidHelper.push(pumpjack.crude, neighbour, PUSH);
                    }
                }
            }
        }
        MachineBaseBlock.setActive(level, pos, state, working);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        crude.writeToNBT(registries, tag);
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("millis", millis);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        crude.readFromNBT(registries, tag);
        energy.setEnergy(tag.getInt("energy"));
        millis = tag.getInt("millis");
    }
}
