package de.craftorio.quest;

import java.util.List;
import java.util.Optional;

/**
 * The guide ("Leitfaden") from the first sale to the mine layer. Each quest pays out credits once; the order is the
 * suggested path through the game, but every goal counts as soon as it is reached.
 */
public final class Quests {
    public static final List<Quest> ALL = List.of(
            earn("first_sale", 1, 100),
            sell("raw_iron", "minecraft:raw_iron", 64, 150),
            unlock("coal_generator", 200),
            unlock("press", 300),
            sell("iron_plate", "craftorio:iron_plate", 100, 400),
            earn("earn_10k", 10_000, 1_000),
            tdLevel("td_5", 5, 1_000),
            unlock("workbench_upgrade_2", 1_500),
            unlock("assembler", 1_500),
            sell("motor", "craftorio:motor", 20, 2_000),
            unlock("cave_entrance", 3_000),
            sell("tin_ingot", "craftorio:tin_ingot", 64, 2_000),
            sell("battery", "craftorio:battery", 32, 3_000),
            tdLevel("td_20", 20, 5_000),
            unlock("mine_shaft", 10_000),
            sell("titanium_plate", "craftorio:titanium_plate", 64, 8_000),
            sell("energy_crystal", "craftorio:energy_crystal", 8, 15_000),
            earn("earn_1m", 1_000_000, 50_000),
            tdLevel("td_40", 40, 50_000));

    private Quests() {
    }

    public static Optional<Quest> byId(String id) {
        return ALL.stream().filter(quest -> quest.id().equals(id)).findFirst();
    }

    private static Quest earn(String id, long amount, long reward) {
        return new Quest(id, Quest.Kind.EARN, "", amount, reward);
    }

    private static Quest sell(String id, String item, long amount, long reward) {
        return new Quest("sell_" + id, Quest.Kind.SELL, item, amount, reward);
    }

    private static Quest unlock(String blueprint, long reward) {
        return new Quest("unlock_" + blueprint, Quest.Kind.UNLOCK, "craftorio:" + blueprint, 1, reward);
    }

    private static Quest tdLevel(String id, int level, long reward) {
        return new Quest(id, Quest.Kind.TD_LEVEL, "", level, reward);
    }
}
