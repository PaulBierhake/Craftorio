package de.craftorio.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** Moving fluid between neighbouring blocks. */
public final class FluidHelper {
    private FluidHelper() {
    }

    /** The fluid handler of the block at {@code pos + side}, as seen from this block (its face towards us). */
    public static @Nullable IFluidHandler neighbour(Level level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(side), side.getOpposite());
    }

    /**
     * Lets fluid flow from {@code self} to {@code other} until both are equally full (see {@link FluidFlow}). The
     * receiving tank is the one of {@code other} that already holds this fluid, else an empty one that takes it.
     */
    public static void balance(FluidBuffer self, IFluidHandler other) {
        FluidStack mine = self.getFluid();
        if (mine.isEmpty()) {
            return;
        }
        int tank = -1;
        for (int i = 0; i < other.getTanks(); i++) {
            FluidStack theirs = other.getFluidInTank(i);
            if (!theirs.isEmpty() && FluidStack.isSameFluid(mine, theirs)) {
                tank = i;
                break;
            }
            if (theirs.isEmpty() && tank < 0 && other.isFluidValid(i, mine)) {
                tank = i;
            }
        }
        if (tank < 0) {
            return;
        }
        int moved = FluidFlow.move(mine.getAmount(), self.getCapacity(), other.getFluidInTank(tank).getAmount(),
                other.getTankCapacity(tank), FluidFlow.MAX_FLOW);
        if (moved > 0) {
            int accepted = other.fill(mine.copyWithAmount(moved), IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0) {
                self.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    /** Pushes up to {@code max} units of {@code source} into {@code target}; returns how much was taken. */
    public static int push(FluidBuffer source, IFluidHandler target, int max) {
        FluidStack offer = source.getFluid();
        if (offer.isEmpty()) {
            return 0;
        }
        int accepted = target.fill(offer.copyWithAmount(Math.min(max, offer.getAmount())), IFluidHandler.FluidAction.EXECUTE);
        if (accepted > 0) {
            source.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        }
        return accepted;
    }
}
