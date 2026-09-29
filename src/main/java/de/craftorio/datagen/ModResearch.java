package de.craftorio.datagen;

import de.craftorio.Craftorio;
import de.craftorio.registry.ModItems;
import de.craftorio.registry.ModRegistries;
import de.craftorio.research.Research;
import de.craftorio.research.Research.Pack;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

/**
 * The research tree. Every blueprint that no research unlocks is available from the start.
 *
 * <p>Unlocked ids are blueprints (workbench recipes) or machine recipes such as {@code assembling/motor}.
 * This is the tree for the content that exists today; it grows with each package of the Factorio rework
 * (docs/FACTORIO-UMBAU.md §5), and the science pack costs follow that table.
 */
public final class ModResearch {
    private static final List<Pack> R_G = List.of(Pack.RED, Pack.GREEN);
    private static int order;

    private ModResearch() {
    }

    public static void bootstrap(BootstrapContext<Research> context) {
        order = 0;
        add(context, "automation", 10, 10, List.of(), List.of("assembler", "long_inserter"));
        add(context, "logistics", 75, 15, List.of(), List.of("underground_belt", "splitter"));
        add(context, "electronics", 30, 15, List.of("automation"), List.of());
        add(context, "fast_inserters", 30, 15, List.of("electronics"), List.of("fast_inserter", "filter_inserter"));
        add(context, "steel_processing", 50, 5, List.of(), List.of("smelting/steel_plate"));
        add(context, "logistic_science_pack", 75, 5, List.of(), List.of("green_science", "assembling/green_science"), key(ModItems.BRONZE_SEAL.get()));
        add(context, "turrets", 10, 10, List.of(), List.of("gun_turret", "magazine", "arena_feeder"));
        add(context, "electric_smelting", 50, 30, List.of("automation"), List.of("electric_furnace"));
        add(context, "engines", 100, 15, R_G, List.of("steel_processing", "logistic_science_pack"), List.of("assembling/motor"));
        add(context, "automation_2", 40, 15, R_G, List.of("electronics", "steel_processing", "logistic_science_pack"), List.of("assembler_2"));
        add(context, "electric_energy_distribution_1", 120, 30, R_G, List.of("electronics", "steel_processing", "logistic_science_pack"),
                List.of("medium_power_pole"));
        add(context, "advanced_material_processing", 75, 30, R_G, List.of("steel_processing", "logistic_science_pack"), List.of("steel_furnace"));
        add(context, "fluid_handling", 50, 15, R_G, List.of("automation_2", "engines"),
                List.of("fluid_pump", "storage_tank", "underground_pipe", "assembling/fluid_pump", "assembling/storage_tank", "assembling/underground_pipe"));
        add(context, "solar_energy", 100, 30, R_G, List.of("electronics", "steel_processing", "logistic_science_pack"), List.of("solar_panel"));
        add(context, "energy_turrets", 100, 30, List.of("turrets", "engines"), List.of("tesla_tower"));
        add(context, "oil_processing", 100, 30, R_G, List.of("fluid_handling"),
                List.of("pumpjack", "oil_refinery", "chemical_plant", "assembling/pumpjack", "assembling/chemical_plant",
                        "oil/basic_oil_processing", "cave_entrance"));
        add(context, "plastics", 200, 30, R_G, List.of("oil_processing"), List.of("chem/plastic_bar"));
        add(context, "sulfur_processing", 150, 30, R_G, List.of("oil_processing"), List.of("chem/sulfur", "chem/sulfuric_acid"));
        add(context, "advanced_electronics", 200, 30, R_G, List.of("plastics"), List.of("assembling/advanced_circuit"));
        add(context, "battery", 150, 30, R_G, List.of("sulfur_processing"), List.of("chem/battery"));
        add(context, "military_2", 20, 15, R_G, List.of("turrets", "logistic_science_pack"),
                List.of("ap_magazine", "grenade", "assembling/ap_magazine", "assembling/grenade"));
        add(context, "stone_walls", 10, 10, List.of(), List.of("stone_wall", "assembling/stone_wall"));
        add(context, "military_science_pack", 30, 15, R_G, List.of("military_2", "stone_walls"),
                List.of("military_science", "assembling/military_science"), key(ModItems.SILVER_SEAL.get()));
        add(context, "flammables", 50, 30, List.of(Pack.RED, Pack.GREEN, Pack.MILITARY), List.of("fluid_handling", "military_science_pack"),
                List.of("flamethrower_turret"));
        add(context, "lasers", 100, 30, List.of(Pack.RED, Pack.GREEN, Pack.BLUE), List.of("chemical_science_pack", "battery"), List.of());
        add(context, "laser_turrets", 150, 30, List.of(Pack.RED, Pack.GREEN, Pack.MILITARY), List.of("lasers", "military_science_pack"),
                List.of("laser_tower"));
        add(context, "electric_energy_accumulators", 150, 30, R_G, List.of("battery", "electric_energy_distribution_1"),
                List.of("accumulator", "assembling/accumulator"));
        add(context, "chemical_science_pack", 75, 10, R_G, List.of("advanced_electronics", "sulfur_processing"),
                List.of("chemical_science", "assembling/blue_science"), key(ModItems.GOLD_SEAL.get()));
        add(context, "elevators", 50, 15, List.of("oil_processing"), List.of("elevator"));
        add(context, "fast_belts", 200, 30, R_G, List.of("logistics", "logistic_science_pack"), List.of("fast_belt", "fast_underground_belt", "fast_splitter"));
        add(context, "mine_shaft", 300, 30, List.of("elevators"), List.of("mine_shaft"), key(ModItems.PLATINUM_SEAL.get()));
        add(context, "deep_mining", 200, 30, List.of("mine_shaft"), List.of("deep_drill"));
        add(context, "express_belts", 150, 30, List.of("fast_belts", "mine_shaft"), List.of("express_belt"));
        add(context, "nuclear_power", 300, 30, List.of("mine_shaft"), List.of("reactor"));
    }

    private static SizedIngredient key(ItemLike item) {
        return SizedIngredient.of(item, 1);
    }

    private static void add(BootstrapContext<Research> context, String name, long units, int seconds, List<String> requires,
                            List<String> unlocks, SizedIngredient... unlockItems) {
        add(context, name, units, seconds, List.of(Pack.RED), requires, unlocks, unlockItems);
    }

    private static void add(BootstrapContext<Research> context, String name, long units, int seconds, List<Pack> packs,
                            List<String> requires, List<String> unlocks, SizedIngredient... unlockItems) {
        context.register(ResourceKey.create(ModRegistries.RESEARCH, id(name)),
                new Research(units, packs, seconds, requires.stream().map(ModResearch::id).toList(),
                        unlocks.stream().map(ModResearch::id).toList(), List.of(unlockItems), order++));
    }

    private static ResourceLocation id(String name) {
        return Craftorio.id(name);
    }
}
