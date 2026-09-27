package de.craftorio.logistics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeltLaneTest {
    private static final float SPEED = 0.1F;

    @Test
    void itemsMoveAndLeaveAtFrontEdge() {
        BeltLane<String> lane = new BeltLane<>();
        lane.acceptFromBehind("a", 0);
        List<String> delivered = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            lane.tick(SPEED, (item, overshoot) -> delivered.add(item));
        }
        assertTrue(delivered.isEmpty());
        assertEquals(0.9F, lane.entries().get(0).progress(), 1e-4);

        lane.tick(SPEED, (item, overshoot) -> delivered.add(item));
        assertEquals(List.of("a"), delivered);
        assertTrue(lane.isEmpty());
    }

    @Test
    void blockedItemsQueueWithSpacing() {
        BeltLane<String> lane = new BeltLane<>();
        for (String item : List.of("a", "b", "c", "d")) {
            lane.tick(0.3F, (i, o) -> false);
            assertTrue(lane.acceptFromBehind(item, 0));
        }
        for (int i = 0; i < 50; i++) {
            lane.tick(SPEED, (item, overshoot) -> false);
        }

        List<BeltLane.Entry<String>> entries = lane.entries();
        assertEquals(1.0F, entries.get(0).progress(), 1e-4);
        assertEquals(0.75F, entries.get(1).progress(), 1e-4);
        assertEquals(0.5F, entries.get(2).progress(), 1e-4);
        assertEquals(0.25F, entries.get(3).progress(), 1e-4);
        assertFalse(lane.acceptFromBehind("e", 0), "full lane");
    }

    @Test
    void acceptFromBehindRespectsLastItem() {
        BeltLane<String> lane = new BeltLane<>();
        lane.acceptFromBehind("a", 0.1F);

        assertFalse(lane.acceptFromBehind("b", 0.05F), "too close to the item already at 0.1");
        lane.tick(0.3F, (i, o) -> false);
        assertTrue(lane.acceptFromBehind("b", 0.2F));
        assertEquals(0.15F, lane.entries().get(1).progress(), 1e-4, "clamped behind 'a' at 0.4");
    }

    @Test
    void sideInsertNeedsGap() {
        BeltLane<String> lane = new BeltLane<>();
        assertTrue(lane.insertAt("a", 0.5F));
        assertFalse(lane.insertAt("b", 0.6F));
        assertTrue(lane.insertAt("c", 0.9F));
        assertTrue(lane.insertAt("d", 0.1F));

        assertEquals("c", lane.front());
        assertEquals("c", lane.removeFront());
        assertEquals("a", lane.front());
    }

    @Test
    void renderProgressInterpolatesLastTick() {
        BeltLane<String> lane = new BeltLane<>();
        lane.acceptFromBehind("a", 0.2F);
        lane.tick(SPEED, (i, o) -> false);

        assertEquals(0.25F, lane.entries().get(0).renderProgress(0.5F), 1e-4);
    }

    @Test
    void fasterTiersMoveMoreItems() {
        assertEquals(7.5F, BeltTier.BASIC.itemsPerSecondPerLane(), 1e-6F);
        assertEquals(2 * BeltTier.BASIC.speedPerTick(), BeltTier.FAST.speedPerTick(), 1e-6F);
        assertEquals(3 * BeltTier.BASIC.speedPerTick(), BeltTier.EXPRESS.speedPerTick(), 1e-6F);
        BeltLane<String> lane = new BeltLane<>();
        lane.insertAt("a", 0.0F);
        List<String> delivered = new ArrayList<>();
        int ticks = 0;
        while (delivered.isEmpty()) {
            lane.tick(BeltTier.EXPRESS.speedPerTick(), (item, overshoot) -> delivered.add(item));
            ticks++;
        }
        assertEquals(4, ticks, "express belt crosses a block in 4 ticks");
    }
}
