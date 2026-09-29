package de.craftorio.datagen;

import de.craftorio.economy.ModDataMaps;
import de.craftorio.economy.SellPrice;
import de.craftorio.registry.ModItems;
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

        // Raw resources
        prices.add(ItemTags.LOGS, new SellPrice(10), false);
        prices.add(ItemTags.SAND, new SellPrice(10), false);
        price(prices, Items.COAL, 10);
        price(prices, Items.CHARCOAL, 10);
        price(prices, Items.RAW_IRON, 10);
        price(prices, Items.RAW_COPPER, 10);
        price(prices, Items.RAW_GOLD, 30);
        price(prices, Items.REDSTONE, 10);
        price(prices, Items.LAPIS_LAZULI, 20);
        price(prices, Items.QUARTZ, 20);
        price(prices, Items.CLAY_BALL, 10);
        price(prices, Items.COBBLESTONE, 4);
        price(prices, Items.DIAMOND, 400);
        price(prices, Items.EMERALD, 300);

        // Step 1: smelted
        price(prices, Items.GLASS, 16);
        price(prices, Items.BRICK, 16);
        price(prices, Items.IRON_INGOT, 16);
        price(prices, Items.COPPER_INGOT, 16);
        price(prices, Items.GOLD_INGOT, 45);
        price(prices, Items.IRON_NUGGET, 2);
        price(prices, Items.GOLD_NUGGET, 5);

        // Step 2: simple parts
        price(prices, ModItems.COPPER_CABLE.get(), 12);
        price(prices, ModItems.PIPE.get(), 24);
        price(prices, ModItems.STONE_BRICK.get(), 12);

        // Step 3+: assembled
        price(prices, ModItems.IRON_GEAR.get(), 60);
        price(prices, ModItems.CIRCUIT.get(), 85);
        price(prices, ModItems.MOTOR.get(), 240);

        // Cave layer
        price(prices, ModItems.RAW_TIN.get(), 15);
        price(prices, ModItems.RAW_LEAD.get(), 15);
        price(prices, ModItems.SULFUR.get(), 20);
        price(prices, ModItems.TIN_INGOT.get(), 24);
        price(prices, ModItems.LEAD_INGOT.get(), 24);
        price(prices, ModItems.BATTERY.get(), 140);
        price(prices, ModItems.ADVANCED_CIRCUIT.get(), 360);

        // Mine layer (diamonds are priced with the raw resources above)
        price(prices, ModItems.RAW_TITANIUM.get(), 40);
        price(prices, ModItems.RAW_URANIUM.get(), 50);
        price(prices, ModItems.CRYSTAL_SHARD.get(), 60);
        price(prices, ModItems.TITANIUM_INGOT.get(), 60);
        price(prices, ModItems.TITANIUM_PLATE.get(), 85);
        price(prices, ModItems.URANIUM_PELLET.get(), 75);
        price(prices, ModItems.FUEL_ROD.get(), 600);
        price(prices, ModItems.ENERGY_CRYSTAL.get(), 1_100);

        // Storage blocks: a small bonus over their contents
        price(prices, Items.COAL_BLOCK, 100);
        price(prices, Items.IRON_BLOCK, 160);
        price(prices, Items.COPPER_BLOCK, 160);
        price(prices, Items.GOLD_BLOCK, 450);
        price(prices, Items.REDSTONE_BLOCK, 100);
        price(prices, Items.LAPIS_BLOCK, 200);
        price(prices, Items.DIAMOND_BLOCK, 4000);
        price(prices, Items.EMERALD_BLOCK, 3000);
    }

    private static void price(Builder<SellPrice, Item> prices, Item item, long credits) {
        prices.add(item.builtInRegistryHolder(), new SellPrice(credits), false);
    }
}
