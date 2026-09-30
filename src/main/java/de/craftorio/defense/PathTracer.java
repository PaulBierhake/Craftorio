package de.craftorio.defense;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Follows the path blocks laid by the player from the enemy portal to the zone core. The path must be a single
 * line: every step has exactly one unvisited neighbour (horizontally adjacent, up to one block higher or lower).
 */
public final class PathTracer {
    /** The path window: the Bloons TD 6 balance assumes a fixed track length. */
    public static final int MIN_LENGTH = 100;
    public static final int MAX_LENGTH = 160;
    private static final int[][] HORIZONTAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private PathTracer() {
    }

    @FunctionalInterface
    public interface Grid {
        boolean test(int x, int y, int z);
    }

    public enum Error {
        NO_START, BRANCH, DEAD_END, TOO_SHORT, TOO_LONG
    }

    /** Either the path (list of {x, y, z}) or an error. */
    public record Result(List<int[]> path, Error error) {
        public boolean ok() {
            return error == null;
        }
    }

    /**
     * @param isPath is there a path block at these coordinates
     * @param isGoal is this path block next to the core
     */
    public static Result trace(int[] portal, Grid isPath, Grid isGoal) {
        List<int[]> starts = neighbours(portal, isPath, Set.of());
        if (starts.isEmpty()) {
            return new Result(List.of(), Error.NO_START);
        }
        if (starts.size() > 1) {
            return new Result(List.of(), Error.BRANCH);
        }
        List<int[]> path = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        int[] current = starts.get(0);
        while (true) {
            path.add(current);
            visited.add(key(current));
            if (path.size() > MAX_LENGTH) {
                return new Result(List.of(), Error.TOO_LONG);
            }
            if (isGoal.test(current[0], current[1], current[2])) {
                return path.size() < MIN_LENGTH ? new Result(List.of(), Error.TOO_SHORT) : new Result(List.copyOf(path), null);
            }
            List<int[]> next = neighbours(current, isPath, visited);
            if (next.isEmpty()) {
                return new Result(List.of(), Error.DEAD_END);
            }
            if (next.size() > 1) {
                return new Result(List.of(), Error.BRANCH);
            }
            current = next.get(0);
        }
    }

    /**
     * May a path block be set here? No path block may end up with more than two path neighbours (portal and core
     * count as neighbours), which rules out branches, junctions and 2×2 areas before the level is started.
     *
     * @param isPath     is there a path block at these coordinates (the new block itself may or may not be set yet)
     * @param isEndpoint is this the enemy portal or the core
     */
    public static boolean allowsBlock(int x, int y, int z, Grid isPath, Grid isEndpoint) {
        Grid after = (px, py, pz) -> px == x && py == y && pz == z || isPath.test(px, py, pz) || isEndpoint.test(px, py, pz);
        if (neighbours(new int[]{x, y, z}, after, Set.of()).size() > 2) {
            return false;
        }
        for (int[] near : neighbours(new int[]{x, y, z}, isPath, Set.of())) {
            if (neighbours(near, after, Set.of()).size() > 2) {
                return false;
            }
        }
        return true;
    }

    private static List<int[]> neighbours(int[] from, Grid isPath, Set<Long> visited) {
        List<int[]> found = new ArrayList<>();
        for (int[] step : HORIZONTAL) {
            for (int dy = -1; dy <= 1; dy++) {
                int[] candidate = {from[0] + step[0], from[1] + dy, from[2] + step[1]};
                if (!visited.contains(key(candidate)) && isPath.test(candidate[0], candidate[1], candidate[2])) {
                    found.add(candidate);
                    break; // at most one height per column
                }
            }
        }
        return found;
    }

    private static long key(int[] pos) {
        return ((long) (pos[0] & 0x3FFFFFF) << 38) | ((long) (pos[2] & 0x3FFFFFF) << 12) | (pos[1] & 0xFFF);
    }
}
