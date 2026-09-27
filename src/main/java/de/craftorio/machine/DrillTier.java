package de.craftorio.machine;

/** Drill generations: bigger area and faster mining per field block, powered by fuel or the grid. */
public enum DrillTier {
    /** 3x3, burns furnace fuel. */
    BURNER(1, 0.03, 0),
    /** 3x3, twice as fast, grid powered. */
    ELECTRIC(1, 0.06, 30),
    /** 5x5, four times as fast per block, grid powered – for the mine layer. */
    DEEP(2, 0.12, 80);

    /** Largest radius of any tier; neighbours further away than twice this can never overlap. */
    public static final int MAX_RADIUS = 2;

    private final int radius;
    private final double itemsPerBlockPerSecond;
    private final int energyPerTick;

    DrillTier(int radius, double itemsPerBlockPerSecond, int energyPerTick) {
        this.radius = radius;
        this.itemsPerBlockPerSecond = itemsPerBlockPerSecond;
        this.energyPerTick = energyPerTick;
    }

    public int radius() {
        return radius;
    }

    public int area() {
        return (2 * radius + 1) * (2 * radius + 1);
    }

    public double itemsPerBlockPerSecond() {
        return itemsPerBlockPerSecond;
    }

    public int energyPerTick() {
        return energyPerTick;
    }

    public boolean usesFuel() {
        return energyPerTick == 0;
    }

    /** Items per second on a fully covered field. */
    public double maxItemsPerSecond() {
        return area() * itemsPerBlockPerSecond;
    }
}
