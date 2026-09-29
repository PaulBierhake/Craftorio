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
    void levelsGetHarderAndBringKeyMaterialsEveryTenLevels() {
        LevelPlan first = LevelPlan.of(1, 1);
        LevelPlan fifth = LevelPlan.of(5, 1);
        LevelPlan tenth = LevelPlan.of(10, 1);

        assertEquals(3, first.waves().size());
        assertTrue(first.waves().stream().flatMap(List::stream).allMatch(type -> type == EnemyType.CRAWLER));
        assertTrue(fifth.waves().stream().flatMap(List::stream).anyMatch(type -> type == EnemyType.BREAKER));
        assertTrue(tenth.enemyCount() > fifth.enemyCount());
        assertTrue(tenth.healthMultiplier() > fifth.healthMultiplier());
        assertEquals(EnemyType.BROOD_MOTHER, tenth.waves().get(tenth.waves().size() - 1).get(tenth.waves().get(tenth.waves().size() - 1).size() - 1));

        assertEquals(LevelPlan.KeyReward.NONE, first.keyReward());
        assertEquals(LevelPlan.KeyReward.SILVER_SEAL, tenth.keyReward());
        assertEquals(LevelPlan.KeyReward.PLATINUM_SEAL, LevelPlan.keyReward(30));
        assertEquals(LevelPlan.KeyReward.BRONZE_SEAL, LevelPlan.keyReward(5));
        assertEquals(LevelPlan.KeyReward.GOLD_SEAL, LevelPlan.keyReward(20));
        assertEquals(LevelPlan.KeyReward.NONE, LevelPlan.keyReward(60));
        assertTrue(tenth.reward() > LevelPlan.of(9, 1).reward() + 100, "milestone bonus");
    }

    @Test
    void moreOnlinePlayersMeanTougherEnemies() {
        assertEquals(1.35, LevelPlan.of(1, 2).healthMultiplier() / LevelPlan.of(1, 1).healthMultiplier(), 1e-9);
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
    void crystalGolemsArriveAtLevelTwentyAndResistAmmunition() {
        assertTrue(LevelPlan.of(19, 1).waves().stream().flatMap(List::stream).noneMatch(type -> type == EnemyType.CRYSTAL_GOLEM));
        assertTrue(LevelPlan.of(20, 1).waves().stream().flatMap(List::stream).anyMatch(type -> type == EnemyType.CRYSTAL_GOLEM));
        assertEquals(3.5, EnemyType.CRYSTAL_GOLEM.damageTaken(10, false), 1e-9);
        assertEquals(10, EnemyType.CRYSTAL_GOLEM.damageTaken(10, true), 1e-9);
        assertEquals(10, EnemyType.BREAKER.damageTaken(10, false), 1e-9);
        assertTrue(TowerType.LASER.energyWeapon());
        assertTrue(TowerType.LASER.range() > TowerType.GUN.range());
    }

    @Test
    void moreOnlinePlayersMeanMoreAndTougherEnemies() {
        LevelPlan solo = LevelPlan.of(7, 1);
        LevelPlan trio = LevelPlan.of(7, 3);
        assertEquals(solo.enemyCount() + 2 * LevelPlan.CRAWLERS_PER_EXTRA_PLAYER * solo.waves().size(), trio.enemyCount());
        assertTrue(trio.healthMultiplier() > solo.healthMultiplier());
        assertEquals(solo.reward(), trio.reward(), "rewards do not scale – the team shares them");
    }
}
