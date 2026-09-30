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
    private static final List<Pack> R_G_B = List.of(Pack.RED, Pack.GREEN, Pack.BLUE);
    private static final List<Pack> R_G_B_P = List.of(Pack.RED, Pack.GREEN, Pack.BLUE, Pack.PRODUCTION);
    private static final List<Pack> R_G_B_P_U = List.of(Pack.RED, Pack.GREEN, Pack.BLUE, Pack.PRODUCTION, Pack.UTILITY);
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
        add(context, "engines", 100, 15, R_G, List.of("steel_processing", "logistic_science_pack"), List.of("assembling/motor"));
        add(context, "automation_2", 40, 15, R_G, List.of("electronics", "steel_processing", "logistic_science_pack"), List.of("assembler_2"));
        add(context, "electric_energy_distribution_1", 120, 30, R_G, List.of("electronics", "steel_processing", "logistic_science_pack"),
                List.of("medium_power_pole"));
        add(context, "advanced_material_processing", 75, 30, R_G, List.of("steel_processing", "logistic_science_pack"), List.of("steel_furnace"));
        add(context, "fluid_handling", 50, 15, R_G, List.of("automation_2", "engines"),
                List.of("fluid_pump", "storage_tank", "underground_pipe", "assembling/fluid_pump", "assembling/storage_tank", "assembling/underground_pipe"));
        add(context, "optics", 10, 15, List.of("electronics"), List.of("lamp"));
        add(context, "solar_energy", 100, 30, R_G, List.of("electronics", "steel_processing", "logistic_science_pack"), List.of("solar_panel"));
        add(context, "energy_turrets", 100, 30, List.of("turrets", "engines"), List.of("tesla_tower"));
        add(context, "oil_processing", 100, 30, R_G, List.of("fluid_handling"),
                List.of("pumpjack", "oil_refinery", "chemical_plant", "assembling/pumpjack", "assembling/chemical_plant",
                        "oil/basic_oil_processing", "chem/solid_fuel_from_petroleum_gas", "cave_entrance"));
        add(context, "plastics", 200, 30, R_G, List.of("oil_processing"), List.of("chem/plastic_bar"));
        add(context, "sulfur_processing", 150, 30, R_G, List.of("oil_processing"), List.of("chem/sulfur", "chem/sulfuric_acid"));
        add(context, "advanced_electronics", 200, 30, R_G, List.of("plastics"), List.of("assembling/advanced_circuit"));
        add(context, "battery", 150, 30, R_G, List.of("sulfur_processing"), List.of("chem/battery"));
        add(context, "military_2", 20, 15, R_G, List.of("turrets", "logistic_science_pack"),
                List.of("ap_magazine", "grenade", "assembling/ap_magazine", "assembling/grenade"));
        add(context, "stone_walls", 10, 10, List.of(), List.of("stone_wall", "assembling/stone_wall"));
        // Tower technology (T3): upgrade tiers 3, 4 and 5 of every tower need these (replacing the XP of Bloons TD 6).
        add(context, "tower_tech_1", 75, 30, R_G, List.of("turrets", "logistic_science_pack"), List.of());
        add(context, "military_science_pack", 30, 15, R_G, List.of("military_2", "stone_walls"),
                List.of("military_science", "assembling/military_science"), key(ModItems.SILVER_SEAL.get()));
        add(context, "tower_tech_2", 150, 30, List.of(Pack.RED, Pack.GREEN, Pack.MILITARY), List.of("tower_tech_1", "military_science_pack"), List.of());
        add(context, "flammables", 50, 30, List.of(Pack.RED, Pack.GREEN, Pack.MILITARY), List.of("fluid_handling", "military_science_pack"),
                List.of("flamethrower_turret"));
        add(context, "lasers", 100, 30, List.of(Pack.RED, Pack.GREEN, Pack.BLUE), List.of("chemical_science_pack", "battery"), List.of());
        add(context, "tower_tech_3", 300, 30, List.of(Pack.RED, Pack.GREEN, Pack.MILITARY, Pack.BLUE), List.of("tower_tech_2", "chemical_science_pack"), List.of());
        add(context, "laser_turrets", 150, 30, List.of(Pack.RED, Pack.GREEN, Pack.MILITARY), List.of("lasers", "military_science_pack"),
                List.of("laser_tower"));
        add(context, "agriculture", 30, 15, List.of(), List.of("greenhouse", "farm/wheat", "farm/carrot", "farm/potato",
                "farm/pumpkin", "farm/sugar_cane", "farm/tree"));
        add(context, "bio_fuel", 50, 30, R_G, List.of("agriculture", "logistic_science_pack"), List.of("assembling/bio_fuel"));
        // Corrections and additions of package U11a (costs and prerequisites from the Factorio 1.1 wiki)
        add(context, "advanced_material_processing_2", 250, 30, R_G_B, List.of("advanced_material_processing", "chemical_science_pack"), List.of("electric_furnace"));
        add(context, "advanced_oil_processing", 75, 30, R_G_B, List.of("chemical_science_pack"),
                List.of("oil/advanced_oil_processing", "chem/heavy_oil_cracking", "chem/light_oil_cracking",
                        "chem/solid_fuel_from_heavy_oil", "chem/solid_fuel_from_light_oil"));
        add(context, "lubricant", 50, 30, R_G_B, List.of("advanced_oil_processing"), List.of("chem/lubricant"));
        add(context, "concrete", 250, 30, R_G, List.of("advanced_material_processing", "automation_2"), List.of("assembling/concrete"));
        add(context, "railway", 75, 30, R_G, List.of("engines", "fast_belts"), List.of("rail", "assembling/rail"));
        add(context, "electric_engine", 50, 30, R_G_B, List.of("lubricant"), List.of("assembling/electric_engine"));
        add(context, "robotics", 75, 30, R_G_B, List.of("electric_engine", "battery"), List.of("flying_robot_frame", "assembling/flying_robot_frame"));
        add(context, "advanced_electronics_2", 300, 30, R_G_B, List.of("advanced_electronics", "chemical_science_pack"), List.of("assembling/processing_unit"));
        add(context, "low_density_structure", 300, 45, R_G_B, List.of("advanced_material_processing", "chemical_science_pack"),
                List.of("low_density_structure", "assembling/low_density_structure"));
        add(context, "mine_shaft", 300, 30, R_G_B, List.of("elevators", "chemical_science_pack"), List.of("mine_shaft"), key(ModItems.PLATINUM_SEAL.get()));
        add(context, "deep_mining", 200, 30, R_G_B, List.of("mine_shaft"), List.of("deep_drill"));
        // Purple and yellow science (U11e): each first research hands in the seal of level 40 or 50
        add(context, "production_science_pack", 100, 30, R_G_B, List.of("productivity_module", "advanced_material_processing_2", "railway"),
                List.of("assembling/production_science"), key(ModItems.DIAMOND_SEAL.get()));
        add(context, "utility_science_pack", 100, 30, R_G_B, List.of("robotics", "advanced_electronics_2", "low_density_structure"),
                List.of("assembling/utility_science"), key(ModItems.STAR_SEAL.get()));
        add(context, "logistics_3", 300, 15, R_G_B_P, List.of("fast_belts", "lubricant", "production_science_pack"), List.of("assembling/express_belt"));
        add(context, "mining_productivity_1", 250, 60, R_G, List.of("advanced_electronics"), List.of());
        add(context, "mining_productivity_2", 500, 60, R_G_B, List.of("mining_productivity_1", "chemical_science_pack"), List.of());
        add(context, "mining_productivity_3", 1000, 60, R_G_B_P_U, List.of("mining_productivity_2", "production_science_pack", "utility_science_pack"), List.of());
        // Modules (U11d). Production science joins the costs of module 3, automation 3 and effect transmission with U11e.
        add(context, "modules", 100, 30, R_G, List.of("advanced_electronics"), List.of());
        for (String kind : List.of("speed", "efficiency", "productivity")) {
            add(context, kind + "_module", 50, 30, R_G, List.of("modules"), List.of("assembling/" + kind + "_module_1"));
            add(context, kind + "_module_2", 75, 30, R_G_B, List.of("advanced_electronics_2", kind + "_module"), List.of("assembling/" + kind + "_module_2"));
            add(context, kind + "_module_3", 300, 60, R_G_B_P, List.of(kind + "_module_2", "production_science_pack"), List.of("assembling/" + kind + "_module_3"));
        }
        add(context, "automation_3", 150, 60, R_G_B_P, List.of("speed_module", "production_science_pack"), List.of("assembler_3"));
        add(context, "effect_transmission", 75, 30, R_G_B_P, List.of("advanced_electronics_2", "production_science_pack"), List.of("beacon"));
        // Uranium (U11b). Production/utility packs join the costs with U11e; Kovarex and reprocessing still lack the production pack.
        add(context, "uranium_processing", 200, 30, R_G_B, List.of("chemical_science_pack", "concrete", "mine_shaft"),
                List.of("centrifuge", "centrifuge/uranium_processing", "assembling/uranium_fuel_cell"));
        add(context, "nuclear_power", 800, 30, R_G_B, List.of("uranium_processing"), List.of("reactor", "heat_pipe", "heat_exchanger", "steam_turbine"));
        // Wiki 1.1 also asks for rocket fuel before Kovarex; there is no rocket fuel here, so nuclear power takes its place.
        add(context, "kovarex_enrichment_process", 1500, 30, R_G_B_P, List.of("uranium_processing", "nuclear_power", "production_science_pack"), List.of("centrifuge/kovarex_enrichment"));
        add(context, "nuclear_fuel_reprocessing", 50, 30, R_G_B_P, List.of("nuclear_power", "production_science_pack"), List.of("centrifuge/nuclear_fuel_reprocessing"));
        add(context, "uranium_ammo", 1000, 45, List.of(Pack.RED, Pack.GREEN, Pack.MILITARY, Pack.BLUE, Pack.UTILITY), List.of("uranium_processing", "military_science_pack", "utility_science_pack"),
                List.of("assembling/uranium_magazine"));
        add(context, "electric_energy_accumulators", 150, 30, R_G, List.of("battery", "electric_energy_distribution_1"),
                List.of("accumulator", "assembling/accumulator"));
        add(context, "chemical_science_pack", 75, 10, R_G, List.of("advanced_electronics", "sulfur_processing"),
                List.of("chemical_science", "assembling/blue_science"), key(ModItems.GOLD_SEAL.get()));
        add(context, "elevators", 50, 15, List.of("oil_processing"), List.of("elevator"));
        add(context, "fast_belts", 200, 30, R_G, List.of("logistics", "logistic_science_pack"), List.of("fast_belt", "fast_underground_belt", "fast_splitter"));
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
