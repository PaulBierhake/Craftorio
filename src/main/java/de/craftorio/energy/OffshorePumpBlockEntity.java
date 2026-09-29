package de.craftorio.energy;

import de.craftorio.fluid.FluidFlow;
import de.craftorio.fluid.FluidHelper;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Gives water to the neighbours that take it, up to {@link FluidFlow#MAX_FLOW} units per tick in all. */
public final class OffshorePumpBlockEntity extends BlockEntity {
    private static final int WATER_CHECK_INTERVAL = 20;
    private int waterCheckIn;
    private boolean water;

    public OffshorePumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OFFSHORE_PUMP.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OffshorePumpBlockEntity pump) {
        if (--pump.waterCheckIn <= 0) {
            pump.waterCheckIn = WATER_CHECK_INTERVAL;
            pump.water = OffshorePumpBlock.hasWater(level, pos);
        }
        if (!pump.water) {
            return;
        }
        int left = FluidFlow.MAX_FLOW;
        for (Direction direction : Direction.values()) {
            IFluidHandler neighbour = FluidHelper.neighbour(level, pos, direction);
            if (neighbour != null && left > 0) {
                left -= neighbour.fill(new FluidStack(Fluids.WATER, left), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }
}
