package de.craftorio.team;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkBudgetTest {
    @Test
    void busiestChunksWinAndTiesGoToTheLowerKey() {
        Map<Long, Integer> weights = Map.of(1L, 5, 2L, 50, 3L, 5, 4L, 20);

        assertEquals(List.of(2L, 4L), ChunkBudget.select(weights, 2));
        assertEquals(List.of(2L, 4L, 1L), ChunkBudget.select(weights, 3));
        assertEquals(4, ChunkBudget.select(weights, 100).size());
        assertEquals(List.of(), ChunkBudget.select(weights, 0));
        assertEquals(List.of(), ChunkBudget.select(Map.of(), 10));
    }
}
