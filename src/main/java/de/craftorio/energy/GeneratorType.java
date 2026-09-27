package de.craftorio.energy;

import de.craftorio.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/** Generator generations: the coal generator burns furnace fuel, the reactor burns fuel rods. */
public enum GeneratorType {
    COAL(60, 20_000, 1_000),
    /** Mine-layer power: one fuel rod runs 6000 ticks (5 minutes) at 400 FE/t. */
    REACTOR(400, 200_000, 4_000);

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
            case COAL -> stack.getBurnTime(RecipeType.SMELTING);
            case REACTOR -> stack.is(ModItems.FUEL_ROD.get()) ? FUEL_ROD_TICKS : 0;
        };
    }
}
