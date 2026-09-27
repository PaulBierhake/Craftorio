package de.craftorio.machine;

/**
 * Square mining area under a drill. Areas of two drills on the same layer must not overlap, so each ore field
 * block feeds at most one drill and a field's total output is capped by its size.
 */
public final class DrillArea {
    private DrillArea() {
    }

    /** True if the square areas (radius 1 = 3x3, 2 = 5x5) around two drills share at least one block. */
    public static boolean overlaps(int x1, int y1, int z1, int radius1, int x2, int y2, int z2, int radius2) {
        if (y1 != y2) {
            return false;
        }
        int reach = radius1 + radius2;
        return Math.abs(x1 - x2) <= reach && Math.abs(z1 - z2) <= reach;
    }
}
