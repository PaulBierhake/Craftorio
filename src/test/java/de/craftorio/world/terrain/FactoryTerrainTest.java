package de.craftorio.world.terrain;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactoryTerrainTest {
    private static final int SIZE = 2048;
    private static final int HALF = SIZE / 2;
    private static final Set<Integer> HEIGHTS = Set.of(59, 60, 61, 64, 68, 72);

    private static int[][] sample(FactoryTerrain terrain) {
        int[][] heights = new int[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                heights[x][z] = terrain.height(x - HALF, z - HALF);
            }
        }
        return heights;
    }

    private static final long[] SEEDS = {12345, 7, -99, 20240229};

    @Test
    void mostOfTheWorldIsTheGroundLevelAndOnlyTheFixedHeightsOccur() {
        for (long seed : SEEDS) {
            int[][] heights = sample(new FactoryTerrain(seed));
            TreeMap<Integer, Integer> counts = new TreeMap<>();
            for (int[] row : heights) {
                for (int h : row) {
                    counts.merge(h, 1, Integer::sum);
                }
            }
            assertTrue(HEIGHTS.containsAll(counts.keySet()), "heights " + counts.keySet() + " (seed " + seed + ")");
            double ground = counts.getOrDefault(64, 0) / (double) (SIZE * SIZE);
            assertTrue(ground >= 0.75, "ground level " + ground + " (seed " + seed + ")");
            double plateau = (counts.getOrDefault(68, 0) + counts.getOrDefault(72, 0)) / (double) (SIZE * SIZE);
            assertTrue(plateau > 0.08 && plateau < 0.2, "plateaus " + plateau + " (seed " + seed + ")");
            double water = (counts.getOrDefault(59, 0) + counts.getOrDefault(60, 0) + counts.getOrDefault(61, 0)) / (double) (SIZE * SIZE);
            assertTrue(water > 0.02 && water < 0.10, "lakes and rivers " + water + " (seed " + seed + ")");
            assertTrue(counts.getOrDefault(72, 0) > 0 && counts.getOrDefault(72, 0) < counts.getOrDefault(68, 0), "plateau two is rarer than one");
        }
    }

    @Test
    void theSpawnAreaIsGroundLevelOnly() {
        for (long seed : SEEDS) {
            FactoryTerrain terrain = new FactoryTerrain(seed);
            for (int x = -192; x <= 192; x++) {
                for (int z = -192; z <= 192; z++) {
                    if (x * x + z * z < 192 * 192) {
                        assertEquals(64, terrain.height(x, z), "(" + x + ", " + z + ") seed " + seed);
                    }
                }
            }
            // between 192 and 256 blocks: no lakes or rivers
            for (int x = -256; x <= 256; x += 2) {
                for (int z = -256; z <= 256; z += 2) {
                    if (x * x + z * z < 256 * 256) {
                        assertTrue(terrain.height(x, z) >= 64, "no water within 256 blocks");
                    }
                }
            }
        }
    }

    @Test
    void noLevelAreaIsSmallerThanSixBySix() {
        for (long seed : SEEDS) {
            int[][] heights = sample(new FactoryTerrain(seed));
            boolean[][] seen = new boolean[SIZE][SIZE];
            int tooSmall = 0;
            int areas = 0;
            for (int x0 = 0; x0 < SIZE; x0++) {
                for (int z0 = 0; z0 < SIZE; z0++) {
                    if (seen[x0][z0]) {
                        continue;
                    }
                    int level = heights[x0][z0];
                    ArrayDeque<int[]> queue = new ArrayDeque<>();
                    queue.add(new int[]{x0, z0});
                    seen[x0][z0] = true;
                    int minX = x0, maxX = x0, minZ = z0, maxZ = z0, size = 0;
                    while (!queue.isEmpty()) {
                        int[] at = queue.poll();
                        size++;
                        minX = Math.min(minX, at[0]);
                        maxX = Math.max(maxX, at[0]);
                        minZ = Math.min(minZ, at[1]);
                        maxZ = Math.max(maxZ, at[1]);
                        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                            int nx = at[0] + d[0];
                            int nz = at[1] + d[1];
                            if (nx >= 0 && nz >= 0 && nx < SIZE && nz < SIZE && !seen[nx][nz] && heights[nx][nz] == level) {
                                seen[nx][nz] = true;
                                queue.add(new int[]{nx, nz});
                            }
                        }
                    }
                    if (level < 64) {
                        continue; // lake and river beds: rivers are thin on purpose
                    }
                    boolean touchesBorder = minX == 0 || minZ == 0 || maxX == SIZE - 1 || maxZ == SIZE - 1;
                    if (touchesBorder) {
                        continue;
                    }
                    areas++;
                    if (maxX - minX + 1 < 6 || maxZ - minZ + 1 < 6) {
                        tooSmall++;
                        System.out.println("SMALL at " + (minX - HALF) + "," + (minZ - HALF) + " around " + heights[Math.max(0, minX - 2)][minZ] + "/" + heights[Math.min(SIZE - 1, maxX + 2)][minZ] + "/" + heights[minX][Math.max(0, minZ - 2)] + "/" + heights[minX][Math.min(SIZE - 1, maxZ + 2)] + " seed " + seed + " level " + level + " size " + size + " box " + (maxX - minX + 1) + "x" + (maxZ - minZ + 1));
                    }
                }
            }
            assertEquals(0, tooSmall, "areas smaller than 6 x 6 of " + areas + " (seed " + seed + ")");
        }
    }

    @Test
    void aColumnCostsLessThanFiftyNanoseconds() {
        FactoryTerrain terrain = new FactoryTerrain(4242);
        long sum = 0;
        for (int round = 0; round < 3; round++) {
            for (int x = 0; x < 1024; x++) {
                for (int z = 0; z < 1024; z++) {
                    sum += terrain.height(x, z);
                }
            }
        }
        double perColumn = Double.MAX_VALUE;
        for (int attempt = 0; attempt < 3; attempt++) {
            int offset = 1024 + attempt * 2048;
            long start = System.nanoTime();
            for (int x = offset; x < offset + 2048; x++) {
                for (int z = 0; z < 1024; z++) {
                    sum += terrain.height(x, z);
                }
            }
            perColumn = Math.min(perColumn, (System.nanoTime() - start) / (2048.0 * 1024.0));
        }
        System.out.println("TERRAIN ns per column " + perColumn + " (" + sum + ")");
        assertTrue(perColumn < 50, perColumn + " ns per column");
    }

    /** Regression: a cache per terrain instance (1.3 MB each) filled the heap, because the density function is copied for every chunk. */
    @Test
    void manyTerrainsOfOneSeedShareOneCachePerThread() throws InterruptedException {
        int[] created = new int[1];
        Thread thread = new Thread(() -> {
            int before = FactoryTerrain.cachesCreated();
            long sum = 0;
            for (int i = 0; i < 300; i++) {
                sum += new FactoryTerrain(4242L).height(i * 16, i * 7);
            }
            created[0] = FactoryTerrain.cachesCreated() - before;
            assertTrue(sum > 0);
        });
        thread.start();
        thread.join();
        assertEquals(1, created[0], "one cache for the whole thread");
    }

    @Test
    void terrainsWithDifferentSettingsNeverShareWrongEntries() {
        FactoryTerrain a = new FactoryTerrain(1L);
        FactoryTerrain b = new FactoryTerrain(2L);
        int[] expectedA = new int[200];
        int[] expectedB = new int[200];
        for (int i = 0; i < 200; i++) {
            expectedA[i] = a.height(i * 40, i * 13);
        }
        for (int i = 0; i < 200; i++) {
            expectedB[i] = b.height(i * 40, i * 13);
        }
        for (int i = 0; i < 200; i++) {
            assertEquals(expectedA[i], a.height(i * 40, i * 13));
            assertEquals(expectedB[i], b.height(i * 40, i * 13));
        }
    }
}
