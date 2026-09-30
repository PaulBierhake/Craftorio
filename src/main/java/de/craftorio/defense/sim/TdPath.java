package de.craftorio.defense.sim;

import java.util.List;

/** The walk points of a path in world coordinates; enemies are a distance in blocks along it. */
public final class TdPath {
    private final double[] x;
    private final double[] y;
    private final double[] z;
    private final double[] start;
    private final double length;

    /** @param points at least two {x, y, z} points */
    public TdPath(List<double[]> points) {
        if (points.size() < 2) {
            throw new IllegalArgumentException("a path needs at least two points");
        }
        int n = points.size();
        x = new double[n];
        y = new double[n];
        z = new double[n];
        start = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = points.get(i)[0];
            y[i] = points.get(i)[1];
            z[i] = points.get(i)[2];
            if (i > 0) {
                start[i] = start[i - 1] + Math.sqrt(sq(x[i] - x[i - 1]) + sq(y[i] - y[i - 1]) + sq(z[i] - z[i - 1]));
            }
        }
        length = start[n - 1];
    }

    private static double sq(double value) {
        return value * value;
    }

    public int size() {
        return x.length;
    }

    /** Length of the whole path in blocks. */
    public double length() {
        return length;
    }

    public double[] point(int index) {
        return new double[]{x[index], y[index], z[index]};
    }

    /** Writes the position at the given distance into {@code out} ({x, y, z}); returns the segment index for the next call as a hint. */
    public int position(double distance, int hint, double[] out) {
        double d = Math.max(0, Math.min(length, distance));
        int i = Math.max(0, Math.min(x.length - 2, hint));
        while (i > 0 && start[i] > d) {
            i--;
        }
        while (i < x.length - 2 && start[i + 1] < d) {
            i++;
        }
        double span = start[i + 1] - start[i];
        double t = span <= 0 ? 0 : (d - start[i]) / span;
        out[0] = x[i] + (x[i + 1] - x[i]) * t;
        out[1] = y[i] + (y[i + 1] - y[i]) * t;
        out[2] = z[i] + (z[i + 1] - z[i]) * t;
        return i;
    }

    public double[] position(double distance) {
        double[] out = new double[3];
        position(distance, 0, out);
        return out;
    }
}
