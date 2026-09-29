package de.craftorio.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolarLogicTest {
    @Test
    void fullPowerByDayNoneByNight() {
        assertEquals(60, SolarLogic.output(0, 60));
        assertEquals(60, SolarLogic.output(6_000, 60));
        assertEquals(0, SolarLogic.output(18_000, 60));
        assertEquals(0, SolarLogic.output(13_000, 60));
    }

    @Test
    void rampsDownAtDuskAndUpAtDawn() {
        int dusk = SolarLogic.output(12_250, 60);
        assertTrue(dusk > 0 && dusk < 60, "dusk " + dusk);
        int dawn = SolarLogic.output(23_500, 60);
        assertTrue(dawn > 0 && dawn < 60, "dawn " + dawn);
        assertEquals(0, SolarLogic.output(23_000, 60));
    }

    @Test
    void followsTheDayCycleOverManyDays() {
        assertEquals(SolarLogic.output(6_000, 60), SolarLogic.output(6_000 + 24_000L * 100, 60));
    }
}
