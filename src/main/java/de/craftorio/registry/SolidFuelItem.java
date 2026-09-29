package de.craftorio.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/** Solid fuel from oil: 12 MJ, three times a piece of coal. */
public final class SolidFuelItem extends Item {
    /** Vanilla burn time of 12 MJ: a piece of coal (1600 ticks) is 4 MJ. */
    private static final int BURN_TIME = 3 * 1_600;

    public SolidFuelItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getBurnTime(ItemStack stack, RecipeType<?> recipeType) {
        return BURN_TIME;
    }
}
