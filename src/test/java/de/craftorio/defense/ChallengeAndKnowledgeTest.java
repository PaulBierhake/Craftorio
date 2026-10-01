package de.craftorio.defense;

import de.craftorio.defense.sim.TdSimulation;
import de.craftorio.defense.sim.TdPath;
import de.craftorio.defense.sim.TowerDefs;
import de.craftorio.defense.sim.TowerProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengeAndKnowledgeTest {
    @Test
    void challengesFollowTheConcept() {
        assertEquals(6, Challenge.values().length);
        assertEquals(2.0, Challenge.DOUBLE_BOSS_HP.bossHpFactor());
        assertEquals(0.5, Challenge.HALF_CASH.coinFactor());
        assertEquals(1.5, Challenge.DOUBLE_BOSS_HP.rewardFactor());
        assertEquals(1.5, Challenge.HALF_CASH.rewardFactor());
        assertEquals(1.3, Challenge.ALTERNATE.rewardFactor());
        assertEquals(1.4, Challenge.APOCALYPSE.rewardFactor());
        assertEquals(2.0, Challenge.CHIMPS.rewardFactor());
        assertTrue(Challenge.APOCALYPSE.continuous() && !Challenge.NONE.continuous());
        assertTrue(Challenge.CHIMPS.oneLife() && Challenge.CHIMPS.noSelling() && Challenge.CHIMPS.noWarChest() && Challenge.CHIMPS.noDepots());
        assertTrue(Challenge.ALTERNATE.alternateRounds());
        assertEquals(Challenge.NONE, Challenge.CHIMPS.next(), "the challenges go round in a circle");
        assertEquals(Challenge.CHIMPS, Challenge.byOrdinal(-1));
    }

    @Test
    void alternateLevelsUseTheAlternateRounds() {
        assertNotEquals(LevelPlan.of(3, Challenge.NONE).rounds().get(0).groups(), LevelPlan.of(3, Challenge.ALTERNATE).rounds().get(0).groups());
        assertEquals(LevelPlan.of(7, Challenge.NONE).rounds(), LevelPlan.of(7, Challenge.DOUBLE_BOSS_HP).rounds(), "the other challenges keep the rounds");
        assertEquals(LevelPlan.of(71, Challenge.NONE).rounds(), LevelPlan.of(71, 1).rounds());
        assertEquals(141, LevelPlan.of(71, Challenge.NONE).rounds().get(0).round(), "level 71 is the first endless one");
    }

    @Test
    void bossHitPointsDoubleInTheSimulation() {
        TdSimulation sim = new TdSimulation(new TdPath(List.of(new double[]{0, 0, 0}, new double[]{40, 0, 0})));
        sim.setBossHpMultiplier(2);
        sim.setRound(40);
        assertEquals(400, sim.spawn("brood_mother").hp(), 1e-9);
        assertEquals(1, sim.spawn("red_crawler").hp(), 1e-9);
        sim.setRound(120);
        assertEquals(2 * 200 * TdSimulation.lateHpFactor(120), sim.spawn("brood_mother").hp(), 1e-6, "on top of the late game");
    }

    @Test
    void knowledgeNeedsItsResearch() {
        Set<String> none = Set.of();
        Set<String> all = new java.util.HashSet<>();
        Knowledge.ALL.forEach(id -> all.add("craftorio:" + id));
        assertEquals(0.7, Knowledge.sellShare(none, 0.7));
        assertEquals(0.75, Knowledge.sellShare(all, 0.7));
        assertEquals(0.8, Knowledge.sellShare(all, 0.8), "a depot's 80 % stays");
        assertEquals(0, Knowledge.upgradeDiscount(none, 1));
        assertEquals(0.05, Knowledge.upgradeDiscount(all, 2));
        assertEquals(0, Knowledge.upgradeDiscount(all, 3));
        assertEquals(0, Knowledge.key(none));
        assertEquals((1 << Knowledge.ALL.size()) - 1, Knowledge.key(all));
    }

    @Test
    void knowledgeChangesTheProfilesOfTowers() {
        Set<String> all = new java.util.HashSet<>();
        Knowledge.ALL.forEach(id -> all.add("craftorio:" + id));
        TowerProfile crossbow = TowerDefs.profile(TowerDefs.get("crossbow_tower"), new int[]{0, 0, 0}).copy();
        Knowledge.apply(crossbow, "crossbow_tower", all);
        assertEquals(3, crossbow.main().pierce);
        TowerProfile gun = TowerDefs.profile(TowerDefs.get("gun_turret"), new int[]{0, 0, 0}).copy();
        Knowledge.apply(gun, "gun_turret", all);
        assertEquals(1, gun.main().pierce, "only the crossbow pierces more");
        TowerProfile village = TowerDefs.profile(TowerDefs.get("command_post"), new int[]{0, 1, 0}).copy();
        double range = village.range;
        double ring = village.attacks.get(0).radius;
        Knowledge.apply(village, "command_post", all);
        assertEquals(range * 1.1, village.range, 1e-9);
        assertTrue(village.attacks.get(0).radius > ring, "the grow blocker's ring widens with it");
        TowerProfile depot = TowerDefs.profile(TowerDefs.get("supply_depot"), new int[]{1, 0, 0}).copy();
        Knowledge.apply(depot, "supply_depot", all);
        assertEquals(132, de.craftorio.defense.sim.DepotEconomy.production(depot), 1e-9, "120 + 10 %");
        TowerProfile unchanged = TowerDefs.profile(TowerDefs.get("supply_depot"), new int[]{1, 0, 0}).copy();
        assertEquals(120, de.craftorio.defense.sim.DepotEconomy.production(TowerDefs.profile(TowerDefs.get("supply_depot"), new int[]{1, 0, 0})), 1e-9, "the shared profile is untouched");
        Knowledge.apply(unchanged, "supply_depot", Set.of());
        assertEquals(120, de.craftorio.defense.sim.DepotEconomy.production(unchanged), 1e-9);
        assertFalse(Knowledge.has(Set.of(), Knowledge.THRIFTY));
    }
}
