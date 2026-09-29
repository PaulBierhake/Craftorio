package de.craftorio.defense.arena;

import de.craftorio.registry.ModBlocks;
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
                buildTile(level, slot, x, z, layout, random);
            }
        }
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

    private static void buildTile(ServerLevel level, int slot, int x, int z, ArenaLayout layout, Random random) {
        ArenaTheme theme = layout.theme();
        Tile tile = layout.tile(x, z);
        BlockPos floor = Arenas.field(slot, x, z, Arenas.FLOOR_Y);
        switch (theme) {
            case FOREST -> {
                switch (tile) {
                    case GROUND -> {
                        set(level, floor, Blocks.GRASS_BLOCK.defaultBlockState());
                        // Only plants that can be replaced (grass, fern): flowers would block paths and towers.
                        int roll = random.nextInt(20);
                        if (roll < 3) {
                            set(level, floor.above(), Blocks.SHORT_GRASS.defaultBlockState());
                        } else if (roll == 3) {
                            set(level, floor.above(), Blocks.FERN.defaultBlockState());
                        }
                    }
                    case ROUGH -> {
                        set(level, floor, Blocks.MOSS_BLOCK.defaultBlockState());
                        if (random.nextInt(3) > 0) {
                            set(level, floor.above(), Blocks.FERN.defaultBlockState());
                        }
                    }
                    default -> {
                        set(level, floor, Blocks.GRASS_BLOCK.defaultBlockState());
                        if (random.nextInt(3) == 0) {
                            leaves(level, floor.above(), Blocks.AZALEA_LEAVES);
                            leaves(level, floor.above(2), Blocks.FLOWERING_AZALEA_LEAVES);
                        } else {
                            int height = 3 + random.nextInt(2);
                            for (int y = 1; y <= height; y++) {
                                set(level, floor.above(y), (random.nextBoolean() ? Blocks.OAK_LOG : Blocks.SPRUCE_LOG).defaultBlockState());
                            }
                            leaves(level, floor.above(height + 1), Blocks.OAK_LEAVES);
                        }
                    }
                }
            }
            case MOUNTAIN -> {
                switch (tile) {
                    case GROUND -> set(level, floor, (random.nextInt(4) == 0 ? Blocks.ANDESITE : Blocks.STONE).defaultBlockState());
                    case ROUGH -> set(level, floor, Blocks.GRAVEL.defaultBlockState());
                    case HIGH -> {
                        set(level, floor, Blocks.STONE.defaultBlockState());
                        set(level, floor.above(), ModBlocks.ARENA_CLIFF.get().defaultBlockState());
                        set(level, floor.above(2), ModBlocks.ARENA_CLIFF.get().defaultBlockState());
                    }
                    case BLOCKED -> {
                        set(level, floor, Blocks.STONE.defaultBlockState());
                        int height = 2 + random.nextInt(3);
                        for (int y = 1; y <= height; y++) {
                            set(level, floor.above(y), (random.nextInt(3) == 0 ? Blocks.COBBLESTONE : Blocks.STONE).defaultBlockState());
                        }
                    }
                }
            }
            case FIRE -> {
                switch (tile) {
                    case GROUND -> set(level, floor, (random.nextInt(3) == 0 ? Blocks.SMOOTH_BASALT : Blocks.BLACKSTONE).defaultBlockState());
                    case ROUGH -> set(level, floor, Blocks.MAGMA_BLOCK.defaultBlockState());
                    default -> {
                        if (random.nextInt(6) == 0) {
                            set(level, floor, Blocks.BASALT.defaultBlockState());
                            for (int y = 1; y <= 2 + random.nextInt(3); y++) {
                                set(level, floor.above(y), Blocks.BASALT.defaultBlockState());
                            }
                        } else {
                            set(level, floor, Blocks.LAVA.defaultBlockState());
                        }
                    }
                }
            }
            case WATER -> {
                switch (tile) {
                    case GROUND -> set(level, floor, (shore(layout, x, z) ? Blocks.SAND : Blocks.GRASS_BLOCK).defaultBlockState());
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
                    for (int y = 1; y <= 4; y++) {
                        set(level, floor.above(y), (y == 4 ? Blocks.CHISELED_SANDSTONE : Blocks.CUT_SANDSTONE).defaultBlockState());
                    }
                } else {
                    set(level, floor, ((x + z) % 2 == 0 ? Blocks.SAND : Blocks.SMOOTH_SANDSTONE).defaultBlockState());
                }
            }
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
