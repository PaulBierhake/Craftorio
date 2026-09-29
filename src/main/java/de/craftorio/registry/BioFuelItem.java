package de.craftorio.registry;

import de.craftorio.energy.Energy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/** Fuel pressed from plant matter: 12 MJ like solid fuel, three times a piece of coal. */
public final class BioFuelItem extends Item {
    /** Vanilla burn time of 12 MJ: a piece of coal (1600 ticks) is 4 MJ. */
    private static final int BURN_TIME = 3 * 1_600;

    public BioFuelItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getBurnTime(ItemStack stack, RecipeType<?> recipeType) {
        return BURN_TIME;
    }

    /** For tests: the energy content in MJ. */
    public static double megajoules() {
        return BURN_TIME * 4.0 / 1_600;
    }
}
