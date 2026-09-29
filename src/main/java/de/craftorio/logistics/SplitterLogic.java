package de.craftorio.logistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Which outputs a splitter offers an item, in the order they are tried. Items alternate between the connected
 * outputs only. With a filter, matching items go to the output chosen for the filter and everything else alternates
 * between the other connected outputs; if the chosen output is not connected (or nothing else is), items fall back to
 * alternating between whatever is connected, so nothing jams. Pure logic for unit tests.
 */
public final class SplitterLogic {
    public enum Output {
        FRONT, LEFT, RIGHT
    }

    private SplitterLogic() {
    }

    /**
     * @param connected    outputs that lead to a belt or container
     * @param hasFilter    a filter item is set
     * @param matches      the item matches the filter (ignored without a filter)
     * @param filterOutput where matching items go
     * @param roundRobin   counter that advances after every item that was handed over
     */
    public static List<Output> order(Set<Output> connected, boolean hasFilter, boolean matches, Output filterOutput, int roundRobin) {
        List<Output> candidates = new ArrayList<>();
        for (Output output : Output.values()) {
            if (connected.contains(output)) {
                candidates.add(output);
            }
        }
        if (hasFilter) {
            if (matches && candidates.contains(filterOutput)) {
                return List.of(filterOutput);
            }
            List<Output> others = new ArrayList<>(candidates);
            others.remove(filterOutput);
            if (!others.isEmpty()) {
                candidates = others;
            }
        }
        return rotate(candidates, roundRobin);
    }

    private static List<Output> rotate(List<Output> outputs, int by) {
        List<Output> rotated = new ArrayList<>(outputs.size());
        for (int i = 0; i < outputs.size(); i++) {
            rotated.add(outputs.get(Math.floorMod(i + by, outputs.size())));
        }
        return rotated;
    }
}
