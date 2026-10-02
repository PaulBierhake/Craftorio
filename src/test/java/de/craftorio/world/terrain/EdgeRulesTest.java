package de.craftorio.world.terrain;

import org.junit.jupiter.api.Test;

import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeRulesTest {
    /** Ground everywhere (top block 64), a plateau (top 68) for x >= 100 and a lake (bed 60) for z >= 300. */
    private static final IntBinaryOperator MAP = (x, z) -> z >= 300 ? 61 : x >= 100 ? 69 : 65;

    @Test
    void edgesAreColumnsNextToAnotherHeight() {
        assertTrue(EdgeRules.onEdge(MAP, 99, 200), "ground next to the plateau");
        assertTrue(EdgeRules.onEdge(MAP, 100, 200), "plateau next to the ground");
        assertFalse(EdgeRules.onEdge(MAP, 98, 200), "two blocks from the edge");
        assertFalse(EdgeRules.onEdge(MAP, 150, 200), "middle of the plateau");
    }

    @Test
    void noTreesAroundTheSpawnAndOnEdges() {
        assertTrue(EdgeRules.treeFree(MAP, 10, -20), "spawn");
        assertTrue(EdgeRules.treeFree(MAP, 0, 64), "radius 64");
        assertFalse(EdgeRules.treeFree(MAP, 0, 65), "just outside the radius");
        assertTrue(EdgeRules.treeFree(MAP, 99, 200), "edge");
        assertFalse(EdgeRules.treeFree(MAP, 50, 200), "open ground");
    }

    @Test
    void shoreIsGroundWithinTwoBlocksOfTheWater() {
        assertEquals(EdgeRules.Shore.BED, EdgeRules.shore(MAP, 10, 310));
        assertEquals(EdgeRules.Shore.SHORE, EdgeRules.shore(MAP, 10, 299));
        assertEquals(EdgeRules.Shore.SHORE, EdgeRules.shore(MAP, 10, 298));
        assertEquals(EdgeRules.Shore.NONE, EdgeRules.shore(MAP, 10, 297));
        assertEquals(EdgeRules.Shore.NONE, EdgeRules.shore(MAP, 150, 200), "plateau is not a shore");
    }

    @Test
    void aboutOneInFourBedsIsGravel() {
        int gravel = 0;
        for (int x = 0; x < 200; x++) {
            for (int z = 0; z < 200; z++) {
                if (EdgeRules.gravel(x, z)) {
                    gravel++;
                }
            }
        }
        double share = gravel / 40_000.0;
        assertTrue(share > 0.18 && share < 0.32, "gravel share " + share);
    }
}
