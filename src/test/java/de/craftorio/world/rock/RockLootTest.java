package de.craftorio.world.rock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RockLootTest {
    @Test
    void bigBouldersGiveStoneAndCoalInTheRangeOfTheConcept() {
        for (int roll = 0; roll < 1_000; roll++) {
            int stone = RockLoot.stone(true, roll);
            int coal = RockLoot.coal(true, roll);
            assertTrue(stone >= 24 && stone <= 50, "stone " + stone);
            assertTrue(coal >= 10 && coal <= 25, "coal " + coal);
        }
    }

    @Test
    void smallRocksGiveOnlyStone() {
        for (int roll = 0; roll < 1_000; roll++) {
            int stone = RockLoot.stone(false, roll);
            assertTrue(stone >= 5 && stone <= 10, "stone " + stone);
            assertEquals(0, RockLoot.coal(false, roll));
        }
    }
}
