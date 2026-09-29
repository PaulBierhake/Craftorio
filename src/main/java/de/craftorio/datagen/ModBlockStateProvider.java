package de.craftorio.datagen;

import de.craftorio.Craftorio;
import de.craftorio.registry.ModBlocks;
import de.craftorio.machine.DrillBlock;
import de.craftorio.logistics.ElevatorBlock;
import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.world.cave.CaveEntranceBlock;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, Craftorio.MOD_ID, fileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(ModBlocks.CAP_ROCK.get(), cubeAll(ModBlocks.CAP_ROCK.get()));
        simpleBlockWithItem(ModBlocks.TRADING_POST.get(), models().cubeBottomTop("trading_post",
                modLoc("block/trading_post_side"), modLoc("block/trading_post_bottom"), modLoc("block/trading_post_top")));

        oreField(ModBlocks.IRON_ORE_FIELD.get());
        oreField(ModBlocks.COPPER_ORE_FIELD.get());
        oreField(ModBlocks.COAL_FIELD.get());
        oreField(ModBlocks.STONE_FIELD.get());
        oreField(ModBlocks.WOOD_FIELD.get());
        oreField(ModBlocks.TIN_ORE_FIELD.get());
        oreField(ModBlocks.LEAD_ORE_FIELD.get());
        oreField(ModBlocks.SULFUR_FIELD.get());
        oreField(ModBlocks.GOLD_ORE_FIELD.get());
        oreField(ModBlocks.QUARTZ_FIELD.get());
        simpleBlockWithItem(ModBlocks.CAVE_RUBBLE.get(), cubeAll(ModBlocks.CAVE_RUBBLE.get()));
        oreField(ModBlocks.DIAMOND_FIELD.get());
        oreField(ModBlocks.TITANIUM_ORE_FIELD.get());
        oreField(ModBlocks.URANIUM_ORE_FIELD.get());
        oreField(ModBlocks.CRYSTAL_FIELD.get());
        simpleBlockWithItem(ModBlocks.MINE_RUBBLE.get(), cubeAll(ModBlocks.MINE_RUBBLE.get()));

        Map<ElevatorBlock.Mode, ModelFile> elevatorModels = new EnumMap<>(ElevatorBlock.Mode.class);
        for (ElevatorBlock.Mode mode : ElevatorBlock.Mode.values()) {
            elevatorModels.put(mode, models().orientable("elevator_" + mode.getSerializedName(), modLoc("block/elevator_" + mode.getSerializedName()),
                    modLoc("block/elevator_front"), modLoc("block/elevator_top")));
        }
        horizontalBlock(ModBlocks.ELEVATOR.get(), state -> elevatorModels.get(state.getValue(ElevatorBlock.MODE)));
        simpleBlockItem(ModBlocks.ELEVATOR.get(), elevatorModels.get(ElevatorBlock.Mode.RECEIVE));

        entrance(ModBlocks.CAVE_ENTRANCE.get(), "cave_entrance", models().getExistingFile(modLoc("block/cave_entrance_open")));
        entrance(ModBlocks.MINE_SHAFT.get(), "mine_shaft", models().withExistingParent("mine_shaft_open", modLoc("block/cave_entrance_open"))
                .texture("particle", modLoc("block/mine_shaft_side")).texture("wood", modLoc("block/mine_shaft_side")));

        drill(ModBlocks.BURNER_DRILL.get(), "burner_drill");
        drill(ModBlocks.ELECTRIC_DRILL.get(), "electric_drill");
        drill(ModBlocks.DEEP_DRILL.get(), "deep_drill");

        // Hand-written models in src/main/resources (belt and arm geometry), rotated by facing.
        belt(ModBlocks.CONVEYOR_BELT.get(), "conveyor_belt", "conveyor_belt", "conveyor_belt_side", true);
        belt(ModBlocks.FAST_BELT.get(), "fast_belt", "fast_belt", "fast_belt_side", true);
        belt(ModBlocks.EXPRESS_BELT.get(), "express_belt", "express_belt", "express_belt_side", true);
        belt(ModBlocks.UNDERGROUND_BELT.get(), "underground_belt", "underground_belt", "conveyor_belt_side", false);
        belt(ModBlocks.FAST_UNDERGROUND_BELT.get(), "fast_underground_belt", "fast_underground_belt", "fast_belt_side", false);
        belt(ModBlocks.SPLITTER.get(), "splitter", "splitter", "conveyor_belt_side", false);
        belt(ModBlocks.FAST_SPLITTER.get(), "fast_splitter", "fast_splitter", "fast_belt_side", false);
        for (var inserter : new Object[][]{{ModBlocks.INSERTER.get(), "inserter", "inserter_arm"},
                {ModBlocks.LONG_INSERTER.get(), "long_inserter", "inserter_long_arm"},
                {ModBlocks.FAST_INSERTER.get(), "fast_inserter", "inserter_fast_arm"},
                {ModBlocks.FILTER_INSERTER.get(), "filter_inserter", "inserter_filter_arm"}}) {
            String name = (String) inserter[1];
            ModelFile model = name.equals("inserter") ? models().getExistingFile(modLoc("block/inserter"))
                    : models().withExistingParent(name, modLoc("block/inserter")).texture("arm", modLoc("block/" + inserter[2]));
            horizontalBlock((Block) inserter[0], model);
            simpleBlockItem((Block) inserter[0], model);
        }

        machine(ModBlocks.BOILER.get(), "boiler");
        machine(ModBlocks.STEAM_ENGINE.get(), "steam_engine");
        simpleBlockWithItem(ModBlocks.OFFSHORE_PUMP.get(), models().cubeBottomTop("offshore_pump", modLoc("block/offshore_pump_side"),
                modLoc("block/machine_top"), modLoc("block/offshore_pump_top")));
        machine(ModBlocks.REACTOR.get(), "reactor");
        machine(ModBlocks.ELECTRIC_FURNACE.get(), "electric_furnace");
        machine(ModBlocks.STONE_FURNACE.get(), "stone_furnace");
        machine(ModBlocks.STEEL_FURNACE.get(), "steel_furnace");
        machine(ModBlocks.ASSEMBLER.get(), "assembler");
        machine(ModBlocks.ASSEMBLER_2.get(), "assembler_2");
        machine(ModBlocks.LABORATORY.get(), "laboratory");
        workbench(ModBlocks.WORKBENCH.get(), "workbench");
        ModelFile terminal = models().orientable("terminal", modLoc("block/machine_side"), modLoc("block/terminal_front"), modLoc("block/machine_top"));
        horizontalBlock(ModBlocks.TERMINAL.get(), terminal);
        simpleBlockItem(ModBlocks.TERMINAL.get(), terminal);

        simpleBlockWithItem(ModBlocks.ZONE_CORE.get(), cubeAll(ModBlocks.ZONE_CORE.get()));
        simpleBlockWithItem(ModBlocks.ARENA_BASE.get(), cubeAll(ModBlocks.ARENA_BASE.get()));
        simpleBlockWithItem(ModBlocks.ARENA_CLIFF.get(), cubeAll(ModBlocks.ARENA_CLIFF.get()));
        for (String name : new String[]{"arena_gate", "arena_exit", "tower_depot"}) {
            Block block = switch (name) {
                case "arena_gate" -> ModBlocks.ARENA_GATE.get();
                case "arena_exit" -> ModBlocks.ARENA_EXIT.get();
                default -> ModBlocks.TOWER_DEPOT.get();
            };
            simpleBlockWithItem(block, models().cubeBottomTop(name, modLoc("block/" + name + "_side"),
                    modLoc("block/" + name + "_top"), modLoc("block/" + name + "_top")));
        }
        simpleBlockWithItem(ModBlocks.ARENA_CONSOLE.get(), models().cubeBottomTop("arena_console", modLoc("block/terminal_front"),
                modLoc("block/machine_top"), modLoc("block/machine_top")));
        simpleBlockWithItem(ModBlocks.ARENA_FEEDER.get(), models().cubeBottomTop("arena_feeder", modLoc("block/arena_feeder_front"),
                modLoc("block/machine_top"), modLoc("block/arena_feeder_top")));
        simpleBlockWithItem(ModBlocks.ENEMY_PORTAL.get(), models().cubeBottomTop("enemy_portal",
                modLoc("block/enemy_portal_side"), modLoc("block/enemy_portal_top"), modLoc("block/enemy_portal_top")));
        for (String name : new String[]{"path_block", "crossbow_tower", "gun_turret", "tesla_tower", "laser_tower", "tower_ruin"}) {
            ModelFile model = models().getExistingFile(modLoc("block/" + name));
            Block block = switch (name) {
                case "path_block" -> ModBlocks.PATH_BLOCK.get();
                case "crossbow_tower" -> ModBlocks.CROSSBOW_TOWER.get();
                case "gun_turret" -> ModBlocks.GUN_TURRET.get();
                case "tesla_tower" -> ModBlocks.TESLA_TOWER.get();
                case "laser_tower" -> ModBlocks.LASER_TOWER.get();
                default -> ModBlocks.TOWER_RUIN.get();
            };
            simpleBlock(block, model);
            simpleBlockItem(block, model);
        }

        ModelFile pole = models().getExistingFile(modLoc("block/power_pole"));
        simpleBlock(ModBlocks.POWER_POLE.get(), pole);
        simpleBlockItem(ModBlocks.POWER_POLE.get(), pole);
        ModelFile mediumPole = models().getExistingFile(modLoc("block/medium_power_pole"));
        simpleBlock(ModBlocks.MEDIUM_POWER_POLE.get(), mediumPole);
        simpleBlockItem(ModBlocks.MEDIUM_POWER_POLE.get(), mediumPole);
        ModelFile solar = models().getExistingFile(modLoc("block/solar_panel"));
        simpleBlock(ModBlocks.SOLAR_PANEL.get(), solar);
        simpleBlockItem(ModBlocks.SOLAR_PANEL.get(), solar);
    }

    private void workbench(Block block, String name) {
        ModelFile model = models().orientableWithBottom(name, modLoc("block/" + name + "_side"), modLoc("block/" + name + "_front"),
                modLoc("block/" + name + "_bottom"), modLoc("block/" + name + "_top"));
        horizontalBlock(block, model);
        simpleBlockItem(block, model);
    }

    /** Shared machine casing with a front that lights up while the machine works. */
    /**
     * Belt models are hand-written in src/main/resources; tiers only swap textures. Slopes use the rising and falling
     * variants, an underground exit its own top texture.
     */
    private void belt(Block block, String name, String top, String side, boolean slopes) {
        ModelFile flat = beltModel(name, "conveyor_belt", top, side);
        ModelFile up = slopes ? beltModel(name + "_up", "conveyor_belt_up", top, side) : flat;
        ModelFile down = slopes ? beltModel(name + "_down", "conveyor_belt_down", top, side) : flat;
        ModelFile exit = top.endsWith("underground_belt") ? beltModel(name + "_exit", "conveyor_belt", top + "_exit", side) : flat;
        getVariantBuilder(block).forAllStates(state -> {
            ModelFile model = switch (state.getValue(de.craftorio.logistics.ConveyorBeltBlock.SLOPE)) {
                case FLAT -> state.getValue(de.craftorio.logistics.ConveyorBeltBlock.EXIT) ? exit : flat;
                case UP -> up;
                case DOWN -> down;
            };
            return ConfiguredModel.builder().modelFile(model)
                    .rotationY(((int) state.getValue(de.craftorio.logistics.ConveyorBeltBlock.FACING).toYRot() + 180) % 360).build();
        });
        simpleBlockItem(block, flat);
    }

    private ModelFile beltModel(String modelName, String parent, String top, String side) {
        if (modelName.equals("conveyor_belt") || modelName.equals("conveyor_belt_up") || modelName.equals("conveyor_belt_down")) {
            return models().getExistingFile(modLoc("block/" + modelName));
        }
        return models().withExistingParent(modelName, modLoc("block/" + parent))
                .texture("particle", modLoc("block/" + side)).texture("top", modLoc("block/" + top)).texture("side", modLoc("block/" + side));
    }

    private void machine(Block block, String name) {
        ModelFile off = models().orientable(name, modLoc("block/machine_side"), modLoc("block/" + name + "_front"), modLoc("block/machine_top"));
        ModelFile on = models().orientable(name + "_on", modLoc("block/machine_side"), modLoc("block/" + name + "_front_on"), modLoc("block/machine_top"));
        horizontalBlock(block, state -> state.getValue(MachineBaseBlock.ACTIVE) ? on : off);
        simpleBlockItem(block, off);
    }

    private String name(Block block) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    /** Construction site, drilling and open frame of a cave entrance or mine shaft. */
    private void entrance(Block block, String name, ModelFile open) {
        ModelFile site = models().cubeBottomTop(name + "_site", modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_bottom"), modLoc("block/" + name + "_top"));
        ModelFile drilling = models().cubeBottomTop(name + "_drilling", modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_bottom"), modLoc("block/" + name + "_drilling"));
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(switch (state.getValue(CaveEntranceBlock.STAGE)) {
                    case CaveEntranceBlock.STAGE_MATERIALS -> site;
                    case CaveEntranceBlock.STAGE_DRILLING -> drilling;
                    default -> open;
                }).build());
        simpleBlockItem(block, site);
    }

    private void drill(Block block, String name) {
        ModelFile off = models().orientable(name, modLoc("block/" + name + "_side"), modLoc("block/" + name + "_front"), modLoc("block/" + name + "_top"));
        ModelFile on = models().orientable(name + "_on", modLoc("block/" + name + "_side"), modLoc("block/" + name + "_front_on"), modLoc("block/" + name + "_top"));
        horizontalBlock(block, state -> state.getValue(DrillBlock.LIT) ? on : off);
        simpleBlockItem(block, off);
    }

    private void oreField(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }
}
