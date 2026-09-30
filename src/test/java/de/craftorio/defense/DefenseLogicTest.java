package de.craftorio.defense;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefenseLogicTest {
    @Test
    void aLevelIsTwoRoundsOfTheListAndBringsSealsAtTheMilestones() {
        LevelPlan first = LevelPlan.of(1, 1);
        LevelPlan tenth = LevelPlan.of(10, 1);

        assertEquals(2, first.rounds().size());
        assertEquals(1, first.rounds().get(0).round());
        assertEquals(2, first.rounds().get(1).round());
        assertEquals(55, first.totalRbe(), 0.5);
        assertEquals(55, first.enemyCount());
        assertEquals(19, tenth.rounds().get(0).round());
        assertEquals(20, tenth.rounds().get(1).round());
        assertEquals(99, LevelPlan.of(50, 1).rounds().get(0).round());
        assertEquals(100, LevelPlan.of(50, 1).rounds().get(1).round());
        assertEquals(47_424 + 67_200, LevelPlan.of(50, 1).totalRbe(), 1);

        assertEquals(LevelPlan.KeyReward.NONE, first.keyReward());
        assertEquals(LevelPlan.KeyReward.SILVER_SEAL, tenth.keyReward());
        assertEquals(LevelPlan.KeyReward.PLATINUM_SEAL, LevelPlan.keyReward(30));
        assertEquals(LevelPlan.KeyReward.BRONZE_SEAL, LevelPlan.keyReward(5));
        assertEquals(LevelPlan.KeyReward.GOLD_SEAL, LevelPlan.keyReward(20));
        assertEquals(LevelPlan.KeyReward.NONE, LevelPlan.keyReward(60));
        assertTrue(tenth.reward() > LevelPlan.of(9, 1).reward() + 100, "milestone bonus");
    }

    @Test
    void theNumberOfPlayersDoesNotChangeTheEnemies() {
        assertEquals(LevelPlan.of(7, 1).totalRbe(), LevelPlan.of(7, 4).totalRbe(), 0);
        assertEquals(LevelPlan.of(7, 1).reward(), LevelPlan.of(7, 4).reward());
    }

    @Test
    void tracesStraightAndSlopedPaths() {
        Set<List<Integer>> blocks = new HashSet<>();
        for (int x = 1; x <= 25; x++) {
            blocks.add(List.of(x, x > 12 ? 65 : 64, 0)); // one step up halfway
        }
        PathTracer.Result result = trace(blocks, 26);

        assertTrue(result.ok(), () -> "error " + result.error());
        assertEquals(25, result.path().size());
        assertEquals(1, result.path().get(0)[0]);
    }

    @Test
    void rejectsBranchesDeadEndsAndShortPaths() {
        Set<List<Integer>> branched = new HashSet<>();
        for (int x = 1; x <= 25; x++) {
            branched.add(List.of(x, 64, 0));
        }
        branched.add(List.of(10, 64, 1));
        assertEquals(PathTracer.Error.BRANCH, trace(branched, 26).error());

        Set<List<Integer>> deadEnd = new HashSet<>();
        for (int x = 1; x <= 15; x++) {
            deadEnd.add(List.of(x, 64, 0));
        }
        assertEquals(PathTracer.Error.DEAD_END, trace(deadEnd, 26).error());
        assertEquals(PathTracer.Error.TOO_SHORT, trace(deadEnd, 16).error());
        assertEquals(PathTracer.Error.NO_START, trace(Set.of(), 26).error());
    }

    /** Portal at x=0, core at x=coreX, all at y=64 and z=0 unless a block says otherwise. */
    private static boolean allows(Set<List<Integer>> blocks, int x, int z) {
        return PathTracer.allowsBlock(x, 64, z, (px, py, pz) -> blocks.contains(List.of(px, py, pz)),
                (px, py, pz) -> py == 64 && pz == 0 && (px == 0 || px == 30));
    }

    @Test
    void pathBlocksMayNotBranchOrFormAreas() {
        Set<List<Integer>> line = new HashSet<>();
        for (int x = 1; x <= 10; x++) {
            line.add(List.of(x, 64, 0));
        }
        assertTrue(allows(line, 11, 0), "straight");
        assertTrue(allows(line, 10, 1), "turn at the end");
        assertFalse(allows(line, 5, 1), "T junction");
        assertFalse(allows(line, 1, 1), "side of the first block (portal is a neighbour)");

        line.add(List.of(10, 64, 1));
        assertFalse(allows(line, 9, 1), "2x2 area");
        assertTrue(allows(line, 10, 2), "curve goes on");
        assertTrue(allows(line, 10, 1) || line.contains(List.of(10, 64, 1)));

        Set<List<Integer>> near = new HashSet<>();
        for (int x = 20; x <= 29; x++) {
            near.add(List.of(x, 64, 0));
        }
        assertFalse(allows(near, 29, 1), "next to the core");
        assertTrue(allows(near, 19, 0));
        // A block one level up still counts as the neighbour in its column.
        assertFalse(allows(Set.of(List.of(4, 65, 0), List.of(5, 64, 1), List.of(5, 64, -1)), 5, 0));
    }

    @Test
    void towerItemsOfLevelOneAtFullHealthStackWithNewOnes() {
        int full = TowerStats.maxHealth(TowerType.GUN, 1);
        assertTrue(TowerStats.isPristine(TowerType.GUN, 1, full));
        assertFalse(TowerStats.isPristine(TowerType.GUN, 1, full - 1));
        assertFalse(TowerStats.isPristine(TowerType.GUN, 2, TowerStats.maxHealth(TowerType.GUN, 2)));
        assertFalse(TowerStats.isPristine(TowerType.GUN, 1, 0), "ruins are not pristine");
    }

    @Test
    void upgradesRaiseDamageAndHealth() {
        assertEquals(4, TowerStats.damage(TowerType.CROSSBOW, 1), 1e-9);
        assertEquals(8.8, TowerStats.damage(TowerType.CROSSBOW, 5), 1e-9);
        assertEquals(200, TowerStats.maxHealth(TowerType.CROSSBOW, 5));
        assertEquals(30, TowerStats.repairCost(70, 100));
        assertEquals(0, TowerStats.repairCost(120, 100));
        assertFalse(TowerStats.upgradeCredits(2) >= TowerStats.upgradeCredits(3));
    }

    /** Portal at x=0, core right after the last block at x = coreX. */
    private static PathTracer.Result trace(Set<List<Integer>> blocks, int coreX) {
        return PathTracer.trace(new int[]{0, 64, 0},
                (x, y, z) -> blocks.contains(List.of(x, y, z)),
                (x, y, z) -> Math.abs(coreX - x) + Math.abs(z) == 1);
    }

    @Test
    void theNewEnemiesArriveWithTheRoundsOfTheList() {
        assertTrue(enemiesOfLevel(18).noneMatch(id -> id.equals("crystal_golem")));
        assertTrue(enemiesOfLevel(19).anyMatch(id -> id.equals("crystal_golem")), "round 38");
        assertTrue(enemiesOfLevel(20).anyMatch(id -> id.equals("brood_mother")), "round 40");
        assertTrue(enemiesOfLevel(30).anyMatch(id -> id.equals("behemoth")), "round 60");
        assertTrue(enemiesOfLevel(40).anyMatch(id -> id.equals("colossus")), "round 80");
        assertTrue(enemiesOfLevel(45).anyMatch(id -> id.equals("shadow_hunter")), "round 90");
        assertTrue(enemiesOfLevel(49).noneMatch(id -> id.equals("swarm_queen")));
        assertTrue(enemiesOfLevel(50).anyMatch(id -> id.equals("swarm_queen")), "round 100");
        assertTrue(TowerType.LASER.energyWeapon());
        assertTrue(TowerType.LASER.range() > TowerType.GUN.range());
    }

    private static java.util.stream.Stream<String> enemiesOfLevel(int level) {
        return LevelPlan.of(level, 1).rounds().stream().flatMap(round -> round.groups().stream()).map(group -> group.enemy());
    }

    @Test
    void towerDamageKindsAndLayerDamage() {
        assertEquals(de.craftorio.defense.sim.DamageKind.SHARP, TowerType.CROSSBOW.damageKind(Magazine.NORMAL));
        assertEquals(de.craftorio.defense.sim.DamageKind.SHARP, TowerType.GUN.damageKind(Magazine.NORMAL));
        assertEquals(de.craftorio.defense.sim.DamageKind.NORMAL, TowerType.GUN.damageKind(Magazine.ARMOUR_PIERCING));
        assertEquals(de.craftorio.defense.sim.DamageKind.ENERGY, TowerType.LASER.damageKind(Magazine.NORMAL));
        assertEquals(de.craftorio.defense.sim.DamageKind.FIRE, TowerType.FLAME.damageKind(Magazine.NORMAL));
        assertEquals(1, TowerStats.layerDamage(TowerType.CROSSBOW, 1, 1));
        assertEquals(8, TowerStats.layerDamage(TowerType.GUN, 1, TowerType.URANIUM_FACTOR));
    }

    @Test
    void theSealsOfTheEndgameComeAtLevelFortyAndFifty() {
        assertEquals(LevelPlan.KeyReward.PLATINUM_SEAL, LevelPlan.of(30, 1).keyReward());
        assertEquals(LevelPlan.KeyReward.DIAMOND_SEAL, LevelPlan.of(40, 1).keyReward());
        assertEquals(LevelPlan.KeyReward.STAR_SEAL, LevelPlan.of(50, 1).keyReward());
        assertEquals(LevelPlan.KeyReward.NONE, LevelPlan.of(41, 1).keyReward());
    }
}
