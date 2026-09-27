package de.craftorio.world.cave;

/**
 * Shape of an underground layer: one large, connected hall with an uneven floor and ceiling and scattered rock
 * pillars. Deterministic from the world seed, so neighbouring unlocked areas join seamlessly.
 */
public final class CaveShape {
    /**
     * @param floorBase    average floor height
     * @param ceilingBase  average ceiling height
     * @param pillarThreshold noise value above which a column is a pillar (lower = more pillars)
     * @param salt         distinguishes the noise of different layers
     */
    public record Params(double floorBase, double floorAmplitude, double ceilingBase, double ceilingAmplitude,
                         double pillarThreshold, long salt) {
    }

    private final Params params;
    private final Noise2D floor;
    private final Noise2D ceiling;
    private final Noise2D pillars;

    public CaveShape(long seed, Params params) {
        this.params = params;
        long base = seed ^ 0x43726166746F7269L ^ (params.salt() * 0x9E3779B97F4A7C15L);
        this.floor = new Noise2D(base);
        this.ceiling = new Noise2D(base * 31 + 1);
        this.pillars = new Noise2D(base * 31 * 31 + 2);
    }

    public static CaveShape of(long seed, Layer layer) {
        return new CaveShape(seed, layer.shape());
    }

    public int floorY(int x, int z) {
        return (int) Math.round(params.floorBase() + params.floorAmplitude() * floor.noise(x * 0.045, z * 0.045));
    }

    public int ceilingY(int x, int z) {
        return (int) Math.round(params.ceilingBase() + params.ceilingAmplitude() * ceiling.noise(x * 0.03, z * 0.03));
    }

    public boolean isPillar(int x, int z) {
        return pillars.noise(x * 0.11, z * 0.11) > params.pillarThreshold();
    }

    /** Open air inside the hall (the floor block itself is solid). */
    public boolean isOpen(int x, int y, int z) {
        return !isPillar(x, z) && y > floorY(x, z) && y < ceilingY(x, z);
    }
}
