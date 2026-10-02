package de.craftorio.world.terrain;

/**
 * The surface of the factory world: every column has exactly one of a few fixed heights, with vertical edges between them
 * and no slopes. Pure logic (no Minecraft), the density function {@link FactoryHeight} only asks it for the height.
 *
 * <p>The land is decided on a grid of 4 × 4 block cells and then smoothed with a majority filter over the 5 × 5 cells around
 * each cell, so no area of one level is smaller than about 12 × 12 blocks. Rivers are the exception: they are thin by design.
 */
public final class FactoryTerrain {
    public static final int GROUND = 64;
    public static final int PLATEAU_ONE = 68;
    public static final int PLATEAU_TWO = 72;
    /** The sea level setting of the world: water fills every block below it, so the top water block is y = 63. */
    public static final int SEA_LEVEL = 64;
    /** Lake bed heights from the shore to the deepest part, and the river bed. */
    public static final int[] LAKE_BEDS = {61, 60, 59};
    public static final int[] RIVER_BEDS = {61, 60};
    public static final int CELL = 4;

    /** How many plateaus there are: none, rare (the default) or normal. */
    public enum Plateaus {
        NONE(2.0, 2.0),
        RARE(0.225, 0.395),
        NORMAL(0.07, 0.30);

        final double one;
        final double two;

        Plateaus(double one, double two) {
            this.one = one;
            this.two = two;
        }
    }

    /** What the surface of a cell is. */
    enum Kind {
        GROUND, PLATEAU_ONE, PLATEAU_TWO, LAKE, RIVER
    }

    // thresholds of the noise fields, calibrated for the shares of the concept (see FactoryTerrainTest)
    private static final double LAKE_LIMIT = -0.44;
    private static final double LAKE_STEP = 0.05;
    private static final double RIVER_WIDTH = 0.020;

    private final FactoryNoise plateau;
    private final FactoryNoise lake;
    private final FactoryNoise river;
    private final Plateaus plateaus;
    private final int spawnRadius;
    private final ThreadLocal<Cache> cache = ThreadLocal.withInitial(Cache::new);

    public FactoryTerrain(long seed, Plateaus plateaus, int spawnRadius) {
        this.plateau = new FactoryNoise(seed * 31 + 1, 260, 3);
        this.lake = new FactoryNoise(seed * 31 + 2, 200, 3);
        this.river = new FactoryNoise(seed * 31 + 3, 520, 2);
        this.plateaus = plateaus;
        this.spawnRadius = spawnRadius;
    }

    public FactoryTerrain(long seed) {
        this(seed, Plateaus.RARE, 192);
    }

    /** The top block of the surface in this column. */
    public int height(int x, int z) {
        long key = key(Math.floorDiv(x, CELL), Math.floorDiv(z, CELL));
        return cache.get().height(this, key, Math.floorDiv(x, CELL), Math.floorDiv(z, CELL));
    }

    private static long key(int cellX, int cellZ) {
        return ((long) cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
    }

    /** The raw kind of a cell (no smoothing), from the noise at its centre. */
    private int rawCode(int cellX, int cellZ) {
        int x = cellX * CELL + CELL / 2;
        int z = cellZ * CELL + CELL / 2;
        double distance = Math.sqrt((double) x * x + (double) z * z);
        double p = plateau.at(x, z);
        if (distance < spawnRadius + CELL) {
            return Kind.GROUND.ordinal();
        }
        if (p > plateaus.two) {
            return Kind.PLATEAU_TWO.ordinal();
        }
        if (p > plateaus.one) {
            return Kind.PLATEAU_ONE.ordinal();
        }
        if (distance < spawnRadius) {
            return Kind.GROUND.ordinal();
        }
        if (distance >= spawnRadius + 80) {
            double l = lake.at(x, z);
            if (l < LAKE_LIMIT) {
                return Kind.LAKE.ordinal() | (depth(l) << 8);
            }
        }
        return Kind.GROUND.ordinal();
    }

    private static int depth(double lakeValue) {
        if (lakeValue < LAKE_LIMIT - 2 * LAKE_STEP) {
            return 2;
        }
        return lakeValue < LAKE_LIMIT - LAKE_STEP ? 1 : 0;
    }

    /** The river bed height in this cell, or 0 if the cell is no river. */
    private int riverBed(int cellX, int cellZ) {
        int x = cellX * CELL + CELL / 2;
        int z = cellZ * CELL + CELL / 2;
        if (Math.sqrt((double) x * x + (double) z * z) < spawnRadius + 80) {
            return 0;
        }
        double distance = Math.abs(river.at(x, z));
        return distance < RIVER_WIDTH * 0.5 ? RIVER_BEDS[1] : distance < RIVER_WIDTH ? RIVER_BEDS[0] : 0;
    }

    /** The height of a cell after the second pass: cells with hardly any equal neighbour take the height of the most common one. */
    private int pass(Cache cache, int layer, int cellX, int cellZ) {
        int own = cache.layer(this, layer - 1, cellX, cellZ);
        int[] heights = new int[8];
        int[] counts = new int[8];
        int kinds = 0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int h = cache.layer(this, layer - 1, cellX + dx, cellZ + dz);
                int i = 0;
                while (i < kinds && heights[i] != h) {
                    i++;
                }
                if (i == kinds) {
                    heights[kinds++] = h;
                }
                counts[i]++;
            }
        }
        int ownCount = 0;
        int best = 0;
        for (int i = 0; i < kinds; i++) {
            if (heights[i] == own) {
                ownCount = counts[i];
            }
            if (counts[i] > counts[best]) {
                best = i;
            }
        }
        return ownCount <= 3 && counts[best] >= ownCount && heights[best] != own ? heights[best] : own;
    }

    /** The height of a cell after the first smoothing. */
    private int stageHeight(Cache cache, int cellX, int cellZ) {
        int centreX = cellX * CELL + CELL / 2;
        int centreZ = cellZ * CELL + CELL / 2;
        double keepClear = spawnRadius + CELL;
        if ((double) centreX * centreX + (double) centreZ * centreZ < keepClear * keepClear) {
            return GROUND;
        }
        int[] votes = new int[4];
        int[] depthOfLake = new int[3];
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                int code = cache.raw(this, cellX + dx, cellZ + dz);
                int kind = code & 0xFF;
                int weight = Math.abs(dx) <= 1 && Math.abs(dz) <= 1 ? 2 : 1;
                int vote = kind == Kind.PLATEAU_ONE.ordinal() ? 1 : kind == Kind.PLATEAU_TWO.ordinal() ? 2 : kind == Kind.LAKE.ordinal() ? 3 : 0;
                votes[vote] += weight;
            }
        }
        // the depth of a lake is the most common depth in the 5 x 5 cells around: it changes in broad steps
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                int code = cache.raw(this, cellX + dx, cellZ + dz);
                if ((code & 0xFF) == Kind.LAKE.ordinal()) {
                    depthOfLake[code >> 8]++;
                }
            }
        }
        int best = 0;
        for (int i = 1; i < 4; i++) {
            if (votes[i] > votes[best]) {
                best = i;
            }
        }
        // plateau two needs plateau one around it: a vote for two counts for one as well
        if (best == 1 && votes[2] > 0 && votes[1] + votes[2] >= 12 && votes[2] * 2 >= votes[1]) {
            best = 2;
        }
        return switch (best) {
            case 1 -> PLATEAU_ONE;
            case 2 -> PLATEAU_TWO;
            case 3 -> {
                int depth = 0;
                for (int i = 1; i < 3; i++) {
                    if (depthOfLake[i] > depthOfLake[depth]) {
                        depth = i;
                    }
                }
                yield LAKE_BEDS[depth];
            }
            default -> {
                int bed = cache.river(this, cellX, cellZ);
                if (bed != 0) {
                    // a river is at least two cells wide: lone cells are dropped
                    int neighbours = 0;
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if ((dx != 0 || dz != 0) && cache.river(this, cellX + dx, cellZ + dz) != 0) {
                                neighbours++;
                            }
                        }
                    }
                    if (neighbours < 3) {
                        bed = 0;
                    }
                }
                yield bed == 0 ? GROUND : bed;
            }
        };
    }

    /** A per-thread cache of cells, so a column costs a lookup and a cell a handful of noise values. */
    private static final class Cache {
        private static final int BITS = 14;
        private static final int MASK = (1 << BITS) - 1;
        private final long[] heightKeys = new long[1 << BITS];
        private final int[] heights = new int[1 << BITS];
        private final long[][] layerKeys = new long[4][1 << BITS];
        private final int[][] layers = new int[4][1 << BITS];
        private final long[] rawKeys = new long[1 << BITS];
        private final int[] raws = new int[1 << BITS];
        private final long[] riverKeys = new long[1 << BITS];
        private final int[] rivers = new int[1 << BITS];

        Cache() {
            java.util.Arrays.fill(heightKeys, Long.MIN_VALUE);
            for (long[] keys : layerKeys) {
                java.util.Arrays.fill(keys, Long.MIN_VALUE);
            }
            java.util.Arrays.fill(rawKeys, Long.MIN_VALUE);
            java.util.Arrays.fill(riverKeys, Long.MIN_VALUE);
        }

        private static int slot(long key) {
            long h = key * 0x9E3779B97F4A7C15L;
            return (int) (h >>> 40) & MASK;
        }

        int height(FactoryTerrain terrain, long key, int cellX, int cellZ) {
            int slot = slot(key);
            if (heightKeys[slot] == key) {
                return heights[slot];
            }
            int value = terrain.pass(this, 3, cellX, cellZ);
            heightKeys[slot] = key;
            heights[slot] = value;
            return value;
        }

        /** Layer 0 is the first smoothing, 1 and 2 are passes that remove what is left of lone cells and thin lines. */
        int layer(FactoryTerrain terrain, int layer, int cellX, int cellZ) {
            long key = key(cellX, cellZ);
            int slot = slot(key);
            if (layerKeys[layer][slot] == key) {
                return layers[layer][slot];
            }
            int value = layer == 0 ? terrain.stageHeight(this, cellX, cellZ) : terrain.pass(this, layer, cellX, cellZ);
            layerKeys[layer][slot] = key;
            layers[layer][slot] = value;
            return value;
        }

        int raw(FactoryTerrain terrain, int cellX, int cellZ) {
            long key = key(cellX, cellZ);
            int slot = slot(key);
            if (rawKeys[slot] == key) {
                return raws[slot];
            }
            int value = terrain.rawCode(cellX, cellZ);
            rawKeys[slot] = key;
            raws[slot] = value;
            return value;
        }

        int river(FactoryTerrain terrain, int cellX, int cellZ) {
            long key = key(cellX, cellZ);
            int slot = slot(key);
            if (riverKeys[slot] == key) {
                return rivers[slot];
            }
            int value = terrain.riverBed(cellX, cellZ);
            riverKeys[slot] = key;
            rivers[slot] = value;
            return value;
        }
    }
}
