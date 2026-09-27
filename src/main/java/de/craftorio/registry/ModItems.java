package de.craftorio.registry;

import de.craftorio.Craftorio;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Craftorio.MOD_ID);

    public static final DeferredItem<BlockItem> CAP_ROCK = ITEMS.registerSimpleBlockItem("cap_rock", ModBlocks.CAP_ROCK);
    public static final DeferredItem<BlockItem> TRADING_POST = ITEMS.registerSimpleBlockItem("trading_post", ModBlocks.TRADING_POST);
    public static final DeferredItem<BlockItem> IRON_ORE_FIELD = ITEMS.registerSimpleBlockItem("iron_ore_field", ModBlocks.IRON_ORE_FIELD);
    public static final DeferredItem<BlockItem> COPPER_ORE_FIELD = ITEMS.registerSimpleBlockItem("copper_ore_field", ModBlocks.COPPER_ORE_FIELD);
    public static final DeferredItem<BlockItem> COAL_FIELD = ITEMS.registerSimpleBlockItem("coal_field", ModBlocks.COAL_FIELD);
    public static final DeferredItem<BlockItem> BURNER_DRILL = ITEMS.registerSimpleBlockItem("burner_drill", ModBlocks.BURNER_DRILL);
    public static final DeferredItem<BlockItem> CONVEYOR_BELT = ITEMS.registerSimpleBlockItem("conveyor_belt", ModBlocks.CONVEYOR_BELT);
    public static final DeferredItem<BlockItem> INSERTER = ITEMS.registerSimpleBlockItem("inserter", ModBlocks.INSERTER);

    private ModItems() {
    }
}
