package de.craftorio.datagen;

import de.craftorio.economy.ModDataMaps;
import de.craftorio.economy.SellPrice;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.DataMapProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Base price table. Each processing step is worth about 1.3–1.6x its inputs, so refining always pays.
 * Vanilla items bridge the gap until Craftorio's own resources arrive in M2.
 */
public final class ModSellPriceProvider extends DataMapProvider {
    public ModSellPriceProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void gather(HolderLookup.Provider registries) {
        var prices = builder(ModDataMaps.SELL_PRICE);

        prices.add(ItemTags.LOGS, new SellPrice(1), false);
        prices.add(ItemTags.SAND, new SellPrice(1), false);

        price(prices, Items.COAL, 1);
        price(prices, Items.CHARCOAL, 1);
        price(prices, Items.RAW_IRON, 1);
        price(prices, Items.RAW_COPPER, 1);
        price(prices, Items.RAW_GOLD, 3);
        price(prices, Items.REDSTONE, 1);
        price(prices, Items.LAPIS_LAZULI, 2);
        price(prices, Items.QUARTZ, 2);
        price(prices, Items.CLAY_BALL, 1);

        price(prices, Items.GLASS, 2);
        price(prices, Items.BRICK, 2);
        price(prices, Items.IRON_INGOT, 2);
        price(prices, Items.COPPER_INGOT, 2);
        price(prices, Items.GOLD_INGOT, 5);
        price(prices, Items.IRON_NUGGET, 1);
        price(prices, Items.GOLD_NUGGET, 1);

        price(prices, Items.COAL_BLOCK, 12);
        price(prices, Items.IRON_BLOCK, 25);
        price(prices, Items.COPPER_BLOCK, 25);
        price(prices, Items.GOLD_BLOCK, 60);
        price(prices, Items.REDSTONE_BLOCK, 12);
        price(prices, Items.LAPIS_BLOCK, 24);

        price(prices, Items.DIAMOND, 40);
        price(prices, Items.EMERALD, 30);
        price(prices, Items.DIAMOND_BLOCK, 480);
        price(prices, Items.EMERALD_BLOCK, 360);
    }

    private static void price(Builder<SellPrice, Item> prices, Item item, long credits) {
        prices.add(item.builtInRegistryHolder(), new SellPrice(credits), false);
    }
}
