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
