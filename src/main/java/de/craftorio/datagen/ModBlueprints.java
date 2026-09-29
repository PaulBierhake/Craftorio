package de.craftorio.datagen;

import de.craftorio.Craftorio;
import de.craftorio.blueprint.Blueprint;
import de.craftorio.registry.ModItems;
import de.craftorio.registry.ModRegistries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

/**
 * The workbench recipes: hand crafting with the recipes of Factorio 1.1 (docs/FACTORIO-UMBAU.md §4). Iron and copper
 * plates are the vanilla ingots. Which of them a team may use is decided by the researches in {@link ModResearch}:
 * a blueprint that no research unlocks is available from the start.
 */
public final class ModBlueprints {
    private static int order;

    private ModBlueprints() {
    }

    public static void bootstrap(BootstrapContext<Blueprint> context) {
        order = 0;
        // Intermediate products
        add(context, "iron_gear", stack(ModItems.IRON_GEAR.get(), 1), iron(2));
        add(context, "copper_cable", stack(ModItems.COPPER_CABLE.get(), 2), copper(1));
        add(context, "circuit", stack(ModItems.CIRCUIT.get(), 1), iron(1), SizedIngredient.of(ModItems.COPPER_CABLE.get(), 3));
        add(context, "pipe", stack(ModItems.PIPE.get(), 1), iron(1));
        add(context, "iron_stick", stack(ModItems.IRON_STICK.get(), 2), iron(1));

        // Start: mining, smelting, transport, power, science
        add(context, "stone_furnace", stack(ModItems.STONE_FURNACE.get(), 1), SizedIngredient.of(Items.COBBLESTONE, 5));
        add(context, "burner_drill", stack(ModItems.BURNER_DRILL.get(), 1),
                SizedIngredient.of(ModItems.IRON_GEAR.get(), 3), SizedIngredient.of(ModItems.STONE_FURNACE.get(), 1), iron(3));
        add(context, "conveyor_belt", stack(ModItems.CONVEYOR_BELT.get(), 2), SizedIngredient.of(ModItems.IRON_GEAR.get(), 1), iron(1));
        add(context, "inserter", stack(ModItems.INSERTER.get(), 1),
                SizedIngredient.of(ModItems.CIRCUIT.get(), 1), SizedIngredient.of(ModItems.IRON_GEAR.get(), 1), iron(1));
        add(context, "boiler", stack(ModItems.BOILER.get(), 1), SizedIngredient.of(ModItems.STONE_FURNACE.get(), 1), SizedIngredient.of(ModItems.PIPE.get(), 4));
        add(context, "steam_engine", stack(ModItems.STEAM_ENGINE.get(), 1),
                SizedIngredient.of(ModItems.IRON_GEAR.get(), 8), SizedIngredient.of(ModItems.PIPE.get(), 5), iron(10));
        add(context, "offshore_pump", stack(ModItems.OFFSHORE_PUMP.get(), 1),
                SizedIngredient.of(ModItems.CIRCUIT.get(), 2), SizedIngredient.of(ModItems.PIPE.get(), 1), SizedIngredient.of(ModItems.IRON_GEAR.get(), 1));
        add(context, "power_pole", stack(ModItems.POWER_POLE.get(), 2), SizedIngredient.of(ItemTags.LOGS, 1), SizedIngredient.of(ModItems.COPPER_CABLE.get(), 2));
        add(context, "electric_drill", stack(ModItems.ELECTRIC_DRILL.get(), 1),
                SizedIngredient.of(ModItems.CIRCUIT.get(), 3), SizedIngredient.of(ModItems.IRON_GEAR.get(), 5), iron(10));
        add(context, "laboratory", stack(ModItems.LABORATORY.get(), 1),
                SizedIngredient.of(ModItems.CIRCUIT.get(), 10), SizedIngredient.of(ModItems.IRON_GEAR.get(), 10),
                SizedIngredient.of(ModItems.CONVEYOR_BELT.get(), 4));
        add(context, "red_science", stack(ModItems.RED_SCIENCE.get(), 1), copper(1), SizedIngredient.of(ModItems.IRON_GEAR.get(), 1));
        // Green science: after the research "logistic science pack"
        add(context, "green_science", stack(ModItems.GREEN_SCIENCE.get(), 1),
                SizedIngredient.of(ModItems.INSERTER.get(), 1), SizedIngredient.of(ModItems.CONVEYOR_BELT.get(), 1));

        // Start: trade and defend
        add(context, "trading_post", stack(ModItems.TRADING_POST.get(), 1),
                SizedIngredient.of(ItemTags.PLANKS, 16), iron(4));
        add(context, "terminal", stack(ModItems.TERMINAL.get(), 1),
                iron(5), SizedIngredient.of(ModItems.CIRCUIT.get(), 2));
        add(context, "arena_gate", stack(ModItems.ARENA_GATE.get(), 1),
                iron(8), SizedIngredient.of(ModItems.STONE_BRICK.get(), 8), SizedIngredient.of(ModItems.CIRCUIT.get(), 2));
        add(context, "crossbow_tower", stack(ModItems.CROSSBOW_TOWER.get(), 1),
                SizedIngredient.of(ItemTags.PLANKS, 12), iron(6), SizedIngredient.of(ModItems.IRON_GEAR.get(), 4));
        add(context, "bolt", stack(ModItems.BOLT.get(), 16), iron(1), SizedIngredient.of(Items.STICK, 2));

        // Automation
        add(context, "assembler", stack(ModItems.ASSEMBLER.get(), 1),
                SizedIngredient.of(ModItems.CIRCUIT.get(), 3), SizedIngredient.of(ModItems.IRON_GEAR.get(), 5), iron(9));
        add(context, "electric_furnace", stack(ModItems.ELECTRIC_FURNACE.get(), 1),
                SizedIngredient.of(ModItems.STONE_BRICK.get(), 10), SizedIngredient.of(ModItems.CIRCUIT.get(), 5),
                SizedIngredient.of(ModItems.STONE_FURNACE.get(), 1));

        // Steel age (the motor and the steel plate itself are machine recipes only, as in Factorio)
        add(context, "medium_power_pole", stack(ModItems.MEDIUM_POWER_POLE.get(), 1),
                SizedIngredient.of(ModItems.STEEL_PLATE.get(), 2), copper(2), SizedIngredient.of(ModItems.IRON_STICK.get(), 4));
        add(context, "assembler_2", stack(ModItems.ASSEMBLER_2.get(), 1),
                SizedIngredient.of(ModItems.STEEL_PLATE.get(), 2), SizedIngredient.of(ModItems.CIRCUIT.get(), 3),
                SizedIngredient.of(ModItems.IRON_GEAR.get(), 5), SizedIngredient.of(ModItems.ASSEMBLER.get(), 1));
        add(context, "steel_furnace", stack(ModItems.STEEL_FURNACE.get(), 1),
                SizedIngredient.of(ModItems.STEEL_PLATE.get(), 6), SizedIngredient.of(ModItems.STONE_BRICK.get(), 10));
        add(context, "underground_pipe", stack(ModItems.UNDERGROUND_PIPE.get(), 2), SizedIngredient.of(ModItems.PIPE.get(), 10), iron(5));
        add(context, "fluid_pump", stack(ModItems.FLUID_PUMP.get(), 1),
                SizedIngredient.of(ModItems.MOTOR.get(), 1), SizedIngredient.of(ModItems.STEEL_PLATE.get(), 1), SizedIngredient.of(ModItems.PIPE.get(), 1));
        add(context, "storage_tank", stack(ModItems.STORAGE_TANK.get(), 1), iron(20), SizedIngredient.of(ModItems.STEEL_PLATE.get(), 5));
        add(context, "solar_panel", stack(ModItems.SOLAR_PANEL.get(), 1),
                SizedIngredient.of(ModItems.STEEL_PLATE.get(), 5), SizedIngredient.of(ModItems.CIRCUIT.get(), 15), copper(5));

        // Logistics
        add(context, "long_inserter", stack(ModItems.LONG_INSERTER.get(), 1),
                SizedIngredient.of(ModItems.INSERTER.get(), 1), SizedIngredient.of(ModItems.IRON_GEAR.get(), 1), iron(1));
        add(context, "underground_belt", stack(ModItems.UNDERGROUND_BELT.get(), 2),
                iron(10), SizedIngredient.of(ModItems.CONVEYOR_BELT.get(), 5));
        add(context, "splitter", stack(ModItems.SPLITTER.get(), 1),
                SizedIngredient.of(ModItems.CIRCUIT.get(), 5), iron(5), SizedIngredient.of(ModItems.CONVEYOR_BELT.get(), 4));
        add(context, "fast_inserter", stack(ModItems.FAST_INSERTER.get(), 1),
                SizedIngredient.of(ModItems.INSERTER.get(), 1), SizedIngredient.of(ModItems.CIRCUIT.get(), 2), iron(2));
        add(context, "filter_inserter", stack(ModItems.FILTER_INSERTER.get(), 1),
                SizedIngredient.of(ModItems.FAST_INSERTER.get(), 1), SizedIngredient.of(ModItems.CIRCUIT.get(), 4));
        add(context, "fast_underground_belt", stack(ModItems.FAST_UNDERGROUND_BELT.get(), 2),
                SizedIngredient.of(ModItems.IRON_GEAR.get(), 40), SizedIngredient.of(ModItems.UNDERGROUND_BELT.get(), 2));
        add(context, "fast_splitter", stack(ModItems.FAST_SPLITTER.get(), 1),
                SizedIngredient.of(ModItems.SPLITTER.get(), 1), SizedIngredient.of(ModItems.IRON_GEAR.get(), 10),
                SizedIngredient.of(ModItems.CIRCUIT.get(), 10));

        // Turrets
        add(context, "arena_feeder", stack(ModItems.ARENA_FEEDER.get(), 1),
                iron(8), SizedIngredient.of(ModItems.COPPER_CABLE.get(), 4), SizedIngredient.of(ModItems.CIRCUIT.get(), 2),
                SizedIngredient.of(ItemTags.PLANKS, 8));
        add(context, "gun_turret", stack(ModItems.GUN_TURRET.get(), 1),
                SizedIngredient.of(ModItems.IRON_GEAR.get(), 10), copper(10), iron(20));
        add(context, "cartridge", stack(ModItems.CARTRIDGE.get(), 16), copper(1), iron(1), SizedIngredient.of(Items.COAL, 1));
        add(context, "tesla_tower", stack(ModItems.TESLA_TOWER.get(), 1),
                SizedIngredient.of(ModItems.COPPER_CABLE.get(), 24), iron(8), SizedIngredient.of(ModItems.CIRCUIT.get(), 4));

        // Caves and mines (reworked with the oil packages)
        add(context, "cave_entrance", stack(ModItems.CAVE_ENTRANCE.get(), 1),
                iron(16), SizedIngredient.of(ModItems.IRON_GEAR.get(), 8), SizedIngredient.of(ModItems.COPPER_CABLE.get(), 8));
        add(context, "elevator", stack(ModItems.ELEVATOR.get(), 2),
                iron(8), SizedIngredient.of(ModItems.MOTOR.get(), 2), SizedIngredient.of(ModItems.COPPER_CABLE.get(), 4));
        add(context, "fast_belt", stack(ModItems.FAST_BELT.get(), 1),
                SizedIngredient.of(ModItems.IRON_GEAR.get(), 5), SizedIngredient.of(ModItems.CONVEYOR_BELT.get(), 1));
        add(context, "mine_shaft", stack(ModItems.MINE_SHAFT.get(), 1),
                SizedIngredient.of(ModItems.LEAD_INGOT.get(), 16), SizedIngredient.of(ModItems.MOTOR.get(), 8),
                SizedIngredient.of(ModItems.ADVANCED_CIRCUIT.get(), 4), SizedIngredient.of(ModItems.BATTERY.get(), 4));
        add(context, "deep_drill", stack(ModItems.DEEP_DRILL.get(), 1),
                SizedIngredient.of(ModItems.ELECTRIC_DRILL.get(), 1), SizedIngredient.of(ModItems.TITANIUM_PLATE.get(), 12),
                SizedIngredient.of(ModItems.MOTOR.get(), 4), SizedIngredient.of(ModItems.ADVANCED_CIRCUIT.get(), 2));
        add(context, "express_belt", stack(ModItems.EXPRESS_BELT.get(), 4),
                SizedIngredient.of(ModItems.FAST_BELT.get(), 4), SizedIngredient.of(ModItems.TITANIUM_PLATE.get(), 2),
                SizedIngredient.of(ModItems.ADVANCED_CIRCUIT.get(), 1));
        add(context, "reactor", stack(ModItems.REACTOR.get(), 1),
                SizedIngredient.of(ModItems.TITANIUM_PLATE.get(), 20), SizedIngredient.of(ModItems.LEAD_INGOT.get(), 32),
                SizedIngredient.of(ModItems.ADVANCED_CIRCUIT.get(), 8), SizedIngredient.of(ModItems.BATTERY.get(), 8));
        add(context, "laser_tower", stack(ModItems.LASER_TOWER.get(), 1),
                SizedIngredient.of(ModItems.TITANIUM_PLATE.get(), 12), SizedIngredient.of(ModItems.ENERGY_CRYSTAL.get(), 2),
                SizedIngredient.of(ModItems.ADVANCED_CIRCUIT.get(), 4), SizedIngredient.of(ModItems.BATTERY.get(), 4));
    }

    /** Iron plates are iron ingots. */
    private static SizedIngredient iron(int count) {
        return SizedIngredient.of(Items.IRON_INGOT, count);
    }

    private static SizedIngredient copper(int count) {
        return SizedIngredient.of(Items.COPPER_INGOT, count);
    }

    private static void add(BootstrapContext<Blueprint> context, String name, ItemStack result, SizedIngredient... ingredients) {
        context.register(ResourceKey.create(ModRegistries.BLUEPRINTS, id(name)), new Blueprint(result, List.of(ingredients), order++));
    }

    private static ResourceLocation id(String name) {
        return Craftorio.id(name);
    }

    private static ItemStack stack(ItemLike item, int count) {
        return new ItemStack(item, count);
    }
}
