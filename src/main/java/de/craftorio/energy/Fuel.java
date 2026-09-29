package de.craftorio.energy;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Fuel values as in Factorio (coal 4 MJ, wood 2 MJ), for every fuel-burning device of the mod. Vanilla furnaces keep
 * their own burn times. Other vanilla fuels are converted so that coal's 1600 ticks correspond to 4 MJ.
 */
public final class Fuel {
    public static final double COAL_MJ = 4;
    public static final double WOOD_MJ = 2;
    private static final int VANILLA_COAL_TICKS = 1_600;

    private Fuel() {
    }

    public static double megajoules(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            return COAL_MJ;
        }
        if (stack.is(ItemTags.LOGS_THAT_BURN)) {
            return WOOD_MJ;
        }
        return stack.getBurnTime(RecipeType.SMELTING) * COAL_MJ / VANILLA_COAL_TICKS;
    }

    public static boolean isFuel(ItemStack stack) {
        return megajoules(stack) > 0;
    }

    /** Ticks one item keeps a device of {@code kw} kilowatts running; 0 if it is not a fuel. */
    public static int burnTicks(ItemStack stack, int kw) {
        return Energy.burnTicks(megajoules(stack), kw);
    }
}
