package de.craftorio.defense.arena;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Random;

/**
 * The inner field of an arena for one level: a square of {@link #SIZE}×{@link #SIZE} tiles. Enemies come through
 * one of three gates in the west wall ({@link #SPAWN_ROWS}) and walk to the core in the east wall at row
 * {@link #CORE_ROW}. The player lays the path; the generator only guarantees that a way exists and leaves room for
 * towers. Pure logic: the same seed and level always give the same map.
 */
public final class ArenaLayout {
    public static final int SIZE = 41;
    public static final int CORE_ROW = 20;
    public static final int[] SPAWN_ROWS = {8, 20, 32};
    /** At least this many tiles must allow towers. */
    public static final int MIN_BUILD_TILES = 250;

    private final ArenaTheme theme;
    private final int spawnRow;
    private final Tile[][] tiles;

    private ArenaLayout(ArenaTheme theme, int spawnRow, Tile[][] tiles) {
        this.theme = theme;
        this.spawnRow = spawnRow;
        this.tiles = tiles;
    }

    public ArenaTheme theme() {
        return theme;
    }

    /** Row (z) of the open enemy gate in the west wall; the path starts at x = 0 in this row. */
    public int spawnRow() {
        return spawnRow;
    }

    /** Tile at field coordinates; outside the field everything is blocked. */
    public Tile tile(int x, int z) {
        return x < 0 || z < 0 || x >= SIZE || z >= SIZE ? Tile.BLOCKED : tiles[x][z];
    }

    public int count(Tile tile) {
        int count = 0;
        for (Tile[] column : tiles) {
            for (Tile t : column) {
                if (t == tile) {
                    count++;
                }
            }
        }
        return count;
    }

    public int buildableTiles() {
        return count(Tile.GROUND) + count(Tile.HIGH);
    }

    /** Length of the shortest possible path from the gate to the core, or -1 if there is none. */
    public int shortestPath() {
        return shortestPath(tiles, spawnRow);
    }

    /** Tiles {x, z} of one shortest way from the gate to the core (used by tests and hints). */
    public java.util.List<int[]> route() {
        int[][] previous = new int[SIZE * SIZE][];
        boolean[] seen = new boolean[SIZE * SIZE];
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{0, spawnRow});
        seen[spawnRow] = true;
        int[][] steps = {{1, 0}, {0, 1}, {0, -1}, {-1, 0}};
        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            if (at[0] == SIZE - 1 && at[1] == CORE_ROW) {
                java.util.LinkedList<int[]> route = new java.util.LinkedList<>();
                for (int[] step = at; step != null; step = previous[step[0] * SIZE + step[1]]) {
                    route.addFirst(step);
                }
                return route;
            }
            for (int[] step : steps) {
                int x = at[0] + step[0];
                int z = at[1] + step[1];
                if (x >= 0 && z >= 0 && x < SIZE && z < SIZE && !seen[x * SIZE + z] && tiles[x][z].allowsPath()) {
                    seen[x * SIZE + z] = true;
                    previous[x * SIZE + z] = at;
                    queue.add(new int[]{x, z});
                }
            }
        }
        return java.util.List.of();
    }

    // --- generation

    public static ArenaTheme themeFor(long seed, int level) {
        if (level % 10 == 0) {
            return ArenaTheme.COLOSSEUM;
        }
        // A fixed shuffle of the four themes per arena, cycled: consecutive levels never repeat a theme.
        ArenaTheme[] order = ArenaTheme.ROTATION.clone();
        Random random = new Random(seed * 31 + 7);
        for (int i = order.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            ArenaTheme swap = order[i];
            order[i] = order[j];
            order[j] = swap;
        }
        return order[Math.floorMod(level - 1 - (level - 1) / 10, order.length)];
    }

    public static ArenaLayout generate(long seed, int level) {
        Random random = new Random(seed ^ (level * 0x9E3779B97F4A7C15L));
        ArenaTheme theme = themeFor(seed, level);
        // Maps get busier with the level.
        double density = Math.min(1.0, 0.45 + level * 0.015);
        Tile[][] tiles = new Tile[SIZE][SIZE];
        for (Tile[] column : tiles) {
            Arrays.fill(column, Tile.GROUND);
        }
        switch (theme) {
            case FOREST -> forest(tiles, random, density);
            case MOUNTAIN -> mountain(tiles, random, density);
            case FIRE -> fire(tiles, random, density);
            case WATER -> water(tiles, random, density);
            case COLOSSEUM -> colosseum(tiles);
        }
        int spawnRow = theme == ArenaTheme.COLOSSEUM ? CORE_ROW : SPAWN_ROWS[random.nextInt(SPAWN_ROWS.length)];
        clearAround(tiles, 0, spawnRow);
        clearAround(tiles, SIZE - 1, CORE_ROW);
        if (shortestPath(tiles, spawnRow) < 0) {
            carve(tiles, random, spawnRow);
        }
        // Never so crowded that there is no room for towers.
        while (buildable(tiles) < MIN_BUILD_TILES) {
            int x = random.nextInt(SIZE);
            int z = random.nextInt(SIZE);
            if (tiles[x][z] == Tile.BLOCKED) {
                tiles[x][z] = Tile.GROUND;
            }
        }
        return new ArenaLayout(theme, spawnRow, tiles);
    }

    private static void forest(Tile[][] tiles, Random random, double density) {
        int clusters = (int) (14 + 16 * density);
        for (int i = 0; i < clusters; i++) {
            blob(tiles, random.nextInt(SIZE), random.nextInt(SIZE), 1 + random.nextInt(3), Tile.BLOCKED, null);
        }
        for (int i = 0; i < 7; i++) {
            blob(tiles, random.nextInt(SIZE), random.nextInt(SIZE), 2 + random.nextInt(3), Tile.ROUGH, Tile.GROUND);
        }
    }

    private static void mountain(Tile[][] tiles, Random random, double density) {
        int ridges = 3 + (int) (3 * density);
        for (int i = 0; i < ridges; i++) {
            int x = 4 + random.nextInt(SIZE - 8);
            int z = random.nextInt(SIZE);
            int length = 10 + random.nextInt(16);
            for (int step = 0; step < length; step++) {
                blob(tiles, x, z, random.nextInt(2), Tile.BLOCKED, null);
                x = clamp(x + random.nextInt(3) - 1);
                z = clamp(z + (random.nextInt(4) == 0 ? 0 : random.nextBoolean() ? 1 : -1));
            }
        }
        int plateaus = 4 + random.nextInt(3);
        for (int i = 0; i < plateaus; i++) {
            rect(tiles, random.nextInt(SIZE - 5), random.nextInt(SIZE - 5), 3 + random.nextInt(3), 3 + random.nextInt(3), Tile.HIGH);
        }
        for (int i = 0; i < 4; i++) {
            blob(tiles, random.nextInt(SIZE), random.nextInt(SIZE), 2 + random.nextInt(2), Tile.ROUGH, Tile.GROUND);
        }
    }

    private static void fire(Tile[][] tiles, Random random, double density) {
        int lakes = 4 + (int) (4 * density);
        for (int i = 0; i < lakes; i++) {
            blob(tiles, 3 + random.nextInt(SIZE - 6), random.nextInt(SIZE), 2 + random.nextInt(3), Tile.BLOCKED, null);
        }
        int vents = 6 + random.nextInt(4);
        for (int i = 0; i < vents; i++) {
            blob(tiles, random.nextInt(SIZE), random.nextInt(SIZE), 1, Tile.ROUGH, Tile.GROUND);
        }
    }

    private static void water(Tile[][] tiles, Random random, double density) {
        // A river from north to south; the only crossings are fords of shallow water.
        int x = 14 + random.nextInt(13);
        int width = 3 + (density > 0.7 ? 1 : 0);
        int fords = 2 + random.nextInt(2);
        int[] fordRows = new int[fords];
        for (int i = 0; i < fords; i++) {
            fordRows[i] = 4 + random.nextInt(SIZE - 8);
        }
        for (int z = 0; z < SIZE; z++) {
            boolean ford = false;
            for (int row : fordRows) {
                ford |= Math.abs(row - z) <= 1;
            }
            for (int dx = 0; dx < width; dx++) {
                tiles[clamp(x + dx)][z] = ford ? Tile.ROUGH : Tile.BLOCKED;
            }
            if (random.nextInt(3) == 0) {
                x = Math.max(6, Math.min(SIZE - 6 - width, x + (random.nextBoolean() ? 1 : -1)));
            }
        }
        int lakes = 1 + (int) (2 * density);
        for (int i = 0; i < lakes; i++) {
            int cx = random.nextInt(SIZE);
            int cz = random.nextInt(SIZE);
            int r = 2 + random.nextInt(2);
            blob(tiles, cx, cz, r + 1, Tile.ROUGH, Tile.GROUND);
            blob(tiles, cx, cz, r, Tile.BLOCKED, null);
        }
    }

    private static void colosseum(Tile[][] tiles) {
        for (int x = 6; x < SIZE - 4; x += 8) {
            for (int z = 4; z < SIZE - 3; z += 8) {
                rect(tiles, x, z, 2, 2, Tile.BLOCKED);
            }
        }
    }

    /** Opens a winding corridor from the gate to the core. */
    private static void carve(Tile[][] tiles, Random random, int spawnRow) {
        int x = 0;
        int z = spawnRow;
        while (x < SIZE - 1 || z != CORE_ROW) {
            if (!tiles[x][z].allowsPath()) {
                tiles[x][z] = Tile.GROUND;
            }
            boolean moveX = z == CORE_ROW || (x < SIZE - 1 && random.nextInt(3) != 0);
            if (moveX && x < SIZE - 1) {
                x++;
            } else {
                z += Integer.signum(CORE_ROW - z);
            }
        }
        tiles[x][z] = Tile.GROUND;
    }

    private static void clearAround(Tile[][] tiles, int cx, int cz) {
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                if (x >= 0 && z >= 0 && x < SIZE && z < SIZE) {
                    tiles[x][z] = Tile.GROUND;
                }
            }
        }
    }

    private static void blob(Tile[][] tiles, int cx, int cz, int radius, Tile tile, Tile onlyOver) {
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                if (x >= 0 && z >= 0 && x < SIZE && z < SIZE && (x - cx) * (x - cx) + (z - cz) * (z - cz) <= radius * radius + 1
                        && (onlyOver == null || tiles[x][z] == onlyOver)) {
                    tiles[x][z] = tile;
                }
            }
        }
    }

    private static void rect(Tile[][] tiles, int x0, int z0, int width, int depth, Tile tile) {
        for (int x = x0; x < x0 + width && x < SIZE; x++) {
            for (int z = z0; z < z0 + depth && z < SIZE; z++) {
                tiles[x][z] = tile;
            }
        }
    }

    private static int buildable(Tile[][] tiles) {
        int count = 0;
        for (Tile[] column : tiles) {
            for (Tile t : column) {
                if (t.allowsTower()) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int shortestPath(Tile[][] tiles, int spawnRow) {
        int[][] distance = new int[SIZE][SIZE];
        for (int[] row : distance) {
            Arrays.fill(row, -1);
        }
        Deque<int[]> queue = new ArrayDeque<>();
        if (!tiles[0][spawnRow].allowsPath()) {
            return -1;
        }
        distance[0][spawnRow] = 1;
        queue.add(new int[]{0, spawnRow});
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            if (at[0] == SIZE - 1 && at[1] == CORE_ROW) {
                return distance[at[0]][at[1]];
            }
            for (int[] step : steps) {
                int x = at[0] + step[0];
                int z = at[1] + step[1];
                if (x >= 0 && z >= 0 && x < SIZE && z < SIZE && distance[x][z] < 0 && tiles[x][z].allowsPath()) {
                    distance[x][z] = distance[at[0]][at[1]] + 1;
                    queue.add(new int[]{x, z});
                }
            }
        }
        return -1;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(SIZE - 1, value));
    }
}
