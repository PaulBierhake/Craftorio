package de.craftorio.registry;

import de.craftorio.Craftorio;
import de.craftorio.blueprint.TerminalBlock;
import de.craftorio.defense.TowerBlock;
import de.craftorio.defense.TowerRuin;
import de.craftorio.defense.TowerType;
import de.craftorio.defense.ZoneBlocks;
import de.craftorio.blueprint.WorkbenchBlock;
import de.craftorio.economy.block.TradingPostBlock;
import de.craftorio.energy.CoalGeneratorBlock;
import de.craftorio.energy.PowerPoleBlock;
import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.machine.MachineType;
import de.craftorio.machine.ProcessingMachineBlock;
import de.craftorio.logistics.ConveyorBeltBlock;
import de.craftorio.logistics.InserterBlock;
import de.craftorio.machine.BurnerDrillBlock;
import de.craftorio.world.OreFieldBlock;
import de.craftorio.world.cave.CaveEntranceBlock;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Craftorio.MOD_ID);

    /** Unbreakable layer separating surface, caves and mines. Only passable through built entrances. */
    public static final DeferredBlock<Block> CAP_ROCK = BLOCKS.registerSimpleBlock("cap_rock",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(-1.0F, 3_600_000.0F)
                    .noLootTable()
                    .isValidSpawn(Blocks::never)
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.DEEPSLATE));

    public static final DeferredBlock<TradingPostBlock> TRADING_POST = BLOCKS.registerBlock("trading_post", TradingPostBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<OreFieldBlock> IRON_ORE_FIELD = oreField("iron_ore_field", () -> Items.RAW_IRON, MapColor.RAW_IRON);
    public static final DeferredBlock<OreFieldBlock> COPPER_ORE_FIELD = oreField("copper_ore_field", () -> Items.RAW_COPPER, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<OreFieldBlock> COAL_FIELD = oreField("coal_field", () -> Items.COAL, MapColor.COLOR_BLACK);

    // Cave layer (M6)
    /** Seals the cave layer until an entrance unlocks the area; unbreakable like cap rock. */
    public static final DeferredBlock<Block> CAVE_RUBBLE = BLOCKS.registerSimpleBlock("cave_rubble",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_GRAY)
                    .strength(-1.0F, 3_600_000.0F)
                    .noLootTable()
                    .isValidSpawn(Blocks::never)
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.GRAVEL));
    public static final DeferredBlock<CaveEntranceBlock> CAVE_ENTRANCE = BLOCKS.registerBlock("cave_entrance", CaveEntranceBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(3.0F, 1200.0F).noOcclusion().sound(SoundType.WOOD));
    public static final DeferredBlock<OreFieldBlock> TIN_ORE_FIELD = oreField("tin_ore_field", () -> ModItems.RAW_TIN.get(), MapColor.COLOR_LIGHT_GRAY);
    public static final DeferredBlock<OreFieldBlock> LEAD_ORE_FIELD = oreField("lead_ore_field", () -> ModItems.RAW_LEAD.get(), MapColor.COLOR_BLUE);
    public static final DeferredBlock<OreFieldBlock> SULFUR_FIELD = oreField("sulfur_field", () -> ModItems.SULFUR.get(), MapColor.COLOR_YELLOW);
    public static final DeferredBlock<OreFieldBlock> GOLD_ORE_FIELD = oreField("gold_ore_field", () -> Items.RAW_GOLD, MapColor.GOLD);
    public static final DeferredBlock<OreFieldBlock> QUARTZ_FIELD = oreField("quartz_field", () -> Items.QUARTZ, MapColor.QUARTZ);

    public static final DeferredBlock<BurnerDrillBlock> BURNER_DRILL = BLOCKS.registerBlock("burner_drill", BurnerDrillBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(BurnerDrillBlock.LIT) ? 10 : 0)
                    .sound(SoundType.METAL));

    public static final DeferredBlock<ConveyorBeltBlock> CONVEYOR_BELT = BLOCKS.registerBlock("conveyor_belt", ConveyorBeltBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5F)
                    .noOcclusion()
                    .sound(SoundType.METAL));

    public static final DeferredBlock<InserterBlock> INSERTER = BLOCKS.registerBlock("inserter", InserterBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(1.5F)
                    .noOcclusion()
                    .sound(SoundType.METAL));

    public static final DeferredBlock<CoalGeneratorBlock> COAL_GENERATOR = BLOCKS.registerBlock("coal_generator", CoalGeneratorBlock::new,
            machineProperties().lightLevel(state -> state.getValue(MachineBaseBlock.ACTIVE) ? 12 : 0));

    public static final DeferredBlock<PowerPoleBlock> POWER_POLE = BLOCKS.registerBlock("power_pole", PowerPoleBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.0F)
                    .noOcclusion()
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<ProcessingMachineBlock> ELECTRIC_FURNACE = machine("electric_furnace", MachineType.ELECTRIC_FURNACE);
    public static final DeferredBlock<ProcessingMachineBlock> PRESS = machine("press", MachineType.PRESS);
    public static final DeferredBlock<ProcessingMachineBlock> ASSEMBLER = machine("assembler", MachineType.ASSEMBLER);

    public static final DeferredBlock<TerminalBlock> TERMINAL = BLOCKS.registerBlock("terminal", TerminalBlock::new,
            machineProperties().lightLevel(state -> 7));

    public static final DeferredBlock<WorkbenchBlock> WORKBENCH = workbench("workbench", 1);
    public static final DeferredBlock<WorkbenchBlock> ASSEMBLY_WORKBENCH = workbench("assembly_workbench", 2);
    public static final DeferredBlock<WorkbenchBlock> PRECISION_WORKBENCH = workbench("precision_workbench", 3);

    public static final DeferredBlock<ZoneBlocks.Core> ZONE_CORE = BLOCKS.registerBlock("zone_core", ZoneBlocks.Core::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3_600_000.0F).lightLevel(state -> 12)
                    .pushReaction(PushReaction.BLOCK).sound(SoundType.AMETHYST));
    public static final DeferredBlock<ZoneBlocks.Portal> ENEMY_PORTAL = BLOCKS.registerBlock("enemy_portal", ZoneBlocks.Portal::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0F).lightLevel(state -> 10)
                    .pushReaction(PushReaction.BLOCK).sound(SoundType.AMETHYST));
    public static final DeferredBlock<ZoneBlocks.Path> PATH_BLOCK = BLOCKS.registerBlock("path_block", ZoneBlocks.Path::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.8F).sound(SoundType.GRAVEL));

    public static final DeferredBlock<TowerBlock> CROSSBOW_TOWER = tower("crossbow_tower", TowerType.CROSSBOW, SoundType.WOOD);
    public static final DeferredBlock<TowerBlock> GUN_TURRET = tower("gun_turret", TowerType.GUN, SoundType.METAL);
    public static final DeferredBlock<TowerBlock> TESLA_TOWER = tower("tesla_tower", TowerType.TESLA, SoundType.COPPER);
    public static final DeferredBlock<TowerRuin> TOWER_RUIN = BLOCKS.registerBlock("tower_ruin", TowerRuin::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.0F).noLootTable().noOcclusion().sound(SoundType.GRAVEL));

    public static DeferredBlock<TowerBlock> tower(TowerType type) {
        return switch (type) {
            case CROSSBOW -> CROSSBOW_TOWER;
            case GUN -> GUN_TURRET;
            case TESLA -> TESLA_TOWER;
        };
    }

    private static DeferredBlock<TowerBlock> tower(String name, TowerType type, SoundType sound) {
        // Towers are only broken deliberately; enemies damage their hit points, never the block.
        return BLOCKS.registerBlock(name, properties -> new TowerBlock(type, properties),
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F, 1200.0F).noOcclusion().sound(sound));
    }

    /** The workbench block of a tier (1–3). */
    public static DeferredBlock<WorkbenchBlock> workbench(int tier) {
        return switch (tier) {
            case 1 -> WORKBENCH;
            case 2 -> ASSEMBLY_WORKBENCH;
            default -> PRECISION_WORKBENCH;
        };
    }

    private static DeferredBlock<WorkbenchBlock> workbench(String name, int tier) {
        return BLOCKS.registerBlock(name, properties -> new WorkbenchBlock(tier, properties),
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(tier == 1 ? SoundType.WOOD : SoundType.METAL));
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    private static DeferredBlock<ProcessingMachineBlock> machine(String name, MachineType type) {
        return BLOCKS.registerBlock(name, properties -> new ProcessingMachineBlock(type, properties),
                machineProperties().lightLevel(state -> state.getValue(MachineBaseBlock.ACTIVE) ? 8 : 0));
    }

    private static DeferredBlock<OreFieldBlock> oreField(String name, Supplier<? extends ItemLike> resource, MapColor color) {
        return BLOCKS.registerBlock(name, properties -> new OreFieldBlock(resource, properties),
                BlockBehaviour.Properties.of()
                        .mapColor(color)
                        .strength(4.0F, 1200.0F)
                        .requiresCorrectToolForDrops()
                        .noLootTable()
                        .pushReaction(PushReaction.BLOCK)
                        .sound(SoundType.STONE));
    }

    private ModBlocks() {
    }
}
