package de.craftorio.energy;

import java.util.ArrayList;
import java.util.List;

/** Splits power poles into networks: poles within {@code range} of each other (Euclidean) are wired together. */
public final class PoleGrouping {
    private PoleGrouping() {
    }

    /** @param positions pole coordinates as {x, y, z}; returns a network index per pole, numbered from 0 */
    public static int[] group(List<int[]> positions, double range) {
        double[] ranges = new double[positions.size()];
        java.util.Arrays.fill(ranges, range);
        return group(positions, ranges);
    }

    /** Like {@link #group(List, double)} with a wire range per pole; two poles are wired when within the shorter one. */
    public static int[] group(List<int[]> positions, double[] ranges) {
        int n = positions.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (connected(positions.get(i), positions.get(j), ranges[i], ranges[j])) {
                    parent[find(parent, i)] = find(parent, j);
                }
            }
        }
        int[] network = new int[n];
        List<Integer> roots = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int root = find(parent, i);
            int index = roots.indexOf(root);
            if (index < 0) {
                index = roots.size();
                roots.add(root);
            }
            network[i] = index;
        }
        return network;
    }

    public static boolean connected(int[] a, int[] b, double range) {
        return distanceSquared(a, b) <= range * range;
    }

    public static boolean connected(int[] a, int[] b, double rangeA, double rangeB) {
        return connected(a, b, Math.min(rangeA, rangeB));
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static double distanceSquared(int[] a, int[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return dx * dx + dy * dy + dz * dz;
    }
}
