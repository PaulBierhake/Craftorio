package de.craftorio.machine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrillTest {
    @Test
    void outputScalesWithFieldBlocksCoveredByArea() {
        DrillProduction full = new DrillProduction(0.05);
        DrillProduction partial = new DrillProduction(0.05);
        int fullItems = 0;
        int partialItems = 0;
        for (int tick = 0; tick < 20 * 100; tick++) {
            fullItems += full.tick(9);
            partialItems += partial.tick(3);
        }

        assertEquals(45, fullItems);
        assertEquals(15, partialItems);
        assertEquals(0, new DrillProduction(0.05).tick(0));
        assertEquals(0.45, full.itemsPerSecond(9), 1e-9);
    }

    @Test
    void areasOfNeighbouringDrillsMustNotOverlap() {
        // 3x3 areas: drills need at least 3 blocks between their centres.
        assertTrue(DrillArea.overlaps(0, 64, 0, 1, 2, 64, 0, 1));
        assertFalse(DrillArea.overlaps(0, 64, 0, 1, 3, 64, 0, 1));
        assertTrue(DrillArea.overlaps(0, 64, 0, 1, 2, 64, -2, 1));
        assertFalse(DrillArea.overlaps(0, 64, 0, 1, 2, 65, 0, 1), "different layers never overlap");
        assertTrue(DrillArea.overlaps(0, 64, 0, 2, 3, 64, 0, 1), "5x5 next to 3x3");
    }

    @Test
    void higherTiersMineFasterAndCoverMore() {
        assertEquals(0.25, DrillTier.BURNER.maxItemsPerSecond(), 1e-9);
        assertEquals(0.54, DrillTier.ELECTRIC.maxItemsPerSecond(), 1e-9);
        assertEquals(25, DrillTier.DEEP.area());
        assertEquals(3.0, DrillTier.DEEP.maxItemsPerSecond(), 1e-9);
        assertTrue(DrillTier.BURNER.usesFuel());
        assertFalse(DrillTier.DEEP.usesFuel());
        for (DrillTier tier : DrillTier.values()) {
            assertTrue(tier.radius() <= DrillTier.MAX_RADIUS);
        }
    }
}
