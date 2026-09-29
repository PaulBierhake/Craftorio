package de.craftorio.logistics;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Which outputs a splitter offers an item, in the order they are tried. Without settings items alternate between
 * front, left and right; a priority output is tried first; with a filter, matching items only go to the front and
 * everything else alternates between left and right. Pure logic for unit tests.
 */
public final class SplitterLogic {
    public enum Output {
        FRONT, LEFT, RIGHT
    }

    private SplitterLogic() {
    }

    /**
     * @param hasFilter    a filter item is set
     * @param matches      the item matches the filter (ignored without a filter)
     * @param priority     the preferred output, or null
     * @param roundRobin   counter that advances after every item that was handed over
     */
    public static List<Output> order(boolean hasFilter, boolean matches, @Nullable Output priority, int roundRobin) {
        if (hasFilter) {
            if (matches) {
                return List.of(Output.FRONT);
            }
            return rotate(List.of(Output.LEFT, Output.RIGHT), roundRobin);
        }
        List<Output> all = rotate(List.of(Output.FRONT, Output.LEFT, Output.RIGHT), roundRobin);
        if (priority == null) {
            return all;
        }
        List<Output> ordered = new ArrayList<>();
        ordered.add(priority);
        all.stream().filter(output -> output != priority).forEach(ordered::add);
        return ordered;
    }

    private static List<Output> rotate(List<Output> outputs, int by) {
        List<Output> rotated = new ArrayList<>(outputs.size());
        for (int i = 0; i < outputs.size(); i++) {
            rotated.add(outputs.get(Math.floorMod(i + by, outputs.size())));
        }
        return rotated;
    }
}
