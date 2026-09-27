package de.craftorio.world.cave;

/**
 * The fixed height bands of the world. Surface above {@link #SURFACE_BOTTOM}; cap rock I; the cave layer; cap
 * rock II; the mine layer down to the bedrock band. Cave and mine layer start as unbreakable fill and only become
 * rock and open halls once an entrance (cave entrance / mine shaft) unlocks the area.
 */
public final class CaveLayers {
    public static final int SURFACE_BOTTOM = 50;
    public static final int CAP_ONE_BOTTOM = 40;
    public static final int CAVE_BOTTOM = 0;
    public static final int CAP_TWO_BOTTOM = -10;
    /** Below this the vanilla bedrock band stays untouched. */
    public static final int MINE_BOTTOM = -59;

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
        FILL,
        /** Always mine fill. */
        MINE_FILL
    }

    private CaveLayers() {
    }

    public static Rule rule(int y) {
        if (y >= SURFACE_BOTTOM || y < MINE_BOTTOM) {
            return Rule.KEEP;
        }
        if (y >= CAP_ONE_BOTTOM) {
            return Rule.CAP_IF_SOLID;
        }
        if (y >= CAVE_BOTTOM) {
            return Rule.FILL;
        }
        return y >= CAP_TWO_BOTTOM ? Rule.CAP : Rule.MINE_FILL;
    }

    public static boolean inCaveLayer(int y) {
        return y >= CAVE_BOTTOM && y < CAP_ONE_BOTTOM;
    }
}
