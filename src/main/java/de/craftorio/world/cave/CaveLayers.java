package de.craftorio.world.cave;

/**
 * The fixed height bands of the world. Surface above {@link #SURFACE_BOTTOM}; cap rock I; the cave layer; cap
 * rock II; the mine layer (M7). Everything in the cave layer starts as unbreakable cave fill and only becomes
 * rock and open caverns once a cave entrance unlocks the area.
 */
public final class CaveLayers {
    public static final int SURFACE_BOTTOM = 50;
    public static final int CAP_ONE_BOTTOM = 40;
    public static final int CAVE_BOTTOM = 0;
    public static final int CAP_TWO_BOTTOM = -10;

    /** Radius in chunks unlocked around a cave entrance (3 = 7x7 chunks). */
    public static final int UNLOCK_RADIUS = 3;

    public enum Rule {
        /** Leave the generated block as it is. */
        KEEP,
        /** Replace solid blocks with cap rock, leave air and fluids (oceans may reach below the surface band). */
        CAP_IF_SOLID,
        /** Always cap rock. */
        CAP,
        /** Always cave fill. */
        FILL
    }

    private CaveLayers() {
    }

    public static Rule rule(int y) {
        if (y >= SURFACE_BOTTOM || y < CAP_TWO_BOTTOM) {
            return Rule.KEEP;
        }
        if (y >= CAP_ONE_BOTTOM) {
            return Rule.CAP_IF_SOLID;
        }
        if (y >= CAVE_BOTTOM) {
            return Rule.FILL;
        }
        return Rule.CAP;
    }

    public static boolean inCaveLayer(int y) {
        return y >= CAVE_BOTTOM && y < CAP_ONE_BOTTOM;
    }
}
