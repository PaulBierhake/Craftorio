package de.craftorio.energy;

/** The power poles: how far a pole's wire reaches and how far around it machines are supplied (a square of edge {@code 2 * supplyRadius + 1}). */
public enum PoleTier {
    /** Wood and copper cable: supplies 5×5. */
    SMALL(8.0, 2),
    /** Steel: supplies 7×7 and wires 9 blocks, as in Factorio. */
    MEDIUM(9.0, 3);

    private final double wireRange;
    private final int supplyRadius;

    PoleTier(double wireRange, int supplyRadius) {
        this.wireRange = wireRange;
        this.supplyRadius = supplyRadius;
    }

    public double wireRange() {
        return wireRange;
    }

    public int supplyRadius() {
        return supplyRadius;
    }

    /** The longest wire any pole reaches. */
    public static double maxWireRange() {
        return MEDIUM.wireRange;
    }
}
