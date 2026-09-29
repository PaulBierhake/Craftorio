package de.craftorio.fluid;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.function.Predicate;

/** A fluid tank that reports changes to its owner. */
public final class FluidBuffer extends FluidTank {
    private final Runnable onChange;

    public FluidBuffer(int capacity, Predicate<FluidStack> validator, Runnable onChange) {
        super(capacity, validator);
        this.onChange = onChange;
    }

    public FluidBuffer(int capacity, Runnable onChange) {
        this(capacity, stack -> true, onChange);
    }

    public static Predicate<FluidStack> only(Fluid fluid) {
        return stack -> stack.getFluid() == fluid;
    }

    @Override
    protected void onContentsChanged() {
        onChange.run();
    }

    public int space() {
        return capacity - fluid.getAmount();
    }

    /** Removes up to {@code amount} of the fluid the owner makes or uses itself. */
    public int use(int amount) {
        return drain(amount, FluidAction.EXECUTE).getAmount();
    }
}
