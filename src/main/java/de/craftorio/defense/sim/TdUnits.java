package de.craftorio.defense.sim;

/** The scale between Bloons TD 6 and the arena: one block is ten BTD6 units, a second is twenty ticks. */
public final class TdUnits {
    public static final double UNITS_PER_BLOCK = 10;
    public static final int TICKS_PER_SECOND = 20;

    private TdUnits() {
    }

    /** Blocks per tick for a speed in BTD6 units per second. */
    public static double blocksPerTick(double unitsPerSecond) {
        return unitsPerSecond / UNITS_PER_BLOCK / TICKS_PER_SECOND;
    }

    public static int ticks(double seconds) {
        return (int) Math.round(seconds * TICKS_PER_SECOND);
    }
}
