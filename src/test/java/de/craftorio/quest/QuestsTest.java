package de.craftorio.quest;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestsTest {
    @Test
    void progressIsMeasuredPerKindAndCapped() {
        Quest.Progress progress = new Quest.Progress(12_345, Map.of("minecraft:raw_iron", 100L), Set.of("craftorio:press"), 3);

        assertTrue(Quests.byId("first_sale").orElseThrow().done(progress));
        assertEquals(64, Quests.byId("sell_raw_iron").orElseThrow().progress(progress), "capped at the goal");
        assertTrue(Quests.byId("unlock_press").orElseThrow().done(progress));
        assertFalse(Quests.byId("unlock_coal_generator").orElseThrow().done(progress));
        assertEquals(3, Quests.byId("td_5").orElseThrow().progress(progress));
        assertTrue(Quests.byId("earn_10k").orElseThrow().done(progress));
    }

    @Test
    void questsHaveUniqueIdsPositiveGoalsAndGrowingRewards() {
        Set<String> ids = new HashSet<>();
        for (Quest quest : Quests.ALL) {
            assertTrue(ids.add(quest.id()), "duplicate " + quest.id());
            assertTrue(quest.amount() > 0 && quest.reward() > 0, quest.id());
        }
        Quest first = Quests.ALL.get(0);
        Quest last = Quests.ALL.get(Quests.ALL.size() - 1);
        assertTrue(last.reward() > first.reward());
        assertFalse(first.done(new Quest.Progress(0, Map.of(), Set.of(), 0)));
    }
}
