package de.craftorio.heat;

import de.craftorio.fluid.FluidBuffer;
import de.craftorio.fluid.FluidHelper;
import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Takes up to 10 MW of heat and turns water into 500 °C steam (about 103 units per second) as long as it is hotter
 * than 500 °C. The steam goes into the pipes and turbines around it.
 */
public final class HeatExchangerBlockEntity extends BlockEntity implements HeatNode {
    public static final int TANK = 200;
    private static final int PUSH = 60;

    private final FluidBuffer water = new FluidBuffer(TANK, FluidBuffer.only(Fluids.WATER), this::setChanged);
    private final FluidBuffer steam = new FluidBuffer(TANK, FluidBuffer.only(ModFluids.HOT_STEAM.get()), this::setChanged);
    private final IFluidHandler handler = new Handler();
    private double temperature = HeatLogic.AMBIENT;
    /** Fraction of a unit of steam that was made but not yet counted (the exchanger makes 5.15 units per tick). */
    private double budget;

    public HeatExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEAT_EXCHANGER.get(), pos, state);
    }

    public FluidBuffer water() {
        return water;
    }

    public FluidBuffer steam() {
        return steam;
    }

    public IFluidHandler handler() {
        return handler;
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
        return HeatLogic.EXCHANGER_CAPACITY;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HeatExchangerBlockEntity exchanger) {
        HeatNode.conduct(level, pos, exchanger);
        boolean working = false;
        if (exchanger.temperature >= HeatLogic.STEAM_TEMPERATURE && !exchanger.water.isEmpty() && exchanger.steam.space() > 0) {
            double usable = HeatLogic.usableHeat(exchanger.temperature, exchanger.capacity());
            exchanger.budget += Math.min(HeatLogic.EXCHANGER_STEAM_PER_TICK, usable / HeatLogic.HEAT_PER_STEAM);
            int units = (int) Math.floor(Math.min(exchanger.budget, Math.min(exchanger.water.getFluidAmount(), exchanger.steam.space())));
            if (units > 0) {
                exchanger.water.use(units);
                exchanger.steam.fill(new FluidStack(ModFluids.HOT_STEAM.get(), units), IFluidHandler.FluidAction.EXECUTE);
                exchanger.addHeat(-units * HeatLogic.HEAT_PER_STEAM);
                exchanger.budget -= units;
                working = true;
            }
            exchanger.budget = Math.min(exchanger.budget, 1);
        }
        if (!exchanger.steam.isEmpty()) {
            for (Direction direction : Direction.values()) {
                IFluidHandler neighbour = FluidHelper.neighbour(level, pos, direction);
                if (neighbour != null) {
                    FluidHelper.push(exchanger.steam, neighbour, PUSH);
                }
            }
        }
        MachineBaseBlock.setActive(level, pos, state, working);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("temperature", temperature);
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("steam", steam.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("temperature") ? tag.getDouble("temperature") : HeatLogic.AMBIENT;
        water.readFromNBT(registries, tag.getCompound("water"));
        steam.readFromNBT(registries, tag.getCompound("steam"));
    }

    /** Water goes in, steam can be taken out. */
    private final class Handler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? water.getFluid() : steam.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return TANK;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && water.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return water.isFluidValid(resource) ? water.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return steam.isFluidValid(resource) ? steam.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return steam.drain(maxDrain, action);
        }
    }
}
