package de.craftorio.economy;

import de.craftorio.Craftorio;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class ModDataMaps {
    /** data/&lt;namespace&gt;/data_maps/item/sell_prices.json; synced so clients can show prices in tooltips. */
    public static final DataMapType<Item, SellPrice> SELL_PRICE = DataMapType
            .builder(Craftorio.id("sell_prices"), Registries.ITEM, SellPrice.CODEC)
            .synced(SellPrice.CODEC, false)
            .build();

    private ModDataMaps() {
    }

    @SubscribeEvent
    public static void register(RegisterDataMapTypesEvent event) {
        event.register(SELL_PRICE);
    }
}
