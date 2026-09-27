package de.craftorio.defense;

import java.util.Comparator;

/** Which enemy in range a tower shoots at. */
public enum TargetMode {
    /** Furthest along the path. */
    FIRST(Comparator.comparingDouble(TdEnemy::progress).reversed()),
    /** Least far along the path. */
    LAST(Comparator.comparingDouble(TdEnemy::progress)),
    /** Most health left. */
    STRONGEST(Comparator.comparingDouble(TdEnemy::getHealth).reversed()),
    /** Least health left – finishes off wounded enemies. */
    WEAKEST(Comparator.comparingDouble(TdEnemy::getHealth));

    private final Comparator<TdEnemy> order;

    TargetMode(Comparator<TdEnemy> order) {
        this.order = order;
    }

    public Comparator<TdEnemy> order() {
        return order;
    }

    public TargetMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
