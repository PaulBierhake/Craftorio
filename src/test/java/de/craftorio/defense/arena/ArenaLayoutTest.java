package de.craftorio.defense.arena;

import de.craftorio.defense.EnemyType;
import de.craftorio.defense.LevelPlan;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaLayoutTest {
    @Test
    void everyMapHasAWayAndRoomForTowers() {
        for (long seed = 0; seed < 30; seed++) {
            for (int level = 1; level <= 45; level++) {
                ArenaLayout layout = ArenaLayout.generate(seed, level);
                assertTrue(layout.shortestPath() >= ArenaLayout.SIZE, "no way at seed " + seed + " level " + level);
                assertTrue(layout.buildableTiles() >= ArenaLayout.MIN_BUILD_TILES, "no room at seed " + seed + " level " + level);
                assertTrue(layout.tile(0, layout.spawnRow()).allowsPath());
                assertTrue(layout.tile(ArenaLayout.SIZE - 1, ArenaLayout.CORE_ROW).allowsPath());
            }
        }
    }

    @Test
    void mapsAreDeterministicAndChangeEveryLevel() {
        ArenaLayout a = ArenaLayout.generate(42, 7);
        ArenaLayout b = ArenaLayout.generate(42, 7);
        assertEquals(a.theme(), b.theme());
        assertEquals(a.count(Tile.BLOCKED), b.count(Tile.BLOCKED));
        for (int level = 1; level < 40; level++) {
            assertNotEquals(ArenaLayout.themeFor(42, level), ArenaLayout.themeFor(42, level + 1), "theme repeats after level " + level);
        }
        assertEquals(ArenaTheme.COLOSSEUM, ArenaLayout.themeFor(42, 10));
        assertEquals(ArenaTheme.COLOSSEUM, ArenaLayout.themeFor(42, 30));
    }

    @Test
    void themesShapeTheTerrain() {
        Set<ArenaTheme> seen = EnumSet.noneOf(ArenaTheme.class);
        for (int level = 1; level <= 10; level++) {
            ArenaLayout layout = ArenaLayout.generate(5, level);
            seen.add(layout.theme());
            switch (layout.theme()) {
                case MOUNTAIN -> assertTrue(layout.count(Tile.HIGH) > 0, "plateaus");
                case WATER, FIRE, FOREST -> assertTrue(layout.count(Tile.ROUGH) > 0, "fords, vents or thicket");
                case COLOSSEUM -> assertTrue(layout.count(Tile.BLOCKED) > 0, "pillars");
            }
        }
        assertEquals(EnumSet.allOf(ArenaTheme.class), seen);
    }

    @Test
    void mutatorsStartAtLevelSixAndSkipBosses() {
        int withMutator = 0;
        for (long seed = 0; seed < 50; seed++) {
            for (int level = 1; level <= 40; level++) {
                Mutator mutator = Mutator.forLevel(seed, level);
                if (level < 6 || level % 10 == 0) {
                    assertEquals(Mutator.NONE, mutator);
                } else if (mutator != Mutator.NONE) {
                    withMutator++;
                    assertTrue(mutator.rewardFactor() > 1);
                }
            }
        }
        assertTrue(withMutator > 300 && withMutator < 900, "about a third of the levels: " + withMutator);
    }

    @Test
    void starsBonusesAndPreview() {
        assertEquals(3, LevelPlan.stars(LevelPlan.LIVES));
        assertEquals(2, LevelPlan.stars(LevelPlan.LIVES / 2));
        assertEquals(1, LevelPlan.stars(1));
        assertEquals(50, LevelPlan.starBonus(100, 3));
        assertEquals(0, LevelPlan.starBonus(100, 1));
        assertEquals(10 * 3, LevelPlan.earlyCallBonus(5, 200));
        Map<EnemyType, Integer> summary = LevelPlan.summary(List.of(EnemyType.CRAWLER, EnemyType.BREAKER, EnemyType.CRAWLER));
        assertEquals(Map.of(EnemyType.CRAWLER, 2, EnemyType.BREAKER, 1), summary);
        assertEquals(EnemyType.CRAWLER, summary.keySet().iterator().next());
    }
}
