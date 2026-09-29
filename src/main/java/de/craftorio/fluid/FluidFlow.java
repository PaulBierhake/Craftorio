package de.craftorio.fluid;

/** How much fluid flows between two containers in one tick; no Minecraft classes so it can be unit tested. */
public final class FluidFlow {
    /** The most one connection moves per tick (a pipe holds 100 units; Factorio's pumps move 1200 units/s = 60/tick). */
    public static final int MAX_FLOW = 60;

    private FluidFlow() {
    }

    /**
     * Amount that flows from container A to container B: the difference of the fill levels (as fractions of the
     * capacity) is halved, so two connected containers settle at the same level. Never more than A holds or B has room for.
     */
    public static int move(int amountA, int capacityA, int amountB, int capacityB, int maxFlow) {
        if (amountA <= 0 || capacityA <= 0 || capacityB <= 0) {
            return 0;
        }
        double difference = (double) amountA / capacityA - (double) amountB / capacityB;
        if (difference <= 0) {
            return 0;
        }
        int moved = (int) Math.floor(difference * Math.min(capacityA, capacityB) / 2);
        return Math.max(0, Math.min(Math.min(moved, maxFlow), Math.min(amountA, capacityB - amountB)));
    }
}
