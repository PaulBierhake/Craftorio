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
        Quest.Progress progress = new Quest.Progress(12_345, Map.of("minecraft:raw_iron", 100L), Set.of("craftorio:press"),
                Map.of("craftorio:burner_drill", 2L), 3);

        assertTrue(Quests.byId("first_sale").orElseThrow().done(progress));
        assertEquals(64, Quests.byId("sell_raw_iron").orElseThrow().progress(progress), "capped at the goal");
        assertTrue(Quests.byId("unlock_press").orElseThrow().done(progress));
        assertFalse(Quests.byId("unlock_coal_generator").orElseThrow().done(progress));
        assertEquals(1, Quests.byId("td_10").orElseThrow().progress(progress) > 0 ? 1 : 0);
        assertTrue(Quests.byId("build_burner_drill").orElseThrow().done(progress));
        assertFalse(Quests.byId("build_trading_post").orElseThrow().done(progress));
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
        assertFalse(first.done(new Quest.Progress(0, Map.of(), Set.of(), Map.of(), 0)));
    }

    @Test
    void currentQuestIsTheFirstUncollectedOne() {
        assertEquals(0, Quests.current(Set.of()).orElseThrow());
        assertEquals(1, Quests.current(Set.of(Quests.ALL.get(0).id())).orElseThrow());
        Set<String> all = new HashSet<>();
        Quests.ALL.forEach(quest -> all.add(quest.id()));
        assertTrue(Quests.current(all).isEmpty());
    }
}
