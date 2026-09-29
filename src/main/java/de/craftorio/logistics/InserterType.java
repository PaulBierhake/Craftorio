package de.craftorio.logistics;

/** Inserter generations with the swing times of Factorio's inserters: 0.83, 1.15 and 2.31 items per second. */
public enum InserterType {
    /** Reaches one block, takes any item. */
    BASIC(24, 1, false),
    /** Reaches two blocks on either side. */
    LONG(17, 2, false),
    FAST(9, 1, false),
    /** Fast, and only moves the item set with a right-click. */
    FILTER(9, 1, true);

    private final int swingTicks;
    private final int reach;
    private final boolean filter;

    InserterType(int swingTicks, int reach, boolean filter) {
        this.swingTicks = swingTicks;
        this.reach = reach;
        this.filter = filter;
    }

    public int swingTicks() {
        return swingTicks;
    }

    /** Blocks between the inserter and the block it takes from or gives to (1 = directly adjacent). */
    public int reach() {
        return reach;
    }

    public boolean hasFilter() {
        return filter;
    }
}
