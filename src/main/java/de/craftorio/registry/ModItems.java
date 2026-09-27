package de.craftorio.registry;

import de.craftorio.Craftorio;
import de.craftorio.blueprint.KeyMaterialItem;
import de.craftorio.blueprint.WorkbenchUpgradeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
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
    public static final DeferredItem<BlockItem> COAL_GENERATOR = ITEMS.registerSimpleBlockItem("coal_generator", ModBlocks.COAL_GENERATOR);
    public static final DeferredItem<BlockItem> POWER_POLE = ITEMS.registerSimpleBlockItem("power_pole", ModBlocks.POWER_POLE);
    public static final DeferredItem<BlockItem> ELECTRIC_FURNACE = ITEMS.registerSimpleBlockItem("electric_furnace", ModBlocks.ELECTRIC_FURNACE);
    public static final DeferredItem<BlockItem> PRESS = ITEMS.registerSimpleBlockItem("press", ModBlocks.PRESS);
    public static final DeferredItem<BlockItem> ASSEMBLER = ITEMS.registerSimpleBlockItem("assembler", ModBlocks.ASSEMBLER);

    public static final DeferredItem<BlockItem> TERMINAL = ITEMS.registerSimpleBlockItem("terminal", ModBlocks.TERMINAL);
    public static final DeferredItem<BlockItem> WORKBENCH = ITEMS.registerSimpleBlockItem("workbench", ModBlocks.WORKBENCH);
    public static final DeferredItem<BlockItem> ASSEMBLY_WORKBENCH = ITEMS.registerSimpleBlockItem("assembly_workbench", ModBlocks.ASSEMBLY_WORKBENCH);
    public static final DeferredItem<BlockItem> PRECISION_WORKBENCH = ITEMS.registerSimpleBlockItem("precision_workbench", ModBlocks.PRECISION_WORKBENCH);

    public static final DeferredItem<WorkbenchUpgradeItem> WORKBENCH_UPGRADE_2 = ITEMS.registerItem("workbench_upgrade_2",
            properties -> new WorkbenchUpgradeItem(2, properties), new Item.Properties().stacksTo(16));
    public static final DeferredItem<WorkbenchUpgradeItem> WORKBENCH_UPGRADE_3 = ITEMS.registerItem("workbench_upgrade_3",
            properties -> new WorkbenchUpgradeItem(3, properties), new Item.Properties().stacksTo(16));

    public static final DeferredItem<BlockItem> ZONE_CORE = ITEMS.registerSimpleBlockItem("zone_core", ModBlocks.ZONE_CORE);
    public static final DeferredItem<BlockItem> ENEMY_PORTAL = ITEMS.registerSimpleBlockItem("enemy_portal", ModBlocks.ENEMY_PORTAL);
    public static final DeferredItem<BlockItem> PATH_BLOCK = ITEMS.registerSimpleBlockItem("path_block", ModBlocks.PATH_BLOCK);
    public static final DeferredItem<BlockItem> CROSSBOW_TOWER = ITEMS.registerSimpleBlockItem("crossbow_tower", ModBlocks.CROSSBOW_TOWER);
    public static final DeferredItem<BlockItem> GUN_TURRET = ITEMS.registerSimpleBlockItem("gun_turret", ModBlocks.GUN_TURRET);
    public static final DeferredItem<BlockItem> TESLA_TOWER = ITEMS.registerSimpleBlockItem("tesla_tower", ModBlocks.TESLA_TOWER);
    public static final DeferredItem<Item> BOLT = ITEMS.registerSimpleItem("bolt");
    public static final DeferredItem<Item> CARTRIDGE = ITEMS.registerSimpleItem("cartridge");

    // Key materials from tower defense milestones (every 10 levels).
    public static final DeferredItem<KeyMaterialItem> DRILL_CORE = keyMaterial("drill_core", 10);
    public static final DeferredItem<KeyMaterialItem> RESONANCE_CRYSTAL = keyMaterial("resonance_crystal", 20);
    public static final DeferredItem<KeyMaterialItem> DEEP_CORE = keyMaterial("deep_core", 30);
    public static final DeferredItem<KeyMaterialItem> STAR_SHARD = keyMaterial("star_shard", 40);

    public static final DeferredItem<BlockItem> CAVE_RUBBLE = ITEMS.registerSimpleBlockItem("cave_rubble", ModBlocks.CAVE_RUBBLE);
    public static final DeferredItem<BlockItem> CAVE_ENTRANCE = ITEMS.registerSimpleBlockItem("cave_entrance", ModBlocks.CAVE_ENTRANCE);
    public static final DeferredItem<BlockItem> TIN_ORE_FIELD = ITEMS.registerSimpleBlockItem("tin_ore_field", ModBlocks.TIN_ORE_FIELD);
    public static final DeferredItem<BlockItem> LEAD_ORE_FIELD = ITEMS.registerSimpleBlockItem("lead_ore_field", ModBlocks.LEAD_ORE_FIELD);
    public static final DeferredItem<BlockItem> SULFUR_FIELD = ITEMS.registerSimpleBlockItem("sulfur_field", ModBlocks.SULFUR_FIELD);
    public static final DeferredItem<BlockItem> GOLD_ORE_FIELD = ITEMS.registerSimpleBlockItem("gold_ore_field", ModBlocks.GOLD_ORE_FIELD);
    public static final DeferredItem<BlockItem> QUARTZ_FIELD = ITEMS.registerSimpleBlockItem("quartz_field", ModBlocks.QUARTZ_FIELD);

    // Cave resources and their products
    public static final DeferredItem<Item> RAW_TIN = ITEMS.registerSimpleItem("raw_tin");
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.registerSimpleItem("tin_ingot");
    public static final DeferredItem<Item> RAW_LEAD = ITEMS.registerSimpleItem("raw_lead");
    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.registerSimpleItem("lead_ingot");
    public static final DeferredItem<Item> SULFUR = ITEMS.registerSimpleItem("sulfur");
    public static final DeferredItem<Item> BATTERY = ITEMS.registerSimpleItem("battery");
    public static final DeferredItem<Item> ADVANCED_CIRCUIT = ITEMS.registerSimpleItem("advanced_circuit");

    // Intermediate products; each processing step is worth more than its inputs.
    public static final DeferredItem<Item> IRON_PLATE = ITEMS.registerSimpleItem("iron_plate");
    public static final DeferredItem<Item> COPPER_CABLE = ITEMS.registerSimpleItem("copper_cable");
    public static final DeferredItem<Item> IRON_GEAR = ITEMS.registerSimpleItem("iron_gear");
    public static final DeferredItem<Item> CIRCUIT = ITEMS.registerSimpleItem("circuit");
    public static final DeferredItem<Item> MOTOR = ITEMS.registerSimpleItem("motor");

    private static DeferredItem<KeyMaterialItem> keyMaterial(String name, int level) {
        return ITEMS.registerItem(name, properties -> new KeyMaterialItem(level, properties),
                new Item.Properties().rarity(Rarity.EPIC).stacksTo(16));
    }

    private ModItems() {
    }
}
