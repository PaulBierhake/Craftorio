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
        for (int x = 1; x <= 110; x++) {
            blocks.add(List.of(x, x > 55 ? 65 : 64, 0)); // one step up halfway
        }
        PathTracer.Result result = trace(blocks, 111);

        assertTrue(result.ok(), () -> "error " + result.error());
        assertEquals(110, result.path().size());
        assertEquals(1, result.path().get(0)[0]);
    }

    @Test
    void rejectsBranchesDeadEndsAndShortPaths() {
        Set<List<Integer>> branched = new HashSet<>();
        for (int x = 1; x <= 110; x++) {
            branched.add(List.of(x, 64, 0));
        }
        branched.add(List.of(10, 64, 1));
        assertEquals(PathTracer.Error.BRANCH, trace(branched, 111).error());

        Set<List<Integer>> deadEnd = new HashSet<>();
        for (int x = 1; x <= 15; x++) {
            deadEnd.add(List.of(x, 64, 0));
        }
        assertEquals(PathTracer.Error.DEAD_END, trace(deadEnd, 111).error());
        assertEquals(PathTracer.Error.NO_START, trace(Set.of(), 111).error());
    }

    @Test
    void theLengthOfThePathMustLieInTheWindow() {
        assertEquals(100, PathTracer.MIN_LENGTH);
        assertEquals(160, PathTracer.MAX_LENGTH);
        for (int[] lengthAndExpected : new int[][]{{99, 1}, {100, 0}, {130, 0}, {160, 0}, {161, 2}}) {
            Set<List<Integer>> line = new HashSet<>();
            for (int x = 1; x <= lengthAndExpected[0]; x++) {
                line.add(List.of(x, 64, 0));
            }
            PathTracer.Result result = trace(line, lengthAndExpected[0] + 1);
            switch (lengthAndExpected[1]) {
                case 0 -> assertTrue(result.ok(), "length " + lengthAndExpected[0] + " " + result.error());
                case 1 -> assertEquals(PathTracer.Error.TOO_SHORT, result.error(), "length " + lengthAndExpected[0]);
                default -> assertEquals(PathTracer.Error.TOO_LONG, result.error(), "length " + lengthAndExpected[0]);
            }
        }
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
    }

    private static java.util.stream.Stream<String> enemiesOfLevel(int level) {
        return LevelPlan.of(level, 1).rounds().stream().flatMap(round -> round.groups().stream()).map(group -> group.enemy());
    }

    @Test
    void towerTypesReadTheirDataFromTheDefinitions() {
        assertEquals("crossbow_tower", TowerType.CROSSBOW.defId());
        assertEquals(200, TowerType.CROSSBOW.def().cost());
        assertTrue(TowerType.CROSSBOW.usesItemAmmo());
        assertTrue(TowerType.TESLA.usesEnergy());
        assertTrue(TowerType.LASER.usesEnergy());
        assertTrue(TowerType.FLAME.usesFluid());
        assertEquals(6, TowerType.FLAME.fluidPerShot());
        assertEquals(400, TowerType.TESLA.energyCapacity());
        assertEquals(200, TowerType.LASER.energyCapacity());
        assertEquals(de.craftorio.defense.sim.DamageKind.NORMAL, Magazine.ARMOUR_PIERCING.damageKind());
        assertEquals(null, Magazine.NORMAL.damageKind());
        assertEquals(1.6, Magazine.ARMOUR_PIERCING.factor(), 0);
        assertEquals(4.8, Magazine.URANIUM.factor(), 0);
    }

    @Test
    void theSealsOfTheEndgameComeAtLevelFortyAndFifty() {
        assertEquals(LevelPlan.KeyReward.PLATINUM_SEAL, LevelPlan.of(30, 1).keyReward());
        assertEquals(LevelPlan.KeyReward.DIAMOND_SEAL, LevelPlan.of(40, 1).keyReward());
        assertEquals(LevelPlan.KeyReward.STAR_SEAL, LevelPlan.of(50, 1).keyReward());
        assertEquals(LevelPlan.KeyReward.NONE, LevelPlan.of(41, 1).keyReward());
    }

    @Test
    void difficultiesFollowBloonsTd6() {
        assertEquals(200, Difficulty.EASY.lives());
        assertEquals(150, Difficulty.MEDIUM.lives());
        assertEquals(100, Difficulty.HARD.lives());
        assertEquals(1, Difficulty.IMPOPPABLE.lives());
        assertEquals(0.85, Difficulty.EASY.priceFactor(), 0);
        assertEquals(1.0, Difficulty.MEDIUM.priceFactor(), 0);
        assertEquals(1.08, Difficulty.HARD.priceFactor(), 0);
        assertEquals(1.2, Difficulty.IMPOPPABLE.priceFactor(), 0);
        // the wiki: Medium bloons are 10 % faster than on Easy, Hard ones 25 %
        assertEquals(1.0, Difficulty.EASY.speedFactor(), 0);
        assertEquals(1.1, Difficulty.MEDIUM.speedFactor(), 0);
        assertEquals(1.25, Difficulty.HARD.speedFactor(), 0);
        assertEquals(1.25 / 1.1, Difficulty.HARD.speedFactor() / Difficulty.MEDIUM.speedFactor(), 1e-9);
        assertEquals(850, Difficulty.EASY.price(1_000));
        assertEquals(1_080, Difficulty.HARD.price(1_000));
        assertEquals(1_200, Difficulty.IMPOPPABLE.price(1_000));
        assertEquals(Difficulty.MEDIUM.lives(), LevelPlan.LIVES);
    }

    @Test
    void theDifficultyCanOnlyBeMadeEasierOnceTheCampaignHasStarted() {
        assertTrue(Difficulty.MEDIUM.mayChangeTo(Difficulty.IMPOPPABLE, false));
        assertTrue(Difficulty.IMPOPPABLE.mayChangeTo(Difficulty.EASY, false));
        assertFalse(Difficulty.MEDIUM.mayChangeTo(Difficulty.HARD, true));
        assertTrue(Difficulty.HARD.mayChangeTo(Difficulty.MEDIUM, true));
        assertFalse(Difficulty.EASY.mayChangeTo(Difficulty.MEDIUM, true));
        assertEquals(Difficulty.MEDIUM, Difficulty.EASY.next());
        assertEquals(Difficulty.EASY, Difficulty.IMPOPPABLE.next());
    }

    @Test
    void arenasFromBeforeTheCoinsStartWithHalfOfWhatBloonsTd6Paid() {
        assertEquals(650, Coins.migratedStart(1), 0);
        // level 2 comes after rounds 1 and 2: (20 + 101) + (35 + 102) = 258, half of it
        assertEquals(650 + 129, Coins.migratedStart(2), 1e-9);
        assertTrue(Coins.migratedStart(20) > Coins.migratedStart(10));
        assertTrue(Coins.earnedUpTo(100) > 160_000, "the wiki's cumulative cash at round 100 (pops and bonuses) is far above 160,000");
    }
}
