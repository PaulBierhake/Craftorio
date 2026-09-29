package de.craftorio.logistics;

/** Belt generations (Factorio speeds). Belts of different tiers connect freely. */
public enum BeltTier {
    BASIC(1.875F),
    FAST(3.75F),
    EXPRESS(5.625F);

    private final float blocksPerSecond;

    BeltTier(float blocksPerSecond) {
        this.blocksPerSecond = blocksPerSecond;
    }

    /** Blocks between the two ends of an underground belt: 4 for the yellow belt, 6 for the red one (Factorio). */
    public int undergroundGap() {
        return switch (this) {
            case BASIC -> 4;
            case FAST -> 6;
            case EXPRESS -> 8;
        };
    }

    public float blocksPerSecond() {
        return blocksPerSecond;
    }

    /** Lane progress per tick (one block = 1.0). */
    public float speedPerTick() {
        return blocksPerSecond / 20F;
    }

    /** Items per second one lane can move when it is packed at minimum spacing. */
    public float itemsPerSecondPerLane() {
        return blocksPerSecond / BeltLane.SPACING;
    }
}
