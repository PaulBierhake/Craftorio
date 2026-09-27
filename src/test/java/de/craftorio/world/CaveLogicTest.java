package de.craftorio.world;

import de.craftorio.world.cave.CaveLayers;
import de.craftorio.world.cave.CaveShape;
import de.craftorio.world.cave.Layer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaveLogicTest {
    @Test
    void heightBandsFollowTheConcept() {
        assertEquals(CaveLayers.Rule.KEEP, CaveLayers.rule(50));
        assertEquals(CaveLayers.Rule.CAP_IF_SOLID, CaveLayers.rule(49));
        assertEquals(CaveLayers.Rule.CAP_IF_SOLID, CaveLayers.rule(40));
        assertEquals(CaveLayers.Rule.FILL, CaveLayers.rule(39));
        assertEquals(CaveLayers.Rule.FILL, CaveLayers.rule(0));
        assertEquals(CaveLayers.Rule.CAP, CaveLayers.rule(-1));
        assertEquals(CaveLayers.Rule.CAP, CaveLayers.rule(-10));
        assertEquals(CaveLayers.Rule.MINE_FILL, CaveLayers.rule(-11));
        assertEquals(CaveLayers.Rule.MINE_FILL, CaveLayers.rule(-59));
        assertEquals(CaveLayers.Rule.KEEP, CaveLayers.rule(-60));
        assertTrue(CaveLayers.inCaveLayer(0));
        assertFalse(CaveLayers.inCaveLayer(40));
    }

    @Test
    void cavesHaveHeadroomStayInsideTheLayerAndAreMostlyWalkable() {
        for (Layer layer : Layer.values()) {
            checkLayer(layer, layer == Layer.CAVES ? 6 : 5);
        }
    }

    private static void checkLayer(Layer layer, int minHeadroom) {
        CaveShape shape = CaveShape.of(12345L, layer);
        int open = 0;
        int columns = 0;
        for (int x = -200; x < 200; x += 3) {
            for (int z = -200; z < 200; z += 3) {
                int floor = shape.floorY(x, z);
                int ceiling = shape.ceilingY(x, z);
                assertTrue(floor >= layer.minY() + 1, layer + " floor above bottom");
                assertTrue(ceiling <= layer.maxY() - 3, layer + " ceiling below the cap rock with rock above");
                assertTrue(ceiling - floor >= minHeadroom, layer + " headroom at " + x + "," + z);
                columns++;
                if (shape.isOpen(x, floor + 1, z)) {
                    open++;
                }
            }
        }
        double openFraction = (double) open / columns;
        assertTrue(openFraction > 0.6 && openFraction < 0.98, layer + " open fraction " + openFraction);
    }

    @Test
    void shapeIsDeterministicPerSeed() {
        CaveShape a = CaveShape.of(99L, Layer.CAVES);
        CaveShape b = CaveShape.of(99L, Layer.CAVES);
        CaveShape mines = CaveShape.of(99L, Layer.MINES);
        assertTrue(mines.floorY(0, 0) < 0, "mine halls lie below Y 0");
        for (int i = 0; i < 50; i++) {
            assertEquals(a.floorY(i * 7, i * 13), b.floorY(i * 7, i * 13));
            assertEquals(a.isPillar(i * 5, -i * 3), b.isPillar(i * 5, -i * 3));
        }
    }
}
