package de.craftorio.world.cave;

import java.util.SplittableRandom;

/** Classic 2D Perlin gradient noise in [-1, 1], seeded; plain Java so it can be unit tested. */
public final class Noise2D {
    private final int[] permutation = new int[512];

    public Noise2D(long seed) {
        int[] base = new int[256];
        for (int i = 0; i < 256; i++) {
            base[i] = i;
        }
        SplittableRandom random = new SplittableRandom(seed);
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = base[i];
            base[i] = base[j];
            base[j] = swap;
        }
        for (int i = 0; i < 512; i++) {
            permutation[i] = base[i & 255];
        }
    }

    public double noise(double x, double y) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;
        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);
        double u = fade(xf);
        double v = fade(yf);
        int aa = permutation[permutation[xi] + yi];
        int ab = permutation[permutation[xi] + yi + 1];
        int ba = permutation[permutation[xi + 1] + yi];
        int bb = permutation[permutation[xi + 1] + yi + 1];
        double x1 = lerp(u, gradient(aa, xf, yf), gradient(ba, xf - 1, yf));
        double x2 = lerp(u, gradient(ab, xf, yf - 1), gradient(bb, xf - 1, yf - 1));
        // Perlin 2D output lies within about [-0.71, 0.71]; scale to roughly [-1, 1].
        return Math.max(-1, Math.min(1, lerp(v, x1, x2) * 1.41));
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double gradient(int hash, double x, double y) {
        return switch (hash & 7) {
            case 0 -> x + y;
            case 1 -> -x + y;
            case 2 -> x - y;
            case 3 -> -x - y;
            case 4 -> x;
            case 5 -> -x;
            case 6 -> y;
            default -> -y;
        };
    }
}
