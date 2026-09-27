package de.craftorio.world;

import de.craftorio.world.cave.CaveLayers;
import de.craftorio.world.cave.CaveShape;
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
        assertEquals(CaveLayers.Rule.KEEP, CaveLayers.rule(-11));
        assertTrue(CaveLayers.inCaveLayer(0));
        assertFalse(CaveLayers.inCaveLayer(40));
    }

    @Test
    void cavesHaveHeadroomStayInsideTheLayerAndAreMostlyWalkable() {
        CaveShape shape = new CaveShape(12345L);
        int open = 0;
        int columns = 0;
        for (int x = -200; x < 200; x += 3) {
            for (int z = -200; z < 200; z += 3) {
                int floor = shape.floorY(x, z);
                int ceiling = shape.ceilingY(x, z);
                assertTrue(floor >= CaveLayers.CAVE_BOTTOM + 1, "floor above bottom");
                assertTrue(ceiling <= CaveLayers.CAP_ONE_BOTTOM - 4, "ceiling below cap rock with rock above");
                assertTrue(ceiling - floor >= 6, "headroom at " + x + "," + z);
                columns++;
                if (shape.isOpen(x, floor + 1, z)) {
                    open++;
                }
            }
        }
        double openFraction = (double) open / columns;
        assertTrue(openFraction > 0.75 && openFraction < 0.98, "open fraction " + openFraction);
    }

    @Test
    void shapeIsDeterministicPerSeed() {
        CaveShape a = new CaveShape(99L);
        CaveShape b = new CaveShape(99L);
        for (int i = 0; i < 50; i++) {
            assertEquals(a.floorY(i * 7, i * 13), b.floorY(i * 7, i * 13));
            assertEquals(a.isPillar(i * 5, -i * 3), b.isPillar(i * 5, -i * 3));
        }
    }
}
