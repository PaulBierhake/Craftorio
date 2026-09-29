package de.craftorio.energy;

import de.craftorio.heat.HeatLogic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeatLogicTest {
    @Test
    void heatFlowsFromHotToCold() {
        assertTrue(HeatLogic.flow(300, 20_000, 2, 100, 20_000, 2) > 0);
        assertTrue(HeatLogic.flow(100, 20_000, 2, 300, 20_000, 2) < 0);
        assertEquals(0, HeatLogic.flow(200, 20_000, 2, 200, 20_000, 2));
    }

    @Test
    void aLinkNeverOvershoots() {
        double a = 900;
        double b = 15;
        for (int i = 0; i < 200; i++) {
            double q = HeatLogic.flow(a, 20_000, 6, b, 20_000, 6);
            a -= q / 20_000;
            b += q / 20_000;
            assertTrue(a >= b, "temperatures crossed after " + i + " steps");
        }
        assertEquals(a, b, 1.0);
    }

    @Test
    void aChainOfPipesConductsTheHeatOfAReactor() {
        // Reactor (index 0) with 40 MW, 30 heat pipes, steady state: every link carries the full 40,000 per tick.
        int pipes = 30;
        double[] temperature = new double[pipes + 1];
        double[] capacity = new double[pipes + 1];
        java.util.Arrays.fill(temperature, HeatLogic.AMBIENT);
        java.util.Arrays.fill(capacity, HeatLogic.PIPE_CAPACITY);
        capacity[0] = HeatLogic.REACTOR_CAPACITY;
        for (int tick = 0; tick < 20_000; tick++) {
            temperature[0] = HeatLogic.warm(temperature[0], capacity[0], HeatLogic.REACTOR_HEAT);
            for (int i = 0; i < pipes; i++) {
                int leftDegree = i == 0 ? 1 : 2;
                double q = HeatLogic.flow(temperature[i], capacity[i], leftDegree, temperature[i + 1], capacity[i + 1], i + 1 == pipes ? 1 : 2);
                temperature[i] = HeatLogic.warm(temperature[i], capacity[i], -q);
                temperature[i + 1] = HeatLogic.warm(temperature[i + 1], capacity[i + 1], q);
            }
            // a heat exchanger at the far end takes 10 MW
            temperature[pipes] = HeatLogic.warm(temperature[pipes], capacity[pipes], -Math.min(HeatLogic.EXCHANGER_HEAT, HeatLogic.usableHeat(temperature[pipes], capacity[pipes])));
        }
        assertTrue(temperature[0] > temperature[pipes], "heat flows down the chain");
        assertTrue(temperature[pipes] >= HeatLogic.STEAM_TEMPERATURE - 1, "the far end gets hot enough for steam: " + temperature[pipes]);
    }

    @Test
    void neighbourBonusDoublesTheHeat() {
        assertEquals(40_000, HeatLogic.reactorHeat(0));
        assertEquals(80_000, HeatLogic.reactorHeat(1));
        assertEquals(120_000, HeatLogic.reactorHeat(2));
    }

    @Test
    void temperatureIsKeptBetweenAmbientAndMaximum() {
        assertEquals(HeatLogic.AMBIENT, HeatLogic.warm(20, 20_000, -1_000_000));
        assertEquals(HeatLogic.MAX_TEMPERATURE, HeatLogic.warm(990, 20_000, 1_000_000));
        assertEquals(35, HeatLogic.warm(15, 20_000, 400_000), 1e-9);
    }

    @Test
    void anExchangerNeedsFiveHundredDegrees() {
        assertEquals(0, HeatLogic.usableHeat(499, HeatLogic.EXCHANGER_CAPACITY));
        assertEquals(0, HeatLogic.usableHeat(500, HeatLogic.EXCHANGER_CAPACITY));
        assertEquals(20_000, HeatLogic.usableHeat(501, HeatLogic.EXCHANGER_CAPACITY));
    }

    @Test
    void oneReactorFeedsFourExchangersAndSevenTurbines() {
        // wiki: 103 steam/s per exchanger, 60 steam/s and 5.82 MW per turbine
        assertEquals(103.09, HeatLogic.EXCHANGER_STEAM_PER_TICK * 20, 0.05);
        double steam = 4 * HeatLogic.EXCHANGER_STEAM_PER_TICK;
        assertEquals(6.87, steam / HeatLogic.TURBINE_STEAM_PER_TICK, 0.02);
        assertEquals(5.82, HeatLogic.TURBINE_POWER / 1000.0, 1e-9);
        assertEquals(40_000, 4 * HeatLogic.EXCHANGER_HEAT);
        assertEquals(HeatLogic.REACTOR_HEAT, 4 * HeatLogic.EXCHANGER_HEAT);
    }

    @Test
    void aFuelCellBurnsTwoHundredSeconds() {
        assertEquals(200, HeatLogic.CELL_TICKS / 20);
        assertTrue(HeatLogic.mayLoadCell(800, 900));
        assertTrue(!HeatLogic.mayLoadCell(900, 900));
    }
}
