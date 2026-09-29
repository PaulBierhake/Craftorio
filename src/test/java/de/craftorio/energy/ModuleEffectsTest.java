package de.craftorio.energy;

import de.craftorio.module.ModuleEffects;
import de.craftorio.module.ModuleKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleEffectsTest {
    @Test
    void theNineModulesFollowTheWiki() {
        assertEquals(new ModuleEffects(20, 50, 0), ModuleKind.SPEED.effects(1));
        assertEquals(new ModuleEffects(30, 60, 0), ModuleKind.SPEED.effects(2));
        assertEquals(new ModuleEffects(50, 70, 0), ModuleKind.SPEED.effects(3));
        assertEquals(new ModuleEffects(0, -30, 0), ModuleKind.EFFICIENCY.effects(1));
        assertEquals(new ModuleEffects(0, -40, 0), ModuleKind.EFFICIENCY.effects(2));
        assertEquals(new ModuleEffects(0, -50, 0), ModuleKind.EFFICIENCY.effects(3));
        assertEquals(new ModuleEffects(-5, 40, 4), ModuleKind.PRODUCTIVITY.effects(1));
        assertEquals(new ModuleEffects(-10, 60, 6), ModuleKind.PRODUCTIVITY.effects(2));
        assertEquals(new ModuleEffects(-15, 80, 10), ModuleKind.PRODUCTIVITY.effects(3));
    }

    @Test
    void effectsAreAddedUp() {
        ModuleEffects two = ModuleKind.SPEED.effects(1).plus(ModuleKind.SPEED.effects(1));
        assertEquals(40, two.speed());
        assertEquals(100, two.energy());
        assertEquals(1.4, two.speedFactor(), 1e-9);
        assertEquals(2.0, two.energyFactor(), 1e-9);
    }

    @Test
    void speedAndEnergyNeverDropBelowTwentyPercent() {
        ModuleEffects efficiency = ModuleKind.EFFICIENCY.effects(3).times(4); // four modules: −200 %
        assertEquals(0.2, efficiency.energyFactor(), 1e-9);
        ModuleEffects slow = ModuleKind.PRODUCTIVITY.effects(3).times(10); // −150 %
        assertEquals(0.2, slow.speedFactor(), 1e-9);
        assertEquals(1, new ModuleEffects(0, -500, 0).power(1)); // never below one
        assertEquals(30, ModuleEffects.NONE.plus(efficiency).power(150));
    }

    @Test
    void aSpeedModuleShortensACraftAndCostsMorePower() {
        ModuleEffects speed = ModuleKind.SPEED.effects(1);
        assertEquals(83, speed.ticks(100), 1);
        assertEquals(225, speed.power(150));
        assertEquals(10, ModuleEffects.NONE.ticks(10));
        assertEquals(1, new ModuleEffects(1_000, 0, 0).ticks(1));
    }

    @Test
    void aBeaconGivesHalfOfSpeedAndEfficiencyAndNoProductivity() {
        ModuleEffects beacon = ModuleKind.SPEED.effects(1).plus(ModuleKind.SPEED.effects(1)); // two speed modules
        ModuleEffects given = beacon.transmitted();
        assertEquals(20, given.speed());
        assertEquals(50, given.energy());
        assertEquals(0, ModuleKind.PRODUCTIVITY.effects(3).transmitted().productivity());
        // two beacons add up
        assertEquals(40, given.plus(given).speed());
    }

    @Test
    void theProductivityBarGivesAnExtraProductPerHundredPercent() {
        ModuleEffects.Bar bar = new ModuleEffects.Bar();
        int extra = 0;
        for (int craft = 0; craft < 100; craft++) {
            extra += bar.add(0.125); // exactly representable, so the bar does not drift
        }
        assertEquals(12, extra);
        assertEquals(0.5, bar.progress(), 1e-9);
        // +300 % is the limit: four modules of tier 3 make +40 % only
        assertEquals(0.4, ModuleKind.PRODUCTIVITY.effects(3).times(4).productivityBonus(), 1e-9);
        assertEquals(3.0, new ModuleEffects(0, 0, 1_000).productivityBonus(), 1e-9);
        assertEquals(0.0, new ModuleEffects(0, 0, -5).productivityBonus(), 1e-9);
    }

    @Test
    void onlyProductivityIsKeptOutOfBeacons() {
        assertTrue(ModuleKind.SPEED.beaconAllowed());
        assertTrue(ModuleKind.EFFICIENCY.beaconAllowed());
        assertTrue(!ModuleKind.PRODUCTIVITY.beaconAllowed());
    }
}
