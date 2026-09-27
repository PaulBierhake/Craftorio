package de.craftorio.world.cave;

import de.craftorio.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Runs once per overworld chunk after all other decoration and rewrites the bands below the surface:
 * cap rock I, the sealed cave layer (unbreakable fill until an entrance unlocks it) and cap rock II.
 */
public final class CaveLayerFeature extends Feature<NoneFeatureConfiguration> {
    public CaveLayerFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkAccess chunk = level.getChunk(context.origin());
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int bottom = Math.max(level.getMinBuildHeight(), CaveLayers.CAP_TWO_BOTTOM);
        BlockState cap = ModBlocks.CAP_ROCK.get().defaultBlockState();
        BlockState fill = ModBlocks.CAVE_RUBBLE.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = bottom; y < CaveLayers.SURFACE_BOTTOM; y++) {
                    pos.set(minX + x, y, minZ + z);
                    BlockState current = chunk.getBlockState(pos);
                    BlockState replacement = switch (CaveLayers.rule(y)) {
                        case KEEP -> null;
                        case CAP -> cap;
                        case FILL -> fill;
                        case CAP_IF_SOLID -> current.isAir() || !current.getFluidState().isEmpty() ? null : cap;
                    };
                    if (replacement != null) {
                        if (current.hasBlockEntity()) {
                            // Chests and spawners of vanilla structures (mineshafts etc.) would be left orphaned.
                            chunk.removeBlockEntity(pos);
                        }
                        chunk.setBlockState(pos, replacement, false);
                    }
                }
            }
        }
        return true;
    }
}
