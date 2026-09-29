package de.craftorio.defense.arena;

import de.craftorio.Craftorio;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Geometry of the arena dimension: every team gets a slot along the x axis. Field tile (x, z) of slot s is the block
 * column at (s × {@link #SPACING} + x, z); its surface is at {@link #FLOOR_Y}, path and towers stand on
 * {@link #BUILD_Y}. Gates are in the west wall, the core in the east wall, the stands with the exit in the south.
 */
public final class Arenas {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, Craftorio.id("arena"));
    public static final int SPACING = 128;
    public static final int FLOOR_Y = 64;
    public static final int BUILD_Y = FLOOR_Y + 1;
    /** Plateaus are two blocks high; towers on them stand here. */
    public static final int HIGH_Y = BUILD_Y + 2;
    public static final int WALL_TOP = FLOOR_Y + 6;
    /** Rows of stands south of the field, rising one block per row. */
    public static final int STAND_ROWS = 6;
    /** Everything built for a slot lies within these bounds (relative to the field origin). */
    public static final int MIN_X = -4;
    public static final int MAX_X = ArenaLayout.SIZE + 3;
    public static final int MIN_Z = -4;
    public static final int MAX_Z = ArenaLayout.SIZE + STAND_ROWS + 1;

    private Arenas() {
    }

    public static boolean isArena(Level level) {
        return level.dimension() == DIMENSION;
    }

    public static BlockPos origin(int slot) {
        return new BlockPos(slot * SPACING, FLOOR_Y, 0);
    }

    /** The slot whose arena contains this position, or -1. */
    public static int slotAt(BlockPos pos) {
        int slot = Math.floorDiv(pos.getX() - MIN_X, SPACING);
        int x = pos.getX() - slot * SPACING;
        return slot >= 0 && x >= MIN_X && x <= MAX_X && pos.getZ() >= MIN_Z && pos.getZ() <= MAX_Z ? slot : -1;
    }

    /** Field coordinates {x, z} of a position inside the field of its slot, or null outside the field. */
    public static int[] tileAt(BlockPos pos) {
        int slot = slotAt(pos);
        if (slot < 0) {
            return null;
        }
        int x = pos.getX() - slot * SPACING;
        int z = pos.getZ();
        return x >= 0 && z >= 0 && x < ArenaLayout.SIZE && z < ArenaLayout.SIZE ? new int[]{x, z} : null;
    }

    public static BlockPos field(int slot, int x, int z, int y) {
        return new BlockPos(slot * SPACING + x, y, z);
    }

    public static BlockPos core(int slot) {
        return field(slot, ArenaLayout.SIZE, ArenaLayout.CORE_ROW, BUILD_Y);
    }

    public static BlockPos gate(int slot, int row) {
        return field(slot, -1, row, BUILD_Y);
    }

    /** Top of the stands, where players arrive. */
    public static Vec3 arrival(int slot) {
        BlockPos pos = field(slot, ArenaLayout.SIZE / 2, ArenaLayout.SIZE + STAND_ROWS, FLOOR_Y + STAND_ROWS + 1);
        return Vec3.atBottomCenterOf(pos);
    }

    public static BlockPos exit(int slot) {
        return field(slot, ArenaLayout.SIZE / 2 - 2, ArenaLayout.SIZE + STAND_ROWS, FLOOR_Y + STAND_ROWS + 1);
    }

    public static BlockPos depot(int slot) {
        return field(slot, ArenaLayout.SIZE / 2 + 2, ArenaLayout.SIZE + STAND_ROWS, FLOOR_Y + STAND_ROWS + 1);
    }

    /** The arena console next to exit and depot: opens the terminal's Defense tab. */
    public static BlockPos console(int slot) {
        return field(slot, ArenaLayout.SIZE / 2 + 4, ArenaLayout.SIZE + STAND_ROWS, FLOOR_Y + STAND_ROWS + 1);
    }
}
