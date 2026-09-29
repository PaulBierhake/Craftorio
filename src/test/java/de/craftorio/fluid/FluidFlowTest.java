package de.craftorio.fluid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidFlowTest {
    @Test
    void nothingFlowsUphillOrBetweenEqualLevels() {
        assertEquals(0, FluidFlow.move(20, 100, 50, 100, 60));
        assertEquals(0, FluidFlow.move(50, 100, 50, 100, 60));
        assertEquals(0, FluidFlow.move(0, 100, 0, 100, 60));
    }

    @Test
    void halfTheDifferenceFlowsSoBothSettleAtTheSameLevel() {
        assertEquals(30, FluidFlow.move(100, 100, 40, 100, 60));
        int moved = FluidFlow.move(100, 100, 0, 100, 60);
        assertEquals(50, moved);
        // Both containers now hold 50, so nothing more moves.
        assertEquals(0, FluidFlow.move(100 - moved, 100, moved, 100, 60));
    }

    @Test
    void flowIsLimitedByTheMaximumTheContentsAndTheRoom() {
        assertEquals(60, FluidFlow.move(25_000, 25_000, 0, 25_000, 60));
        assertEquals(2, FluidFlow.move(5, 100, 0, 1_000_000, 60));
        assertTrue(FluidFlow.move(100, 100, 95, 100, 60) <= 5);
    }

    @Test
    void aFullTankFeedsAnEmptyPipeByFillLevelNotByAmount() {
        // 50 % of a big tank into an empty pipe of 100: the pipe fills to its capacity limit per tick.
        int moved = FluidFlow.move(12_500, 25_000, 0, 100, 60);
        assertTrue(moved > 0 && moved <= 50, "moved " + moved);
    }
}
