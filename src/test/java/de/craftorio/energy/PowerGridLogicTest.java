package de.craftorio.energy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PowerGridLogicTest {
    @Test
    void polesWithinRangeFormOneNetwork() {
        List<int[]> poles = List.of(
                new int[]{0, 64, 0}, new int[]{7, 64, 0}, new int[]{14, 64, 0}, // chain of three
                new int[]{40, 64, 0});                                        // far away
        int[] network = PoleGrouping.group(poles, 7.5);

        assertEquals(network[0], network[1]);
        assertEquals(network[1], network[2]);
        assertNotEquals(network[0], network[3]);
    }

    @Test
    void fullSupplyWhenGeneratorsCanCoverDemand() {
        PowerDistribution.Result result = PowerDistribution.distribute(new long[]{200}, new long[]{20, 30});

        assertEquals(50, result.transferred());
        assertArrayEquals(new long[]{50}, result.taken());
        assertArrayEquals(new long[]{20, 30}, result.given());
    }

    @Test
    void shortageIsSharedProportionally() {
        PowerDistribution.Result result = PowerDistribution.distribute(new long[]{30, 30}, new long[]{100, 20});

        assertEquals(60, result.transferred());
        assertArrayEquals(new long[]{30, 30}, result.taken());
        assertArrayEquals(new long[]{50, 10}, result.given());
    }

    @Test
    void roundingNeverLosesOrInventsEnergy() {
        PowerDistribution.Result result = PowerDistribution.distribute(new long[]{10, 10, 10}, new long[]{7, 7, 7});

        assertEquals(21, result.transferred());
        assertEquals(21, result.taken()[0] + result.taken()[1] + result.taken()[2]);
        assertArrayEquals(new long[]{7, 7, 7}, result.given());
    }

    @Test
    void nothingFlowsWithoutProducersOrConsumers() {
        assertEquals(0, PowerDistribution.distribute(new long[0], new long[]{10}).transferred());
        assertEquals(0, PowerDistribution.distribute(new long[]{10}, new long[0]).transferred());
    }
}
