package de.craftorio.world.tool;

import de.craftorio.world.terrain.FactoryTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** The terrain tools: filling water with land (landfill) and lowering a plateau by one step (cliff explosives). */
public final class WorldTools {
    /** Landfill fills a 3 × 3 area. */
    public static final int LANDFILL_RADIUS = 1;
    /** Cliff explosives lower a 5 × 5 area. */
    public static final int CLIFF_RADIUS = 2;

    private WorldTools() {
    }

    /**
     * Fills the water column around {@code hit} (the topmost water block of the clicked column) in a 3 × 3 area up to the
     * water surface; the top block is grass (sand in the desert). Returns how many columns were filled.
     */
    public static int landfill(Level level, BlockPos hit) {
        int surface = hit.getY();
        while (level.getFluidState(new BlockPos(hit.getX(), surface + 1, hit.getZ())).is(net.minecraft.tags.FluidTags.WATER)) {
            surface++;
        }
        int filled = 0;
        for (int dx = -LANDFILL_RADIUS; dx <= LANDFILL_RADIUS; dx++) {
            for (int dz = -LANDFILL_RADIUS; dz <= LANDFILL_RADIUS; dz++) {
                int x = hit.getX() + dx;
                int z = hit.getZ() + dz;
                if (!isWater(level, new BlockPos(x, surface, z))) {
                    continue;
                }
                int bottom = surface;
                while (isWater(level, new BlockPos(x, bottom - 1, z))) {
                    bottom--;
                }
                BlockState ground = groundBlock(level, new BlockPos(x, surface, z));
                for (int y = bottom; y <= surface; y++) {
                    level.setBlock(new BlockPos(x, y, z), y == surface ? ground : Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                }
                filled++;
            }
        }
        return filled;
    }

    /** True if the clicked block is water or a water-logged block of the water surface. */
    public static boolean isWater(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER) && level.getBlockState(pos).getBlock() == Blocks.WATER;
    }

    /** The step below a plateau height, or -1 if the height is not a plateau. */
    public static int stepBelow(int top) {
        if (top == FactoryTerrain.PLATEAU_TWO) {
            return FactoryTerrain.PLATEAU_ONE;
        }
        if (top == FactoryTerrain.PLATEAU_ONE) {
            return FactoryTerrain.GROUND;
        }
        return -1;
    }

    /**
     * Lowers the 5 × 5 area around {@code pos} to the step below the plateau; the debris drops as cobblestone. Returns the
     * number of columns lowered, 0 if there is no plateau (or no cliff) here.
     */
    public static int blastCliff(Level level, BlockPos pos) {
        int top = columnTop(level, pos.getX(), pos.getZ());
        int target = stepBelow(top);
        if (target < 0 || !hasEdge(level, pos, top)) {
            return 0;
        }
        int lowered = 0;
        int debris = 0;
        for (int dx = -CLIFF_RADIUS; dx <= CLIFF_RADIUS; dx++) {
            for (int dz = -CLIFF_RADIUS; dz <= CLIFF_RADIUS; dz++) {
                int x = pos.getX() + dx;
                int z = pos.getZ() + dz;
                int columnTop = columnTop(level, x, z);
                if (columnTop <= target) {
                    continue;
                }
                int ceiling = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                for (int y = target + 1; y < ceiling; y++) {
                    BlockPos block = new BlockPos(x, y, z);
                    if (y <= columnTop && !level.getBlockState(block).isAir()) {
                        debris++;
                    }
                    level.setBlock(block, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                BlockPos surface = new BlockPos(x, target, z);
                if (!level.getBlockState(surface).is(BlockTags.DIRT)) {
                    level.setBlock(surface, groundBlock(level, surface), Block.UPDATE_ALL);
                }
                lowered++;
            }
        }
        if (lowered > 0) {
            drop(level, pos, debris / 4);
        }
        return lowered;
    }

    private static void drop(Level level, BlockPos pos, int stones) {
        while (stones > 0) {
            int count = Math.min(stones, 64);
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, new ItemStack(Items.COBBLESTONE, count)));
            stones -= count;
        }
    }

    /** True if the plateau around {@code pos} ends within the 5 × 5 area: some column there is lower than the plateau. */
    private static boolean hasEdge(Level level, BlockPos pos, int top) {
        for (int dx = -CLIFF_RADIUS; dx <= CLIFF_RADIUS; dx++) {
            for (int dz = -CLIFF_RADIUS; dz <= CLIFF_RADIUS; dz++) {
                if (columnTop(level, pos.getX() + dx, pos.getZ() + dz) < top) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The top ground block of the column; trees standing on it do not count. */
    private static int columnTop(Level level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        while (y > level.getMinBuildHeight()) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
                break;
            }
            y--;
        }
        return y;
    }

    /** The surface block of the biome: sand in the desert, grass elsewhere. */
    private static BlockState groundBlock(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return biome.is(Biomes.DESERT) ? Blocks.SAND.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
    }
}
