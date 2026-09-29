package de.craftorio.defense.arena;

import de.craftorio.registry.ModBlocks;
import de.craftorio.world.cave.Noise2D;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Random;

/** Builds the fixed frame of an arena (base, walls, gates, core, stands) and the themed field of a level. */
public final class ArenaBuilder {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private ArenaBuilder() {
    }

    /** Base plate, walls with the three enemy gates, the core and the stands with exit gate and tower depot. */
    public static void buildFrame(ServerLevel level, int slot) {
        BlockState base = ModBlocks.ARENA_BASE.get().defaultBlockState();
        BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState trim = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        BlockState stand = Blocks.STONE_BRICKS.defaultBlockState();
        int size = ArenaLayout.SIZE;
        for (int x = Arenas.MIN_X; x <= Arenas.MAX_X; x++) {
            for (int z = Arenas.MIN_Z; z <= Arenas.MAX_Z; z++) {
                set(level, Arenas.field(slot, x, z, Arenas.FLOOR_Y - 1), base);
                boolean inField = x >= 0 && z >= 0 && x < size && z < size;
                boolean ring = !inField && x >= -1 && x <= size && z >= -1 && z <= size;
                for (int y = Arenas.WALL_TOP + 2; y >= Arenas.FLOOR_Y; y--) {
                    if (!inField) {
                        set(level, Arenas.field(slot, x, z, y), Blocks.AIR.defaultBlockState());
                    }
                }
                if (ring && z < size) {
                    // West, east and north walls; lanterns on top every few blocks.
                    for (int y = Arenas.FLOOR_Y; y <= Arenas.WALL_TOP; y++) {
                        set(level, Arenas.field(slot, x, z, y), y == Arenas.WALL_TOP ? trim : wall);
                    }
                    if (Math.floorMod(x + z, 5) == 0) {
                        set(level, Arenas.field(slot, x, z, Arenas.WALL_TOP + 1), Blocks.LANTERN.defaultBlockState());
                    }
                } else if (z >= size && z <= size + Arenas.STAND_ROWS && x >= -1 && x <= size) {
                    // South: a low wall, then stands rising row by row.
                    int top = Arenas.FLOOR_Y + (z - size);
                    for (int y = Arenas.FLOOR_Y; y <= Math.max(Arenas.FLOOR_Y + 1, top); y++) {
                        set(level, Arenas.field(slot, x, z, y), z == size ? wall : stand);
                    }
                }
            }
        }
        for (int row : ArenaLayout.SPAWN_ROWS) {
            closeGate(level, slot, row);
        }
        set(level, Arenas.core(slot), ModBlocks.ZONE_CORE.get().defaultBlockState());
        set(level, Arenas.core(slot).above(), Blocks.AIR.defaultBlockState());
        set(level, Arenas.core(slot).above(2), Blocks.AIR.defaultBlockState());
        set(level, Arenas.exit(slot), ModBlocks.ARENA_EXIT.get().defaultBlockState());
        set(level, Arenas.depot(slot), ModBlocks.TOWER_DEPOT.get().defaultBlockState());
        set(level, Arenas.console(slot), ModBlocks.ARENA_CONSOLE.get().defaultBlockState());
    }

    /** Arenas built before the console existed get it the next time their team enters. */
    public static void addConsole(ServerLevel level, int slot) {
        set(level, Arenas.console(slot), ModBlocks.ARENA_CONSOLE.get().defaultBlockState());
    }

    public static void openGate(ServerLevel level, int slot, int row) {
        BlockPos gate = Arenas.gate(slot, row);
        set(level, gate, ModBlocks.ENEMY_PORTAL.get().defaultBlockState());
        set(level, gate.above(), Blocks.AIR.defaultBlockState());
    }

    public static void closeGate(ServerLevel level, int slot, int row) {
        BlockPos gate = Arenas.gate(slot, row);
        set(level, gate, Blocks.IRON_BARS.defaultBlockState());
        set(level, gate.above(), Blocks.IRON_BARS.defaultBlockState());
    }

    /** Rebuilds the field for a new map: clears everything above the base and lays the themed terrain. */
    public static void buildField(ServerLevel level, int slot, ArenaLayout layout, long seed) {
        Random random = new Random(seed);
        Noise2D noise = new Noise2D(seed);
        for (int row : ArenaLayout.SPAWN_ROWS) {
            if (row == layout.spawnRow()) {
                openGate(level, slot, row);
            } else {
                closeGate(level, slot, row);
            }
        }
        for (int x = 0; x < ArenaLayout.SIZE; x++) {
            for (int z = 0; z < ArenaLayout.SIZE; z++) {
                // Top to bottom, so plants never lose their support first and drop as items.
                for (int y = Arenas.WALL_TOP + 2; y >= Arenas.FLOOR_Y; y--) {
                    set(level, Arenas.field(slot, x, z, y), Blocks.AIR.defaultBlockState());
                }
                set(level, Arenas.field(slot, x, z, Arenas.FLOOR_Y - 1), ModBlocks.ARENA_BASE.get().defaultBlockState());
                buildTile(level, slot, x, z, layout, random, noise);
            }
        }
        placeStructures(level, slot, layout, random);
        hangBanners(level, slot, layout.theme());
        removeItems(level, slot);
    }

    /** Safety net: whatever dropped while the map was rebuilt disappears. */
    private static void removeItems(ServerLevel level, int slot) {
        AABB box = new AABB(Arenas.field(slot, Arenas.MIN_X, Arenas.MIN_Z, Arenas.FLOOR_Y - 1).getCenter(),
                Arenas.field(slot, Arenas.MAX_X, Arenas.MAX_Z, Arenas.WALL_TOP + 3).getCenter()).inflate(1);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
            item.discard();
        }
    }

    /** A pick from a palette by noise value in [-1, 1]. */
    private static Block pick(double n, Block... palette) {
        int index = (int) Math.min(palette.length - 1, Math.floor((n + 1) / 2 * palette.length));
        return palette[Math.max(0, index)];
    }

    private static void buildTile(ServerLevel level, int slot, int x, int z, ArenaLayout layout, Random random, Noise2D noise) {
        ArenaTheme theme = layout.theme();
        Tile tile = layout.tile(x, z);
        BlockPos floor = Arenas.field(slot, x, z, Arenas.FLOOR_Y);
        double patch = noise.noise(x * 0.16, z * 0.16);
        double detail = noise.noise(x * 0.5 + 40, z * 0.5 + 40);
        switch (theme) {
            case FOREST -> {
                switch (tile) {
                    case GROUND -> {
                        set(level, floor, (patch > 0.4 ? Blocks.PODZOL : patch < -0.45 ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK).defaultBlockState());
                        // Only plants that can be replaced (grass, fern): flowers would block paths and towers.
                        int roll = random.nextInt(20);
                        if (roll < 3 && patch <= 0.4 && patch >= -0.45) {
                            set(level, floor.above(), Blocks.SHORT_GRASS.defaultBlockState());
                        } else if (roll == 3 && patch <= 0.4) {
                            set(level, floor.above(), Blocks.FERN.defaultBlockState());
                        }
                    }
                    case ROUGH -> {
                        set(level, floor, (detail > 0.3 ? Blocks.PODZOL : Blocks.MOSS_BLOCK).defaultBlockState());
                        if (random.nextInt(3) > 0) {
                            set(level, floor.above(), Blocks.FERN.defaultBlockState());
                        }
                    }
                    default -> tree(level, floor, patch, random);
                }
            }
            case MOUNTAIN -> {
                switch (tile) {
                    case GROUND -> set(level, floor, pick(patch, Blocks.STONE, Blocks.STONE, Blocks.ANDESITE, Blocks.TUFF, Blocks.DIORITE).defaultBlockState());
                    case ROUGH -> set(level, floor, (detail > 0.35 ? Blocks.COARSE_DIRT : Blocks.GRAVEL).defaultBlockState());
                    case HIGH -> {
                        set(level, floor, (detail > 0.3 ? Blocks.ANDESITE : Blocks.STONE).defaultBlockState());
                        set(level, floor.above(), ModBlocks.ARENA_CLIFF.get().defaultBlockState());
                        set(level, floor.above(2), ModBlocks.ARENA_CLIFF.get().defaultBlockState());
                    }
                    case BLOCKED -> {
                        set(level, floor, Blocks.STONE.defaultBlockState());
                        int height = 2 + random.nextInt(4);
                        for (int y = 1; y <= height; y++) {
                            set(level, floor.above(y), pick(noise.noise(x * 0.6 + y, z * 0.6), Blocks.STONE, Blocks.COBBLESTONE, Blocks.ANDESITE,
                                    Blocks.MOSSY_COBBLESTONE).defaultBlockState());
                        }
                    }
                }
            }
            case FIRE -> {
                switch (tile) {
                    case GROUND -> set(level, floor, pick(patch, Blocks.BLACKSTONE, Blocks.BLACKSTONE, Blocks.SMOOTH_BASALT, Blocks.BASALT, Blocks.SOUL_SOIL).defaultBlockState());
                    case ROUGH -> set(level, floor, Blocks.MAGMA_BLOCK.defaultBlockState());
                    default -> {
                        if (random.nextInt(6) == 0) {
                            set(level, floor, Blocks.BASALT.defaultBlockState());
                            for (int y = 1; y <= 2 + random.nextInt(4); y++) {
                                set(level, floor.above(y), (random.nextInt(4) == 0 ? Blocks.BLACKSTONE : Blocks.BASALT).defaultBlockState());
                            }
                        } else {
                            set(level, floor, Blocks.LAVA.defaultBlockState());
                        }
                    }
                }
            }
            case WATER -> {
                switch (tile) {
                    case GROUND -> set(level, floor, (shore(layout, x, z) ? (detail > 0.35 ? Blocks.GRAVEL : detail < -0.35 ? Blocks.CLAY : Blocks.SAND)
                            : patch > 0.5 ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK).defaultBlockState());
                    // Fords are stepping stones, clearly different from the open water (deep tiles) around them.
                    case ROUGH -> set(level, floor, ((x + z) % 2 == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.MOSSY_STONE_BRICKS).defaultBlockState());
                    default -> {
                        set(level, floor.below(), Blocks.DARK_PRISMARINE.defaultBlockState());
                        set(level, floor, Blocks.WATER.defaultBlockState());
                        if (random.nextInt(8) == 0) {
                            set(level, floor.above(), Blocks.LILY_PAD.defaultBlockState());
                        }
                    }
                }
            }
            case COLOSSEUM -> {
                if (tile == Tile.BLOCKED) {
                    set(level, floor, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
                    int height = 2 + random.nextInt(3);
                    for (int y = 1; y <= height; y++) {
                        set(level, floor.above(y), (y == height ? Blocks.CHISELED_SANDSTONE : Blocks.CUT_SANDSTONE).defaultBlockState());
                    }
                } else {
                    set(level, floor, ((x + z) % 2 == 0 ? (detail > 0.5 ? Blocks.RED_SAND : Blocks.SAND)
                            : (detail < -0.5 ? Blocks.CUT_SANDSTONE : Blocks.SMOOTH_SANDSTONE)).defaultBlockState());
                }
            }
        }
    }

    /** Oak, birch, tall spruce, bush or a group of mushrooms; leaves stay in the tile's own column. */
    private static void tree(ServerLevel level, BlockPos floor, double patch, Random random) {
        set(level, floor, (patch > 0.4 ? Blocks.PODZOL : Blocks.GRASS_BLOCK).defaultBlockState());
        int kind = random.nextInt(8);
        if (kind < 2) {
            leaves(level, floor.above(), Blocks.AZALEA_LEAVES);
            leaves(level, floor.above(2), random.nextBoolean() ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES);
        } else if (kind == 2) {
            set(level, floor, Blocks.PODZOL.defaultBlockState());
            set(level, floor.above(), (random.nextBoolean() ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState());
            set(level, floor.above(2), Blocks.AIR.defaultBlockState());
        } else if (patch > 0.25) {
            int height = 4 + random.nextInt(2);
            for (int y = 1; y <= height; y++) {
                set(level, floor.above(y), Blocks.BIRCH_LOG.defaultBlockState());
            }
            leaves(level, floor.above(height + 1), Blocks.BIRCH_LEAVES);
        } else if (patch < -0.25) {
            int height = 5 + random.nextInt(3);
            for (int y = 1; y <= height; y++) {
                set(level, floor.above(y), Blocks.SPRUCE_LOG.defaultBlockState());
            }
            leaves(level, floor.above(height + 1), Blocks.SPRUCE_LEAVES);
            leaves(level, floor.above(height + 2), Blocks.SPRUCE_LEAVES);
        } else {
            int height = 3 + random.nextInt(2);
            for (int y = 1; y <= height; y++) {
                set(level, floor.above(y), (random.nextInt(4) == 0 ? Blocks.DARK_OAK_LOG : Blocks.OAK_LOG).defaultBlockState());
            }
            leaves(level, floor.above(height + 1), Blocks.OAK_LEAVES);
        }
    }

    // --- structures on blocked ground

    /** Small buildings on 3×3 areas that are blocked anyway: huts, wells and ruins, arches, forges, piers, ruined columns. */
    private static void placeStructures(ServerLevel level, int slot, ArenaLayout layout, Random random) {
        boolean[][] used = new boolean[ArenaLayout.SIZE][ArenaLayout.SIZE];
        for (int x = 1; x < ArenaLayout.SIZE - 1; x++) {
            for (int z = 1; z < ArenaLayout.SIZE - 1; z++) {
                if (!footprintFree(layout, used, x, z) || random.nextInt(9) != 0) {
                    continue;
                }
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        int ux = x + dx;
                        int uz = z + dz;
                        if (ux >= 0 && uz >= 0 && ux < ArenaLayout.SIZE && uz < ArenaLayout.SIZE) {
                            used[ux][uz] = true;
                        }
                    }
                }
                BlockPos center = Arenas.field(slot, x, z, Arenas.FLOOR_Y);
                switch (layout.theme()) {
                    case FOREST -> {
                        switch (random.nextInt(3)) {
                            case 0 -> hut(level, center, random);
                            case 1 -> well(level, center);
                            default -> ruin(level, center, random, Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE);
                        }
                    }
                    case MOUNTAIN -> {
                        if (random.nextBoolean()) {
                            arch(level, center);
                        } else {
                            ruin(level, center, random, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS);
                        }
                    }
                    case FIRE -> forge(level, center);
                    case WATER -> {
                        if (random.nextBoolean()) {
                            pier(level, center, random.nextBoolean());
                        } else {
                            ruin(level, center, random, Blocks.MOSSY_STONE_BRICKS, Blocks.STONE_BRICKS);
                        }
                    }
                    case COLOSSEUM -> columns(level, center, random);
                }
            }
        }
    }

    private static boolean footprintFree(ArenaLayout layout, boolean[][] used, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (layout.tile(x + dx, z + dz) != Tile.BLOCKED || used[x + dx][z + dz]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void clearAbove(ServerLevel level, BlockPos center, int height) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = 1; y <= height; y++) {
                    set(level, center.offset(dx, y, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void hut(ServerLevel level, BlockPos center, Random random) {
        clearAbove(level, center, 6);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(level, center.offset(dx, 0, dz), Blocks.SPRUCE_PLANKS.defaultBlockState());
                boolean ring = dx != 0 || dz != 0;
                for (int y = 1; y <= 3; y++) {
                    boolean door = dx == 0 && dz == 1 && y <= 2;
                    if (ring && !door) {
                        set(level, center.offset(dx, y, dz), (y == 2 && (dx == 0 || dz == 0) && !(dz == 1) ? Blocks.GLASS_PANE : Blocks.OAK_PLANKS).defaultBlockState());
                    }
                }
                set(level, center.offset(dx, 4, dz), Blocks.SPRUCE_SLAB.defaultBlockState());
            }
        }
        set(level, center.above(3), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, false));
    }

    private static void well(ServerLevel level, BlockPos center) {
        clearAbove(level, center, 4);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean middle = dx == 0 && dz == 0;
                set(level, center.offset(dx, 0, dz), (middle ? Blocks.WATER : Blocks.COBBLESTONE).defaultBlockState());
                if (!middle) {
                    set(level, center.offset(dx, 1, dz), Blocks.COBBLESTONE_WALL.defaultBlockState());
                }
            }
        }
        for (int[] post : new int[][]{{-1, -1}, {1, 1}}) {
            set(level, center.offset(post[0], 2, post[1]), Blocks.OAK_FENCE.defaultBlockState());
            set(level, center.offset(post[0], 3, post[1]), Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int dx = -1; dx <= 1; dx++) {
            set(level, center.offset(dx, 4, dx), Blocks.OAK_SLAB.defaultBlockState());
        }
    }

    private static void ruin(ServerLevel level, BlockPos center, Random random, Block main, Block worn) {
        clearAbove(level, center, 4);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                int height = random.nextInt(4);
                for (int y = 1; y <= height; y++) {
                    set(level, center.offset(dx, y, dz), (random.nextInt(3) == 0 ? worn : main).defaultBlockState());
                }
            }
        }
    }

    private static void arch(ServerLevel level, BlockPos center) {
        clearAbove(level, center, 5);
        for (int side : new int[]{-1, 1}) {
            for (int y = 1; y <= 3; y++) {
                set(level, center.offset(side, y, 0), (y == 3 ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState());
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            set(level, center.offset(dx, 4, 0), Blocks.STONE_BRICK_SLAB.defaultBlockState());
        }
    }

    private static void forge(ServerLevel level, BlockPos center) {
        clearAbove(level, center, 4);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean middle = dx == 0 && dz == 0;
                set(level, center.offset(dx, 0, dz), (middle ? Blocks.LAVA : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState());
                if (!middle) {
                    for (int y = 1; y <= 2; y++) {
                        set(level, center.offset(dx, y, dz), (y == 2 ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState());
                    }
                }
            }
        }
        set(level, center.offset(0, 3, 0), Blocks.CHAIN.defaultBlockState());
    }

    private static void pier(ServerLevel level, BlockPos center, boolean alongX) {
        clearAbove(level, center, 3);
        for (int i = -1; i <= 1; i++) {
            BlockPos plank = alongX ? center.offset(i, 1, 0) : center.offset(0, 1, i);
            set(level, plank, Blocks.OAK_SLAB.defaultBlockState());
        }
        set(level, alongX ? center.offset(-1, 2, 0) : center.offset(0, 2, -1), Blocks.OAK_FENCE.defaultBlockState());
        set(level, alongX ? center.offset(1, 2, 0) : center.offset(0, 2, 1), Blocks.OAK_FENCE.defaultBlockState());
        set(level, alongX ? center.offset(1, 3, 0) : center.offset(0, 3, 1), Blocks.LANTERN.defaultBlockState());
    }

    private static void columns(ServerLevel level, BlockPos center, Random random) {
        clearAbove(level, center, 6);
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                int height = 1 + random.nextInt(4);
                for (int y = 1; y <= height; y++) {
                    set(level, center.offset(dx, y, dz), (y == height && height < 4 ? Blocks.CUT_SANDSTONE_SLAB : Blocks.CUT_SANDSTONE).defaultBlockState());
                }
            }
        }
    }

    /** Banners in the theme's colour on the inside of the west, north and east walls. */
    private static void hangBanners(ServerLevel level, int slot, ArenaTheme theme) {
        Block banner = switch (theme) {
            case FOREST -> Blocks.GREEN_WALL_BANNER;
            case MOUNTAIN -> Blocks.GRAY_WALL_BANNER;
            case FIRE -> Blocks.RED_WALL_BANNER;
            case WATER -> Blocks.BLUE_WALL_BANNER;
            case COLOSSEUM -> Blocks.YELLOW_WALL_BANNER;
        };
        int y = Arenas.WALL_TOP - 1;
        for (int i = 2; i < ArenaLayout.SIZE - 1; i += 5) {
            set(level, Arenas.field(slot, 0, i, y), banner.defaultBlockState().setValue(WallBannerBlock.FACING, Direction.EAST));
            set(level, Arenas.field(slot, ArenaLayout.SIZE - 1, i, y), banner.defaultBlockState().setValue(WallBannerBlock.FACING, Direction.WEST));
            set(level, Arenas.field(slot, i, 0, y), banner.defaultBlockState().setValue(WallBannerBlock.FACING, Direction.SOUTH));
        }
    }

    /** Next to water (shallow or deep). */
    private static boolean shore(ArenaLayout layout, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Tile near = layout.tile(x + dx, z + dz);
                if ((dx != 0 || dz != 0) && (near == Tile.ROUGH || near == Tile.BLOCKED) && x + dx >= 0 && z + dz >= 0
                        && x + dx < ArenaLayout.SIZE && z + dz < ArenaLayout.SIZE) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void leaves(ServerLevel level, BlockPos pos, Block block) {
        set(level, pos, block.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos) != state) {
            level.setBlock(pos, state, FLAGS);
        }
    }
}
