package de.craftorio.defense.arena;

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
    void everyMapHasARouteInThePathWindowThatThePathTracerAccepts() {
        for (long seed = 0; seed < 60; seed++) {
            for (int level = 1; level <= 50; level++) {
                ArenaLayout layout = ArenaLayout.generate(seed, level);
                List<int[]> route = layout.route();
                String where = "seed " + seed + " level " + level + " " + layout.theme() + " gate " + layout.spawnRow();
                assertTrue(route.size() >= ArenaLayout.MIN_PATH && route.size() <= ArenaLayout.MAX_PATH, where + ": route of " + route.size());
                Set<List<Integer>> cells = new java.util.HashSet<>();
                for (int[] cell : route) {
                    assertTrue(layout.tile(cell[0], cell[1]).allowsPath(), where + ": blocked route tile " + cell[0] + "," + cell[1]);
                    cells.add(List.of(cell[0], 0, cell[1]));
                }
                assertEquals(route.size(), cells.size(), where + ": the route crosses itself");
                de.craftorio.defense.PathTracer.Result traced = de.craftorio.defense.PathTracer.trace(new int[]{-1, 0, layout.spawnRow()},
                        (x, y, z) -> cells.contains(List.of(x, y, z)),
                        (x, y, z) -> Math.abs(x - ArenaLayout.SIZE) + Math.abs(z - ArenaLayout.CORE_ROW) == 1);
                assertTrue(traced.ok(), where + ": " + traced.error());
                assertEquals(route.size(), traced.path().size(), where);
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
    void starsBonusesAndPreview() {
        assertEquals(3, LevelPlan.stars(LevelPlan.LIVES));
        assertEquals(2, LevelPlan.stars(LevelPlan.LIVES / 2));
        assertEquals(1, LevelPlan.stars(1));
        assertEquals(50, LevelPlan.starBonus(100, 3));
        assertEquals(0, LevelPlan.starBonus(100, 1));
        assertEquals(3, LevelPlan.stars(200, 200));
        assertEquals(2, LevelPlan.stars(100, 200));
        assertEquals(1, LevelPlan.stars(99, 200));
        assertEquals(1, LevelPlan.stars(0, 1), "impoppable: one life, a lost life is a lost level");
        // round 3: 10 red, 5 blue, 15 red crawlers: 25 red and 5 blue, red first
        Map<Integer, Integer> summary = LevelPlan.summary(de.craftorio.defense.sim.RoundDefs.get(3));
        assertEquals(Map.of(0, 25, 1, 5), summary);
        assertEquals(0, summary.keySet().iterator().next());
        // round 17: regrowing yellow crawlers carry the regrow bit above the index
        assertEquals(Map.of(3 | 2 << 8, 12), LevelPlan.summary(de.craftorio.defense.sim.RoundDefs.get(17)));
    }
}
