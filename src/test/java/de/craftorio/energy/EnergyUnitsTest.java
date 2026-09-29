package de.craftorio.energy;

import de.craftorio.Pacing;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnergyUnitsTest {
    @Test
    void oneKilowattIsOneFePerTick() {
        assertEquals(20_000, Energy.FE_PER_MJ);
        // A burner drill (150 kW) runs 26.7 s on 4 MJ of coal, a steam engine (900 kW) 4.4 s, a stone furnace (90 kW) 44 s.
        assertEquals(533, Energy.burnTicks(Fuel.COAL_MJ, 150));
        assertEquals(89, Energy.burnTicks(Fuel.COAL_MJ, Energy.STEAM_ENGINE_KW));
        assertEquals(0, Energy.burnTicks(4, 0));
        // Wood carries half the energy of coal.
        assertEquals(Energy.burnTicks(Fuel.COAL_MJ, 150) / 2, Energy.burnTicks(Fuel.WOOD_MJ, 150), 1);
    }

    @Test
    void generatorsCarryTheirMachines() {
        // One 900 kW generator runs five 180 kW electric furnaces.
        assertEquals(5, Energy.STEAM_ENGINE_KW / de.craftorio.machine.MachineType.ELECTRIC_FURNACE.energyPerTick());
        assertTrue(GeneratorType.REACTOR.fePerTick() > Energy.STEAM_ENGINE_KW);
    }

    @Test
    void pacingFactorsScaleTimesAndNeverGoBelowOneTick() {
        assertEquals(100, Pacing.scaledTicks(100, 1.0));
        assertEquals(50, Pacing.scaledTicks(100, 2.0));
        assertEquals(200, Pacing.scaledTicks(100, 0.5));
        assertEquals(1, Pacing.scaledTicks(1, 100.0));
    }
}
