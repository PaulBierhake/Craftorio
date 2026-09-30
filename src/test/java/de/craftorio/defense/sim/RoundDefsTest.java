package de.craftorio.defense.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundDefsTest {
    @Test
    void everyRoundHasTheRbeOfTheWikiList() {
        assertEquals(140, RoundDefs.count());
        for (int round = 1; round <= RoundDefs.count(); round++) {
            RoundDef def = RoundDefs.get(round);
            assertEquals(round, def.round());
            if (round <= 100) {
                assertEquals(def.rbe(), RoundDefs.rbe(def), 0.5, "round " + round);
            }
        }
    }

    @Test
    void referenceRounds() {
        assertEquals(20, RoundDefs.get(1).rbe());
        assertEquals(2_289, RoundDefs.get(45).rbe());
        assertEquals(3_540, RoundDefs.get(50).rbe());
        assertEquals(3_164, RoundDefs.get(60).rbe());
        assertEquals(16_656, RoundDefs.get(80).rbe());
        assertEquals(67_200, RoundDefs.get(100).rbe());
        assertEquals(List.of(new RoundDef.Group("red_crawler", 20, false, false, false)), RoundDefs.get(1).groups());
        assertEquals(616, RoundDefs.get(40).rbe());
    }

    @Test
    void firstAppearances() {
        assertEquals(17, first(g -> g.regrow()));
        assertEquals(24, first(g -> g.camo()));
        assertEquals(20, first(g -> g.enemy().equals("soot_crawler")));
        assertEquals(22, first(g -> g.enemy().equals("frost_crawler")));
        assertEquals(25, first(g -> g.enemy().equals("ember_crawler")));
        assertEquals(26, first(g -> g.enemy().equals("twilight_crawler")));
        assertEquals(28, first(g -> g.enemy().equals("ironbreaker")));
        assertEquals(35, first(g -> g.enemy().equals("shimmer_crawler")));
        assertEquals(38, first(g -> g.enemy().equals("crystal_golem")));
        assertEquals(40, first(g -> g.enemy().equals("brood_mother")));
        assertEquals(45, first(g -> g.fortified()));
        assertEquals(60, first(g -> g.enemy().equals("behemoth")));
        assertEquals(80, first(g -> g.enemy().equals("colossus")));
        assertEquals(90, first(g -> g.enemy().equals("shadow_hunter")));
        assertEquals(100, first(g -> g.enemy().equals("swarm_queen")));
    }

    private static int first(java.util.function.Predicate<RoundDef.Group> test) {
        for (int round = 1; round <= RoundDefs.count(); round++) {
            if (RoundDefs.get(round).groups().stream().anyMatch(test)) {
                return round;
            }
        }
        return -1;
    }

    @Test
    void spawnsAreSpreadOverTheDuration() {
        RoundDef round = RoundDefs.get(1);
        List<RoundDef.Spawn> spawns = round.spawns();
        assertEquals(20, spawns.size());
        assertEquals(0, spawns.get(0).tick());
        assertEquals(round.durationTicks(), spawns.get(19).tick());
        assertEquals(350, round.durationTicks(), 1);
        for (int i = 1; i < spawns.size(); i++) {
            assertTrue(spawns.get(i).tick() >= spawns.get(i - 1).tick());
        }
        assertEquals(0, RoundDefs.get(100).spawns().get(0).tick());
        assertEquals(RoundDefs.get(140).round(), RoundDefs.get(500).round(), "past the list the last round repeats");
    }
}
