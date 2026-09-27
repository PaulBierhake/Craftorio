package de.craftorio.world.cave;

/**
 * Shape of the cave layer: one large, connected hall with an uneven floor and ceiling and scattered rock pillars.
 * Deterministic from the world seed, so neighbouring unlocked areas join seamlessly.
 */
public final class CaveShape {
    /** Floor between 4 and 10, ceiling between 16 and 34: always at least 6 blocks of headroom. */
    private static final double FLOOR_BASE = 7;
    private static final double FLOOR_AMPLITUDE = 3;
    private static final double CEILING_BASE = 25;
    private static final double CEILING_AMPLITUDE = 9;
    private static final double PILLAR_THRESHOLD = 0.42;

    private final Noise2D floor;
    private final Noise2D ceiling;
    private final Noise2D pillars;

    public CaveShape(long seed) {
        long base = seed ^ 0x43726166746F7269L;
        this.floor = new Noise2D(base);
        this.ceiling = new Noise2D(base * 31 + 1);
        this.pillars = new Noise2D(base * 31 * 31 + 2);
    }

    public int floorY(int x, int z) {
        return (int) Math.round(FLOOR_BASE + FLOOR_AMPLITUDE * floor.noise(x * 0.045, z * 0.045));
    }

    public int ceilingY(int x, int z) {
        return (int) Math.round(CEILING_BASE + CEILING_AMPLITUDE * ceiling.noise(x * 0.03, z * 0.03));
    }

    public boolean isPillar(int x, int z) {
        return pillars.noise(x * 0.11, z * 0.11) > PILLAR_THRESHOLD;
    }

    /** Open air inside the hall (the floor block itself is solid). */
    public boolean isOpen(int x, int y, int z) {
        return !isPillar(x, z) && y > floorY(x, z) && y < ceilingY(x, z);
    }
}
