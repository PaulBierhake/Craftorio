package de.craftorio.world.terrain;

import java.util.function.IntBinaryOperator;

/**
 * Pure rules for the finishing touches of the factory world: which columns are cliff edges or shores, where trees do not
 * stand. The heights are the first free block above the ground ({@code ground + 1}), as the world generator's
 * heightmaps give them.
 */
public final class EdgeRules {
    /** Around the spawn no trees stand (an open building ground), in blocks. */
    public static final int TREE_FREE_RADIUS = 64;
    /** A bed at or below this height (top block 62) belongs to a lake or a river. */
    private static final int WATER_BED_HEIGHT = FactoryTerrain.SEA_LEVEL - 1;

    public enum Shore {
        NONE, SHORE, BED
    }

    private EdgeRules() {
    }

    /** True if a neighbouring column (8 around) has another height: the column is on a cliff edge or a shore. */
    public static boolean onEdge(IntBinaryOperator height, int x, int z) {
        int here = height.applyAsInt(x, z);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && height.applyAsInt(x + dx, z + dz) != here) {
                    return true;
                }
            }
        }
        return false;
    }

    /** True if no tree stands here: on edges and around the spawn. */
    public static boolean treeFree(IntBinaryOperator height, int x, int z) {
        return (long) x * x + (long) z * z <= (long) TREE_FREE_RADIUS * TREE_FREE_RADIUS || onEdge(height, x, z);
    }

    /**
     * Whether the column is the bed of a lake or river ({@code BED}), a ground column right at the water ({@code SHORE},
     * within 2 blocks of a bed) or neither.
     */
    public static Shore shore(IntBinaryOperator height, int x, int z) {
        int here = height.applyAsInt(x, z);
        if (here - 1 <= WATER_BED_HEIGHT) {
            return Shore.BED;
        }
        if (here - 1 != FactoryTerrain.GROUND) {
            return Shore.NONE;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (height.applyAsInt(x + dx, z + dz) - 1 <= WATER_BED_HEIGHT) {
                    return Shore.SHORE;
                }
            }
        }
        return Shore.NONE;
    }

    /** Sand or gravel for a bed column, from a stable hash of the position (about one in four is gravel). */
    public static boolean gravel(int x, int z) {
        int h = x * 73856093 ^ z * 19349663;
        return Math.floorMod(h ^ (h >>> 13), 4) == 0;
    }
}
