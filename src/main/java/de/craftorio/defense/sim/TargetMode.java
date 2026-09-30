package de.craftorio.defense.sim;

import java.util.Comparator;
import java.util.Locale;

/** Which enemy in range a tower shoots at (Bloons TD 6: First, Last, Close, Strong). */
public enum TargetMode {
    /** Furthest along the path. */
    FIRST,
    /** Least far along the path. */
    LAST,
    /** Nearest to the tower. */
    CLOSE,
    /** Most RBE left. */
    STRONG;

    /** Orders the enemies best first for a tower at the given place. */
    public Comparator<SimEnemy> order(double x, double y, double z) {
        return switch (this) {
            case FIRST -> Comparator.comparingDouble(SimEnemy::distance).reversed().thenComparingInt(SimEnemy::id);
            case LAST -> Comparator.comparingDouble(SimEnemy::distance).thenComparingInt(SimEnemy::id);
            case CLOSE -> Comparator.<SimEnemy>comparingDouble(enemy -> enemy.distanceSqr(x, y, z)).thenComparingInt(SimEnemy::id);
            case STRONG -> Comparator.comparingDouble(SimEnemy::rbe).reversed().thenComparingInt(SimEnemy::id);
        };
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static TargetMode byId(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }

    /** The next mode among those a tower offers (wraps around); the current one if there is only one. */
    public static TargetMode next(TargetMode current, java.util.List<TargetMode> offered) {
        if (offered.isEmpty()) {
            return current;
        }
        int index = offered.indexOf(current);
        return offered.get((index + 1) % offered.size());
    }
}
