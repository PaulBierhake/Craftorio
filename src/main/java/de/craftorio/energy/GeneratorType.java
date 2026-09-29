package de.craftorio.energy;

import de.craftorio.registry.ModItems;
import net.minecraft.world.item.ItemStack;

/** Generator generations: the coal generator burns fuel (Factorio fuel values, see {@link Fuel}), the reactor burns fuel rods. */
public enum GeneratorType {
    /** 900 kW like a Factorio steam engine; the fuel buffer is a few seconds of output. */
    COAL(900, 60_000, 1_800),
    /** Mine-layer power: one fuel rod runs 6000 ticks (5 minutes) at 8000 kW. */
    REACTOR(8_000, 400_000, 16_000);

    public static final int FUEL_ROD_TICKS = 6_000;

    private final int fePerTick;
    private final int capacity;
    private final int maxOutput;

    GeneratorType(int fePerTick, int capacity, int maxOutput) {
        this.fePerTick = fePerTick;
        this.capacity = capacity;
        this.maxOutput = maxOutput;
    }

    public int fePerTick() {
        return fePerTick;
    }

    public int capacity() {
        return capacity;
    }

    public int maxOutput() {
        return maxOutput;
    }

    /** Ticks one item of this fuel keeps the generator running; 0 if it is not a fuel for this type. */
    public int burnTime(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return switch (this) {
            case COAL -> Fuel.burnTicks(stack, fePerTick);
            case REACTOR -> stack.is(ModItems.FUEL_ROD.get()) ? FUEL_ROD_TICKS : 0;
        };
    }
}
