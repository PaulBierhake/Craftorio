package de.craftorio.energy;

import de.craftorio.registry.ModItems;
import net.minecraft.world.item.ItemStack;

/** Generators fed with a fuel item; today only the reactor, which burns fuel rods (boiler and steam engine burn coal, see {@link BoilerBlockEntity}). */
public enum GeneratorType {
    /** Endgame power: one fuel rod runs 6000 ticks (5 minutes) at 8000 kW. */
    REACTOR(8_000, 400_000, 16_000);

    public static final int FUEL_CELL_TICKS = 4_000;

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
            case REACTOR -> stack.is(ModItems.URANIUM_FUEL_CELL.get()) ? FUEL_CELL_TICKS : 0;
        };
    }
}
