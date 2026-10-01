package de.craftorio.defense.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeplayRoundsTest {
    @Test
    void theLateGameTableFollowsTheWiki() {
        assertEquals(1.02, TdSimulation.lateHpFactor(81), 1e-9);
        assertEquals(1.40, TdSimulation.lateHpFactor(100), 1e-9);
        assertEquals(1.45, TdSimulation.lateHpFactor(101), 1e-9);
        assertEquals(2.75, TdSimulation.lateHpFactor(125), 1e-9);
        assertEquals(2.90, TdSimulation.lateHpFactor(126), 1e-9);
        assertEquals(6.50, TdSimulation.lateHpFactor(150), 1e-9);
        assertEquals(41.50, TdSimulation.lateHpFactor(250), 1e-9);
        assertEquals(91.50, TdSimulation.lateHpFactor(300), 1e-9);
        assertEquals(491.50, TdSimulation.lateHpFactor(500), 1e-9);
        assertEquals(496.5, TdSimulation.lateHpFactor(501), 1e-9);
        assertEquals(1.40, TdSimulation.lateSpeedFactor(100), 1e-9);
        assertEquals(1.60, TdSimulation.lateSpeedFactor(101), 1e-9);
        assertEquals(2.58, TdSimulation.lateSpeedFactor(150), 1e-9);
        assertEquals(3.98, TdSimulation.lateSpeedFactor(200), 1e-9);
        assertEquals(4.50, TdSimulation.lateSpeedFactor(201), 1e-9);
        assertEquals(5.50, TdSimulation.lateSpeedFactor(251), 1e-9);
        assertEquals(6.0, TdSimulation.lateSpeedFactor(252), 1e-9);
        assertTrue(TdSimulation.lateHpFactor(1_000) > TdSimulation.lateHpFactor(501));
    }

    @Test
    void endlessRoundsStartWhereTheStandardListEnds() {
        assertEquals(140, RoundDefs.count());
        RoundDef endless = RoundDefs.get(141);
        assertEquals(141, endless.round());
        assertSame(endless, RoundDefs.get(141), "drawn once, the same for everybody");
        assertNotEquals(RoundDefs.get(142).groups(), endless.groups());
        assertEquals(RoundDefs.get(141).groups(), FreeplayRounds.round(141).groups(), "the seed is the round number");
    }

    @Test
    void theBudgetGrowsThreePercentARound() {
        assertEquals(800_000 * 1.03, FreeplayRounds.budget(141), 1);
        assertEquals(800_000 * Math.pow(1.03, 60), FreeplayRounds.budget(200), 1);
        assertTrue(FreeplayRounds.budget(2_000) <= FreeplayRounds.MAX_BUDGET);
    }

    @Test
    void endlessRoundsAreMadeOfRealEnemiesWithinTheirLimits() {
        for (int number = 141; number <= 900; number += 7) {
            RoundDef round = RoundDefs.get(number);
            assertTrue(round.enemyCount() >= 1 && round.enemyCount() <= FreeplayRounds.MAX_ENEMIES + 40, "round " + number + ": " + round.enemyCount());
            assertTrue(round.groups().size() <= FreeplayRounds.MAX_GROUPS + 1, "round " + number);
            assertTrue(round.duration() >= 25 && round.duration() <= 50);
            for (RoundDef.Group group : round.groups()) {
                assertTrue(EnemyDefs.exists(group.enemy()), group.enemy());
                assertTrue(group.count() >= 1);
                assertTrue(!group.fortified() || EnemyDefs.get(group.enemy()).canBeFortified(), "round " + number + " " + group.enemy());
            }
            double rbe = RoundDefs.rbe(round);
            assertTrue(rbe >= Math.min(FreeplayRounds.budget(number), 1e8) * 0.01, "round " + number + " has an RBE of " + rbe);
            assertTrue(round.rbe() > 0 && round.cash() >= 0);
            if (number % 10 == 0) {
                assertEquals("swarm_queen", round.groups().get(0).enemy(), "every tenth round brings the swarm queen");
            }
        }
    }

    @Test
    void endlessRoundsGetHarderOnAverage() {
        double early = 0;
        double late = 0;
        for (int i = 0; i < 20; i++) {
            early += RoundDefs.rbe(RoundDefs.get(141 + i));
            late += RoundDefs.rbe(RoundDefs.get(301 + i));
        }
        assertTrue(late > early * 3, early + " vs " + late);
    }
}
