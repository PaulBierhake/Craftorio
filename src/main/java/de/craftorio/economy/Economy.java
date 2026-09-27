package de.craftorio.economy;

import de.craftorio.team.TeamData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class Economy {
    private Economy() {
    }

    /** Price of a single item, or 0 if it can't be sold. */
    public static long unitPrice(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        SellPrice price = stack.getItemHolder().getData(ModDataMaps.SELL_PRICE);
        return price == null ? 0 : price.price();
    }

    public static boolean isSellable(ItemStack stack) {
        return unitPrice(stack) > 0;
    }

    /** Credits the whole stack to the team and returns the amount earned. Does not modify the stack. */
    public static long sell(MinecraftServer server, UUID teamId, ItemStack stack) {
        long unit = unitPrice(stack);
        if (unit == 0) {
            return 0;
        }
        long earned;
        try {
            earned = Math.multiplyExact(unit, stack.getCount());
        } catch (ArithmeticException overflow) {
            earned = Long.MAX_VALUE;
        }
        TeamData.registry(server).deposit(teamId, earned);
        return earned;
    }
}
