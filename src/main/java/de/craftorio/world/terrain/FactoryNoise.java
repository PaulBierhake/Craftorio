package de.craftorio.world.terrain;

import java.util.SplittableRandom;

/**
 * Smooth 2D gradient noise with octaves, seeded by a long, in plain Java so the terrain can be tested without Minecraft.
 * Values lie roughly between -1 and 1 and are centred on 0.
 */
public final class FactoryNoise {
    private static final int SIZE = 256;
    private static final double[][] GRADIENTS;

    static {
        GRADIENTS = new double[8][2];
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8 + Math.PI / 8;
            GRADIENTS[i][0] = Math.cos(angle);
            GRADIENTS[i][1] = Math.sin(angle);
        }
    }

    private final int[] permutation = new int[SIZE * 2];
    private final double wavelength;
    private final int octaves;
    private final double norm;

    /**
     * @param wavelength blocks per period of the first octave
     * @param octaves    number of octaves; every one has half the wavelength and half the weight
     */
    public FactoryNoise(long seed, double wavelength, int octaves) {
        SplittableRandom random = new SplittableRandom(seed);
        int[] base = new int[SIZE];
        for (int i = 0; i < SIZE; i++) {
            base[i] = i;
        }
        for (int i = SIZE - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = base[i];
            base[i] = base[j];
            base[j] = swap;
        }
        for (int i = 0; i < SIZE * 2; i++) {
            permutation[i] = base[i & (SIZE - 1)];
        }
        this.wavelength = wavelength;
        this.octaves = octaves;
        double total = 0;
        double weight = 1;
        for (int i = 0; i < octaves; i++) {
            total += weight;
            weight *= 0.5;
        }
        // single-octave gradient noise spans about +-0.7; scale the sum back to about +-1
        this.norm = 1.0 / (total * 0.7);
    }

    public double at(double x, double z) {
        double sum = 0;
        double frequency = 1.0 / wavelength;
        double weight = 1;
        for (int i = 0; i < octaves; i++) {
            sum += weight * single(x * frequency + i * 17.31, z * frequency - i * 11.7);
            frequency *= 2;
            weight *= 0.5;
        }
        return Math.max(-1, Math.min(1, sum * norm));
    }

    private double single(double x, double z) {
        int xi = (int) Math.floor(x);
        int zi = (int) Math.floor(z);
        double xf = x - xi;
        double zf = z - zi;
        double u = fade(xf);
        double v = fade(zf);
        int ix = xi & (SIZE - 1);
        int iz = zi & (SIZE - 1);
        double n00 = dot(permutation[permutation[ix] + iz], xf, zf);
        double n10 = dot(permutation[permutation[ix + 1] + iz], xf - 1, zf);
        double n01 = dot(permutation[permutation[ix] + iz + 1], xf, zf - 1);
        double n11 = dot(permutation[permutation[ix + 1] + iz + 1], xf - 1, zf - 1);
        double top = n00 + u * (n10 - n00);
        double bottom = n01 + u * (n11 - n01);
        return top + v * (bottom - top);
    }

    private static double dot(int hash, double x, double z) {
        double[] gradient = GRADIENTS[hash & 7];
        return gradient[0] * x + gradient[1] * z;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
}
