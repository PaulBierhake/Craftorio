package de.craftorio.logistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeltSlopeTest {
    /** Outputs and inputs as in {@link BeltSlope}: flat 0/0, up 0/+1 (input/output), down +1/0. */
    @Test
    void edgesMeetOnlyAtTheSameHeight() {
        // flat to flat on the same level
        assertTrue(BeltGeometry.connects(64, 0, 64, 0));
        // an up belt (output +1) hands over to a flat or up belt (input 0) one level higher
        assertTrue(BeltGeometry.connects(64, 1, 65, 0));
        assertFalse(BeltGeometry.connects(64, 1, 64, 0));
        // a flat belt hands over to a down belt (input +1) one level lower
        assertTrue(BeltGeometry.connects(65, 0, 64, 1));
        assertFalse(BeltGeometry.connects(64, 0, 64, 1));
        // down to down
        assertTrue(BeltGeometry.connects(64, 0, 63, 1));
    }
}
