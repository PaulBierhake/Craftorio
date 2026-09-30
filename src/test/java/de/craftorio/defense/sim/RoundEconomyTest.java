package de.craftorio.defense.sim;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundEconomyTest {
    private static TdPath straight() {
        return new TdPath(List.of(new double[]{0, 0, 0}, new double[]{150, 0, 0}));
    }

    /** Pops every enemy of a round completely and returns the coins that paid. */
    private static double coinsForPoppingRound(int round) {
        TdSimulation sim = new TdSimulation(straight());
        sim.setRound(round);
        RoundDef def = RoundDefs.get(round);
        for (RoundDef.Group group : def.groups()) {
            for (int i = 0; i < group.count(); i++) {
                sim.spawn(group.enemy(), group.camo(), group.regrow(), group.fortified());
            }
        }
        int guard = 0;
        while (sim.count() > 0) {
            for (SimEnemy enemy : List.copyOf(sim.enemies())) {
                sim.hit(enemy, 1e12, DamageKind.NORMAL);
            }
            assertTrue(++guard < 50, "round " + round + " does not end");
        }
        return sim.takeCoins();
    }

    @Test
    void poppingEveryEnemyPaysWhatTheWikiListsForEveryRound() {
        for (int round = 1; round <= RoundDefs.count(); round++) {
            double wiki = RoundDefs.get(round).cash();
            double ours = coinsForPoppingRound(round);
            assertEquals(wiki, ours, Math.max(0.05, wiki * 0.001), "round " + round);
        }
    }

    @Test
    void incomeFactorAndRoundBonus() {
        assertEquals(1, RoundRules.incomeFactor(1), 0);
        assertEquals(1, RoundRules.incomeFactor(50), 0);
        assertEquals(0.5, RoundRules.incomeFactor(51), 0);
        assertEquals(0.5, RoundRules.incomeFactor(60), 0);
        assertEquals(0.2, RoundRules.incomeFactor(61), 0);
        assertEquals(0.2, RoundRules.incomeFactor(85), 0);
        assertEquals(0.1, RoundRules.incomeFactor(86), 0);
        assertEquals(0.1, RoundRules.incomeFactor(100), 0);
        assertEquals(0.05, RoundRules.incomeFactor(101), 0);
        assertEquals(0.04, RoundRules.incomeFactor(121), 0);
        assertEquals(0.02, RoundRules.incomeFactor(141), 0);
        assertEquals(101, RoundRules.roundBonus(1));
        assertEquals(200, RoundRules.roundBonus(100));
        assertEquals(650, RoundRules.START_COINS);
    }

    @Test
    void theFirstTwoRoundsPayWhatBloonsTd6Pays() {
        // round 1: 20 pops + 101 bonus (wiki: $20 + $101), round 2: 35 + 102
        assertEquals(20, coinsForPoppingRound(1), 1e-9);
        assertEquals(35, coinsForPoppingRound(2), 1e-9);
        assertEquals(121, coinsForPoppingRound(1) + RoundRules.roundBonus(1), 1e-9);
    }

    @Test
    void aSuperCrystalGolemPaysEightySevenForItsLayer() {
        assertEquals(95, EnemyDefs.cash(EnemyDefs.get("crystal_golem"), false));
        assertEquals(95, EnemyDefs.cash(EnemyDefs.get("crystal_golem"), true), "87 + 8 for the rainbow chain: the same total");
        assertEquals(381, EnemyDefs.cash(EnemyDefs.get("brood_mother"), true));
        assertEquals(1, EnemyDefs.get("crystal_golem").popCash(false));
        assertEquals(87, EnemyDefs.get("crystal_golem").popCash(true));
    }

    /** Lives lost by leaking (wiki: Freeplay lives cost, and the RBE before round 81). */
    @Test
    void leakCostsLivesLikeTheWiki() {
        Map<String, Integer> late = Map.ofEntries(Map.entry("soot_crawler", 6), Map.entry("frost_crawler", 6), Map.entry("ember_crawler", 6),
                Map.entry("ironbreaker", 7), Map.entry("twilight_crawler", 7), Map.entry("shimmer_crawler", 8), Map.entry("crystal_golem", 65),
                Map.entry("brood_mother", 460), Map.entry("behemoth", 2540), Map.entry("colossus", 14160), Map.entry("shadow_hunter", 660),
                Map.entry("swarm_queen", 50300));
        late.forEach((id, lives) -> assertEquals(lives, EnemyDefs.leak(EnemyDefs.get(id), true, false), "late " + id));
        Map<String, Integer> lateFortified = Map.of("ironbreaker", 10, "crystal_golem", 75, "brood_mother", 700, "behemoth", 4200,
                "colossus", 24800, "shadow_hunter", 1100, "swarm_queen", 92900);
        lateFortified.forEach((id, lives) -> assertEquals(lives, EnemyDefs.leak(EnemyDefs.get(id), true, true), "late fortified " + id));
        // before round 81 it is the RBE
        assertEquals(616, EnemyDefs.leak(EnemyDefs.get("brood_mother"), false, false));
        assertEquals(104, EnemyDefs.leak(EnemyDefs.get("crystal_golem"), false, false));
        assertEquals(856, EnemyDefs.leak(EnemyDefs.get("brood_mother"), false, true));
        assertEquals(55_760, EnemyDefs.leak(EnemyDefs.get("swarm_queen"), false, false));
    }

    @Test
    void aLeakedEnemyCostsWhatIsLeftOfIt() {
        TdSimulation sim = new TdSimulation(straight());
        SimEnemy moab = sim.spawn("brood_mother");
        assertEquals(616, sim.lifeCost(moab));
        sim.hit(moab, 50, DamageKind.SHARP);
        assertEquals(566, sim.lifeCost(moab));
        sim.setRound(81);
        SimEnemy late = sim.spawn("brood_mother");
        assertEquals(460, sim.lifeCost(late), "unscaled, although the hit points grew by 2 %");
        sim.hit(late, 10.2, DamageKind.SHARP);
        assertEquals(450, sim.lifeCost(late));
    }

    @Test
    void roundsKnowWhenTheirLastEnemyIsGone() {
        TdSimulation sim = new TdSimulation(straight());
        sim.setRound(3);
        SimEnemy first = sim.spawn("pink_crawler");
        sim.setRound(4);
        sim.spawn("red_crawler");
        assertEquals(1, sim.aliveOfRound(3));
        assertEquals(1, sim.aliveOfRound(4));
        sim.hit(first, 1, DamageKind.SHARP);
        assertEquals(1, sim.aliveOfRound(3), "its child still counts for round 3");
        sim.hit(sim.enemies().get(1), 100, DamageKind.SHARP);
        assertEquals(0, sim.aliveOfRound(3));
        assertEquals(1, sim.aliveOfRound(4));
    }
}
