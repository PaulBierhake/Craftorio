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
            mine("raw_iron", "minecraft:raw_iron", 16, 20),
            mine("cobblestone", "minecraft:cobblestone", 20, 20),
            build("stone_furnace", 1, 20),
            build("burner_drill", 1, 40),
            build("conveyor_belt", 1, 20),
            build("boiler", 1, 30),
            build("offshore_pump", 1, 30),
            build("steam_engine", 1, 40),
            build("power_pole", 1, 20),
            // Science and automation
            build("laboratory", 1, 60),
            build("red_science", 10, 60),
            build("inserter", 1, 30),
            unlock("assembler", 100),
            build("assembler", 1, 100),
            // Trade, then defend
            build("trading_post", 1, 40),
            earn("first_sale", 1, 40),
            build("arena_gate", 1, 100),
            build("crossbow_tower", 1, 100),
            tdLevel("td_1", 1, 200),
            unlock("arena_feeder", 100),
            build("arena_feeder", 1, 150),
            earn("earn_10k", 10_000, 300),
            tdLevel("td_10", 10, 1_000),
            // Caves
            unlock("cave_entrance", 500),
            build("cave_entrance", 1, 500),
            sell("tin_ingot", "craftorio:tin_ingot", 64, 1_000),
            sell("battery", "craftorio:battery", 32, 1_500),
            unlock("elevator", 300),
            tdLevel("td_20", 20, 2_500),
            // Mines
            tdLevel("td_30", 30, 5_000),
            unlock("mine_shaft", 3_000),
            sell("titanium_plate", "craftorio:titanium_plate", 64, 4_000),
            sell("energy_crystal", "craftorio:energy_crystal", 8, 7_500),
            earn("earn_1m", 1_000_000, 25_000),
            tdLevel("td_40", 40, 25_000));

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

    private static Quest build(String blueprint, long times, long reward) {
        return new Quest("build_" + blueprint, Quest.Kind.BUILD, "craftorio:" + blueprint, times, reward);
    }

    private static Quest tdLevel(String id, int level, long reward) {
        return new Quest(id, Quest.Kind.TD_LEVEL, "", level, reward);
    }
}
