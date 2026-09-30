package de.craftorio.defense.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TdSimulationTest {
    private static TdPath straight(double length) {
        return new TdPath(List.of(new double[]{0, 0, 0}, new double[]{length, 0, 0}));
    }

    private static TdSimulation sim() {
        return new TdSimulation(straight(150));
    }

    /** RBE of every enemy kind (bloons wiki, version 56.3). */
    private static final Map<String, Integer> RBE = Map.ofEntries(
            Map.entry("red_crawler", 1), Map.entry("blue_crawler", 2), Map.entry("green_crawler", 3), Map.entry("yellow_crawler", 4),
            Map.entry("pink_crawler", 5), Map.entry("soot_crawler", 11), Map.entry("frost_crawler", 11), Map.entry("ember_crawler", 11),
            Map.entry("ironbreaker", 23), Map.entry("twilight_crawler", 23), Map.entry("shimmer_crawler", 47), Map.entry("crystal_golem", 104),
            Map.entry("brood_mother", 616), Map.entry("behemoth", 3164), Map.entry("colossus", 16656), Map.entry("shadow_hunter", 816),
            Map.entry("swarm_queen", 55760));

    /** Fortified RBE: lead 26, ceramic 114, fortified MOAB-class ones pass it on to their children. */
    private static final Map<String, Integer> FORTIFIED_RBE = Map.of("ironbreaker", 26, "crystal_golem", 114, "brood_mother", 856,
            "behemoth", 4824, "colossus", 27296, "shadow_hunter", 1256, "swarm_queen", 98360);

    @Test
    void everyEnemyHasTheRbeOfTheWiki() {
        assertEquals(EnemyDefs.IDS.size(), RBE.size());
        for (String id : EnemyDefs.IDS) {
            assertEquals((double) RBE.get(id), EnemyDefs.rbe(EnemyDefs.get(id), false, false), id);
            assertEquals(EnemyDefs.get(id).index(), EnemyDefs.IDS.indexOf(id), id);
        }
        FORTIFIED_RBE.forEach((id, rbe) -> assertEquals((double) rbe, EnemyDefs.rbe(EnemyDefs.get(id), false, true), "fortified " + id));
    }

    @Test
    void speedsAreTheWikiValuesInBlocks() {
        assertEquals(0.125, EnemyDefs.get("red_crawler").blocksPerTick(), 1e-9);
        assertEquals(2.5, EnemyDefs.get("red_crawler").blocksPerTick() * 20, 1e-9);
        assertEquals(62.5, EnemyDefs.get("crystal_golem").speed(), 0);
        assertEquals(10, TdUnits.UNITS_PER_BLOCK, 0);
        TdSimulation sim = sim();
        SimEnemy red = sim.spawn("red_crawler");
        for (int i = 0; i < 20; i++) {
            sim.tick();
        }
        assertEquals(2.5, red.distance(), 1e-9);
        assertEquals(2.5, red.x(), 1e-9);
    }

    @Test
    void peelingAPinkCrawlerDownTheLayers() {
        TdSimulation sim = sim();
        SimEnemy pink = sim.spawn("pink_crawler");
        assertEquals(5, pink.rbe(), 0);
        assertEquals(1, sim.hit(pink, 1, DamageKind.SHARP));
        assertEquals(1, sim.count());
        SimEnemy now = sim.enemies().get(0);
        assertEquals("yellow_crawler", now.def().id());
        assertEquals(4, now.rbe(), 0);
        assertEquals(4, sim.hit(now, 10, DamageKind.SHARP), "the excess pops the rest");
        assertEquals(0, sim.count());
        assertEquals(5, sim.paidPops());
    }

    @Test
    void excessDamageGoesToEveryChildButNotFromMoabClass() {
        TdSimulation sim = sim();
        SimEnemy zebra = sim.spawn("twilight_crawler");
        assertEquals(EnemyDefs.layers(EnemyDefs.get("twilight_crawler"), false), sim.hit(zebra, 100, DamageKind.SHARP));
        assertEquals(0, sim.count());

        SimEnemy moab = sim.spawn("brood_mother");
        assertEquals(1, sim.hit(moab, 100_000, DamageKind.SHARP), "a MOAB pops once and keeps its excess");
        assertEquals(4, sim.count());
        assertTrue(sim.enemies().stream().allMatch(e -> e.def().id().equals("crystal_golem") && e.hp() == 10));
    }

    @Test
    void immunitiesBlockDamageButNormalHitsEverything() {
        TdSimulation sim = sim();
        SimEnemy soot = sim.spawn("soot_crawler");
        assertEquals(0, sim.hit(soot, 5, DamageKind.EXPLOSION));
        assertEquals("soot_crawler", soot.def().id());
        assertEquals(1, sim.hit(soot, 1, DamageKind.SHARP) > 0 ? 1 : 0);
        assertEquals(0, sim.hit(sim.spawn("brood_mother"), 1, DamageKind.SHARP), "hit but not popped");

        SimEnemy ironbreaker = sim.spawn("ironbreaker");
        for (DamageKind kind : new DamageKind[]{DamageKind.SHARP, DamageKind.COLD, DamageKind.ENERGY}) {
            assertEquals(0, sim.hit(ironbreaker, 1, kind), kind.name());
        }
        assertTrue(sim.hit(ironbreaker, 1, DamageKind.NORMAL) > 0);
        assertTrue(sim.hit(sim.spawn("ironbreaker"), 1, DamageKind.EXPLOSION) > 0);
        assertEquals(0, sim.hit(sim.spawn("frost_crawler"), 1, DamageKind.COLD));
        assertEquals(0, sim.hit(sim.spawn("ember_crawler"), 1, DamageKind.FIRE));
        assertEquals(0, sim.hit(sim.spawn("ember_crawler"), 1, DamageKind.ENERGY));
        assertEquals(0, sim.hit(sim.spawn("twilight_crawler"), 1, DamageKind.EXPLOSION));
        assertEquals(0, sim.hit(sim.spawn("twilight_crawler"), 1, DamageKind.COLD));
        assertTrue(sim.hit(sim.spawn("twilight_crawler"), 1, DamageKind.FIRE) > 0);
        SimEnemy ddt = sim.spawn("shadow_hunter");
        for (DamageKind kind : new DamageKind[]{DamageKind.SHARP, DamageKind.COLD, DamageKind.ENERGY, DamageKind.EXPLOSION}) {
            assertEquals(0, sim.hit(ddt, 1, kind), kind.name());
        }
        sim.hit(ddt, 1, DamageKind.FIRE);
        assertEquals(399, ddt.hp(), 0, "fire hurts it");
    }

    @Test
    void ceramicsAndBossesHaveRealHitPoints() {
        TdSimulation sim = sim();
        SimEnemy golem = sim.spawn("crystal_golem");
        for (int i = 0; i < 9; i++) {
            assertEquals(0, sim.hit(golem, 1, DamageKind.SHARP));
        }
        assertEquals(1, sim.hit(golem, 1, DamageKind.SHARP));
        assertEquals(2, sim.count());
        assertEquals(200, EnemyDefs.get("brood_mother").hp(), 0);
        assertEquals(700, EnemyDefs.get("behemoth").hp(), 0);
        assertEquals(4000, EnemyDefs.get("colossus").hp(), 0);
        assertEquals(400, EnemyDefs.get("shadow_hunter").hp(), 0);
        assertEquals(20000, EnemyDefs.get("swarm_queen").hp(), 0);
    }

    @Test
    void fortifiedEnemiesTakeMoreAndPassItOnOnlyWhereItCanBeUsed() {
        TdSimulation sim = sim();
        SimEnemy lead = sim.spawn("ironbreaker", false, false, true);
        assertEquals(4, lead.hp(), 0);
        assertEquals(26, lead.rbe(), 0);
        SimEnemy ceramic = sim.spawn("crystal_golem", false, false, true);
        assertEquals(20, ceramic.hp(), 0);
        sim.hit(ceramic, 20, DamageKind.SHARP);
        assertEquals(2, sim.enemies().stream().filter(e -> e.def().id().equals("shimmer_crawler") && !e.fortified()).count(),
                "the rainbow children of a fortified ceramic are plain");
        SimEnemy moab = sim.spawn("brood_mother", false, false, true);
        assertEquals(400, moab.hp(), 0);
        sim.hit(moab, 400, DamageKind.SHARP);
        List<SimEnemy> golems = sim.enemies().stream().filter(e -> e.def().id().equals("crystal_golem") && e.fortified()).toList();
        assertEquals(4, golems.size());
        assertEquals(20, golems.get(0).hp(), 0);
        assertFalse(sim.spawn("red_crawler", false, false, true).fortified(), "a red crawler cannot be fortified");
    }

    @Test
    void regrowHealsOneLayerEveryThreeSeconds() {
        TdSimulation sim = sim();
        SimEnemy yellow = sim.spawn("yellow_crawler", false, true, false);
        sim.hit(yellow, 1, DamageKind.SHARP);
        SimEnemy green = sim.enemies().get(0);
        assertEquals("green_crawler", green.def().id());
        assertTrue(green.regrow());
        for (int i = 0; i < TdSimulation.REGROW_TICKS - 1; i++) {
            sim.tick();
        }
        assertEquals("green_crawler", green.def().id(), "not yet after 59 ticks");
        sim.tick();
        assertEquals("yellow_crawler", green.def().id(), "after 3 s the layer is back");
        assertEquals(4, green.rbe(), 0);
        for (int i = 0; i < 3 * TdSimulation.REGROW_TICKS; i++) {
            sim.tick();
        }
        assertEquals("yellow_crawler", green.def().id(), "not past its original form");
    }

    @Test
    void regrownLayersPayNoCoin() {
        TdSimulation sim = sim();
        SimEnemy yellow = sim.spawn("yellow_crawler", false, true, false);
        assertEquals(1, sim.hit(yellow, 1, DamageKind.SHARP));
        for (int i = 0; i < TdSimulation.REGROW_TICKS; i++) {
            sim.tick();
        }
        SimEnemy regrown = sim.enemies().get(0);
        assertEquals("yellow_crawler", regrown.def().id());
        assertEquals(0, sim.hit(regrown, 1, DamageKind.SHARP), "the regrown layer is free");
        SimEnemy green = sim.enemies().get(0);
        assertEquals("green_crawler", green.def().id());
        assertEquals(1, sim.hit(green, 1, DamageKind.SHARP), "the next layer pays again");
    }

    @Test
    void regrowingChildrenGrowBackToTheParent() {
        TdSimulation sim = sim();
        SimEnemy rainbow = sim.spawn("shimmer_crawler", false, true, false);
        sim.hit(rainbow, 1, DamageKind.SHARP);
        assertEquals(2, sim.count());
        for (int i = 0; i < TdSimulation.REGROW_TICKS; i++) {
            sim.tick();
        }
        assertEquals(2, sim.count());
        assertTrue(sim.enemies().stream().allMatch(e -> e.def().id().equals("shimmer_crawler")), "both zebras healed back to rainbows");
    }

    @Test
    void shadowHuntersBreedCamoRegrowingGolemsThatStayGolems() {
        TdSimulation sim = sim();
        SimEnemy ddt = sim.spawn("shadow_hunter");
        assertTrue(ddt.camo());
        assertFalse(ddt.regrow());
        sim.hit(ddt, 400, DamageKind.FIRE);
        assertEquals(4, sim.count());
        for (SimEnemy golem : sim.enemies()) {
            assertEquals("crystal_golem", golem.def().id());
            assertTrue(golem.camo());
            assertTrue(golem.regrow());
        }
        sim.hit(sim.enemies().get(0), 10, DamageKind.SHARP);
        for (int i = 0; i < 10 * TdSimulation.REGROW_TICKS; i++) {
            sim.tick();
        }
        assertTrue(sim.enemies().stream().allMatch(e -> e.def().id().equals("crystal_golem") || e.def().id().equals("shimmer_crawler")));
    }

    @Test
    void camoEnemiesAreOnlyTargetedWithDetection() {
        TdSimulation sim = sim();
        sim.spawn("red_crawler", true, false, false);
        Comparator<SimEnemy> first = Comparator.comparingDouble(SimEnemy::distance).reversed();
        assertTrue(sim.targets(0, 0, 0, 5, false, first, 5).isEmpty());
        assertEquals(1, sim.targets(0, 0, 0, 5, true, first, 5).size());
        sim.spawn("red_crawler");
        assertEquals(1, sim.targets(0, 0, 0, 5, false, first, 5).size());
        assertEquals(2, sim.targets(0, 0, 0, 5, true, first, 5).size());
        assertEquals(1, sim.targets(0, 0, 0, 5, true, first, 1).size());
        assertTrue(sim.targets(100, 0, 0, 5, true, first, 5).isEmpty());
    }

    @Test
    void leakingCostsTheRemainingRbe() {
        TdSimulation sim = new TdSimulation(straight(10));
        SimEnemy pink = sim.spawn("pink_crawler");
        sim.hit(pink, 1, DamageKind.SHARP);
        for (int i = 0; i < 200; i++) {
            sim.tick();
        }
        assertEquals(0, sim.count());
        assertEquals(4, sim.takeLeaked());
        assertEquals(0, sim.takeLeaked());
    }

    @Test
    void childrenKeepTheirPlaceOnThePath() {
        TdSimulation sim = sim();
        SimEnemy moab = sim.spawn("brood_mother");
        for (int i = 0; i < 100; i++) {
            sim.tick();
        }
        double at = moab.distance();
        sim.hit(moab, 200, DamageKind.SHARP);
        for (SimEnemy child : sim.enemies()) {
            assertEquals(at, child.distance(), 1e-9);
            assertEquals(at, child.x(), 1e-9);
        }
    }

    @Test
    void lateGameRules() {
        TdSimulation sim = sim();
        sim.setRound(80);
        assertEquals(1, sim.hpFactor(), 0);
        assertEquals(104, sim.spawn("crystal_golem").rbe(), 0);
        sim.setRound(81);
        assertTrue(sim.lateGame());
        SimEnemy golem = sim.spawn("crystal_golem");
        assertEquals(60, golem.hp(), 0, "super crystal golem");
        assertEquals(68, golem.rbe(), 0);
        sim.hit(golem, 60, DamageKind.SHARP);
        assertEquals(1, sim.enemies().stream().filter(e -> e.def().id().equals("shimmer_crawler")).count(), "only one rainbow comes out");
        assertEquals(8, sim.enemies().stream().filter(e -> e.def().id().equals("shimmer_crawler")).findFirst().orElseThrow().rbe(), 0);
        assertEquals(204, sim.spawn("brood_mother").hp(), 1e-9);
        sim.setRound(100);
        assertEquals(1.4, TdSimulation.lateHpFactor(100), 1e-9);
        assertEquals(1.4, TdSimulation.lateSpeedFactor(100), 1e-9);
        assertEquals(1.6, TdSimulation.lateSpeedFactor(101), 1e-9);
        assertEquals(2.58, TdSimulation.lateSpeedFactor(150), 1e-9);
        assertEquals(3.0, TdSimulation.lateSpeedFactor(151), 1e-9);
        assertEquals(1.45, TdSimulation.lateHpFactor(101), 1e-9);
        assertEquals(6.5, TdSimulation.lateHpFactor(150), 1e-9);
        assertEquals(41.5, TdSimulation.lateHpFactor(250), 1e-9);
        assertEquals(1, TdSimulation.lateSpeedFactor(80), 0);
        SimEnemy red = sim.spawn("red_crawler");
        sim.tick();
        assertEquals(0.125 * 1.4, red.distance(), 1e-9);
    }

    @Test
    void theNonBossChildrenOfBlackAndLeadAreSingleInTheLateGame() {
        TdSimulation sim = sim();
        sim.setRound(90);
        SimEnemy lead = sim.spawn("ironbreaker");
        sim.hit(lead, 1, DamageKind.NORMAL);
        assertEquals(1, sim.count());
        assertEquals("soot_crawler", sim.enemies().get(0).def().id());
        sim.hit(sim.enemies().get(0), 1, DamageKind.NORMAL);
        assertEquals(1, sim.count());
        assertEquals("pink_crawler", sim.enemies().get(0).def().id());
    }

    @Test
    void popLayersMatchTheCoinsOfTheWiki() {
        // 1 coin per layer: a MOAB pays 1 + 4 ceramics of 95 (R40: 381), a ceramic 1 + 2 rainbows of 47.
        assertEquals(95, EnemyDefs.layers(EnemyDefs.get("crystal_golem"), false));
        assertEquals(381, EnemyDefs.layers(EnemyDefs.get("brood_mother"), false));
        assertEquals(1525, EnemyDefs.layers(EnemyDefs.get("behemoth"), false));
    }

    @Test
    void theTargetListKeepsTheOrderGiven() {
        TdSimulation sim = sim();
        SimEnemy far = sim.spawn("red_crawler");
        for (int i = 0; i < 40; i++) {
            sim.tick();
        }
        SimEnemy near = sim.spawn("red_crawler");
        assertNotNull(near);
        Comparator<SimEnemy> first = Comparator.comparingDouble(SimEnemy::distance).reversed();
        assertEquals(far.id(), sim.targets(0, 0, 0, 50, false, first, 2).get(0).id());
        Comparator<SimEnemy> last = Comparator.comparingDouble(SimEnemy::distance);
        assertEquals(near.id(), sim.targets(0, 0, 0, 50, false, last, 2).get(0).id());
    }

    @Test
    void thousandEnemiesTickInMilliseconds() {
        TdSimulation sim = new TdSimulation(straight(10_000));
        String[] kinds = {"red_crawler", "yellow_crawler", "shimmer_crawler", "crystal_golem", "brood_mother"};
        for (int i = 0; i < 1000; i++) {
            sim.spawn(kinds[i % kinds.length], i % 7 == 0, i % 5 == 0, i % 3 == 0);
        }
        List<SimEnemy> copy = new ArrayList<>(sim.enemies());
        for (int i = 0; i < 50; i++) {
            sim.tick(); // warm-up
        }
        long start = System.nanoTime();
        int ticks = 200;
        for (int i = 0; i < ticks; i++) {
            sim.tick();
        }
        double millis = (System.nanoTime() - start) / 1e6 / ticks;
        assertEquals(1000, sim.count());
        assertTrue(millis < 10, "a tick with 1000 enemies took " + millis + " ms");
        assertEquals(1000, copy.size());
    }
}
