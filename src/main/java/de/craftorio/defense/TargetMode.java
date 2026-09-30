package de.craftorio.defense;

import de.craftorio.defense.sim.SimEnemy;

import java.util.Comparator;

/** Which enemy in range a tower shoots at. */
public enum TargetMode {
    /** Furthest along the path. */
    FIRST(Comparator.comparingDouble(SimEnemy::distance).reversed().thenComparingInt(SimEnemy::id)),
    /** Least far along the path. */
    LAST(Comparator.comparingDouble(SimEnemy::distance).thenComparingInt(SimEnemy::id)),
    /** Most RBE left. */
    STRONGEST(Comparator.comparingDouble(SimEnemy::rbe).reversed().thenComparingInt(SimEnemy::id)),
    /** Least RBE left – finishes off wounded enemies. */
    WEAKEST(Comparator.comparingDouble(SimEnemy::rbe).thenComparingInt(SimEnemy::id));

    private final Comparator<SimEnemy> order;

    TargetMode(Comparator<SimEnemy> order) {
        this.order = order;
    }

    public Comparator<SimEnemy> order() {
        return order;
    }

    public TargetMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
