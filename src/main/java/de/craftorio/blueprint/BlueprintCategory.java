package de.craftorio.blueprint;

import java.util.HashMap;
import java.util.Map;

/** The tabs of the workbench: what a blueprint is for. */
public enum BlueprintCategory {
    PARTS, LOGISTICS, PRODUCTION, POWER, FLUIDS, DEFENSE;

    private static final Map<String, BlueprintCategory> BY_ID = new HashMap<>();

    static {
        add(PARTS, "iron_gear", "copper_cable", "circuit", "iron_stick", "pipe", "red_science", "green_science", "military_science",
                "chemical_science", "rail", "flying_robot_frame", "low_density_structure");
        add(LOGISTICS, "conveyor_belt", "fast_belt", "underground_belt", "fast_underground_belt", "splitter", "fast_splitter", "inserter",
                "long_inserter", "fast_inserter", "filter_inserter", "elevator", "trading_post", "terminal");
        add(PRODUCTION, "stone_furnace", "steel_furnace", "electric_furnace", "burner_drill", "electric_drill", "deep_drill", "assembler",
                "assembler_2", "assembler_3", "laboratory", "greenhouse", "centrifuge", "beacon", "cave_entrance", "mine_shaft");
        add(POWER, "boiler", "steam_engine", "offshore_pump", "power_pole", "medium_power_pole", "solar_panel", "accumulator", "reactor",
                "heat_pipe", "heat_exchanger", "steam_turbine", "lamp");
        add(FLUIDS, "underground_pipe", "fluid_pump", "storage_tank", "pumpjack", "oil_refinery", "chemical_plant");
        add(DEFENSE, "arena_gate", "arena_feeder", "crossbow_tower", "gun_turret", "tesla_tower", "laser_tower", "flamethrower_turret", "mortar_turret", "supply_depot", "frost_tower", "glue_turret", "command_post",
                "stone_wall", "bolt", "magazine", "ap_magazine", "grenade");
    }

    private static void add(BlueprintCategory category, String... ids) {
        for (String id : ids) {
            BY_ID.put(id, category);
        }
    }

    /** The category of the blueprint with this id path; unknown ones count as production. */
    public static BlueprintCategory of(String path) {
        return BY_ID.getOrDefault(path, PRODUCTION);
    }

    public static boolean isListed(String path) {
        return BY_ID.containsKey(path);
    }
}
