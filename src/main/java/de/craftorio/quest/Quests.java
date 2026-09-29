package de.craftorio.quest;

import java.util.List;
import java.util.Optional;

/**
 * The guide ("Leitfaden") from the first sale to the mine layer. Each quest pays out credits once; the order is the
 * suggested path through the game, but every goal counts as soon as it is reached.
 */
public final class Quests {
    public static final List<Quest> ALL = List.of(
            // By hand to the first drill and the first power
            mine("raw_iron", "minecraft:raw_iron", 16, 0).withItem("minecraft:coal", 8),
            mine("cobblestone", "minecraft:cobblestone", 20, 10),
            mine("raw_copper", "minecraft:raw_copper", 16, 0).withItem("minecraft:coal", 8),
            mine("oak_log", "minecraft:oak_log", 10, 10),
            build("stone_furnace", 1, 0).withItem("craftorio:iron_gear", 3),
            build("burner_drill", 1, 0).withItem("minecraft:coal", 16),
            build("conveyor_belt", 1, 0).withItem("craftorio:conveyor_belt", 8),
            build("inserter", 1, 0).withItem("craftorio:circuit", 2),
            build("boiler", 1, 0).withItem("craftorio:pipe", 4),
            build("offshore_pump", 1, 10),
            build("steam_engine", 1, 0).withItem("craftorio:iron_gear", 4),
            build("power_pole", 1, 0).withItem("craftorio:copper_cable", 4),
            // Science and automation
            build("laboratory", 1, 0).withItem("craftorio:red_science", 5),
            build("red_science", 10, 0).withItem("craftorio:circuit", 5),
            unlock("assembler", 0).withItem("craftorio:iron_gear", 10),
            build("assembler", 1, 0).withItem("craftorio:circuit", 5),
            build("terminal", 1, 10),
            unlock("underground_belt", 0).withItem("craftorio:conveyor_belt", 8),
            build("splitter", 1, 0).withItem("craftorio:circuit", 3),
            // Trade, then defend
            build("trading_post", 1, 20),
            earn("first_sale", 1, 20),
            build("arena_gate", 1, 50),
            build("crossbow_tower", 1, 0).withItem("craftorio:bolt", 32),
            tdLevel("td_1", 1, 100),
            unlock("gun_turret", 0).withItem("craftorio:magazine", 10),
            unlock("arena_feeder", 30),
            build("arena_feeder", 1, 60),
            tdLevel("td_5", 5, 200),
            earn("earn_10k", 10_000, 100),
            // Green science and steel
            unlock("green_science", 0).withItem("craftorio:red_science", 10),
            build("green_science", 10, 0).withItem("craftorio:circuit", 5),
            unlock("steel", "craftorio:smelting/steel_plate", 0).withItem("minecraft:iron_ingot", 10),
            unlock("engine", "craftorio:assembling/motor", 0).withItem("craftorio:iron_gear", 10),
            unlock("assembler_2", 0).withItem("craftorio:steel_plate", 2),
            unlock("medium_power_pole", 0).withItem("craftorio:steel_plate", 2),
            unlock("solar_panel", 50),
            // Fluids and oil
            unlock("fluid_pump", 0).withItem("craftorio:pipe", 10),
            unlock("cave_entrance", 100).withItem("craftorio:pipe", 10),
            build("cave_entrance", 1, 150),
            build("pumpjack", 1, 50),
            build("oil_refinery", 1, 50),
            build("chemical_plant", 1, 50),
            unlock("plastics", "craftorio:chem/plastic_bar", 50),
            sell("plastic_bar", "craftorio:plastic_bar", 64, 200),
            unlock("battery", "craftorio:chem/battery", 50),
            sell("battery", "craftorio:battery", 32, 300),
            unlock("advanced_circuit", "craftorio:assembling/advanced_circuit", 50),
            unlock("accumulator", 50),
            // Plants
            unlock("greenhouse", 0).withItem("craftorio:pipe", 5),
            build("greenhouse", 1, 30),
            unlock("bio_fuel", "craftorio:assembling/bio_fuel", 50),
            tdLevel("td_10", 10, 500),
            // Military and blue science
            unlock("military_science", 100),
            unlock("flamethrower_turret", 100),
            unlock("chemical_science", 200),
            tdLevel("td_20", 20, 1_000),
            unlock("elevator", 100),
            // Mines
            tdLevel("td_30", 30, 2_000),
            unlock("mine_shaft", 500),
            sell("steel_plate", "craftorio:steel_plate", 64, 500),
            sell("advanced_circuit", "craftorio:advanced_circuit", 8, 1_000),
            earn("earn_1m", 1_000_000, 5_000),
            tdLevel("td_40", 40, 5_000));

    /** The first quest whose reward has not been collected yet: what the player should do next. */
    public static java.util.Optional<Integer> current(java.util.Set<String> claimed) {
        for (int i = 0; i < ALL.size(); i++) {
            if (!claimed.contains(ALL.get(i).id())) {
                return java.util.Optional.of(i);
            }
        }
        return java.util.Optional.empty();
    }

    private Quests() {
    }

    public static Optional<Quest> byId(String id) {
        return ALL.stream().filter(quest -> quest.id().equals(id)).findFirst();
    }

    private static Quest earn(String id, long amount, long reward) {
        return new Quest(id, Quest.Kind.EARN, "", amount, reward);
    }

    private static Quest mine(String id, String item, long amount, long reward) {
        return new Quest("mine_" + id, Quest.Kind.MINE, item, amount, reward);
    }

    private static Quest sell(String id, String item, long amount, long reward) {
        return new Quest("sell_" + id, Quest.Kind.SELL, item, amount, reward);
    }

    private static Quest unlock(String blueprint, long reward) {
        return new Quest("unlock_" + blueprint, Quest.Kind.UNLOCK, "craftorio:" + blueprint, 1, reward);
    }

    /** A research goal named by what it unlocks: a recipe id that has no blueprint of its own. */
    private static Quest unlock(String id, String unlockable, long reward) {
        return new Quest("unlock_" + id, Quest.Kind.UNLOCK, unlockable, 1, reward);
    }

    private static Quest build(String blueprint, long times, long reward) {
        return new Quest("build_" + blueprint, Quest.Kind.BUILD, "craftorio:" + blueprint, times, reward);
    }

    private static Quest tdLevel(String id, int level, long reward) {
        return new Quest(id, Quest.Kind.TD_LEVEL, "", level, reward);
    }
}
