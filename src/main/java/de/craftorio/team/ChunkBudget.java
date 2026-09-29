package de.craftorio.team;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Which chunks a team may keep loaded: the ones with the most of its blocks, up to the limit. Pure logic. */
public final class ChunkBudget {
    private ChunkBudget() {
    }

    /** The chunk keys with the highest weights (ties: lower key first), at most {@code limit}. */
    public static List<Long> select(Map<Long, Integer> weights, int limit) {
        return weights.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .limit(Math.max(0, limit))
                .map(Map.Entry::getKey)
                .toList();
    }
}
