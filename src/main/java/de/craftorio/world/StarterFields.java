package de.craftorio.world;

import de.craftorio.Craftorio;
import de.craftorio.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Guarantees one small field of each basic resource (iron, copper, coal, stone) near spawn, so every world can start automating. */
@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class StarterFields {
    private static final int SIZE = 30;

    private StarterFields() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        State state = level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new, State::load, null), "craftorio_starter_fields");
        if (state.placed) {
            return;
        }
        BlockPos spawn = level.getSharedSpawnPos();
        RandomSource random = RandomSource.create(level.getSeed());
        place(level, spawn.offset(18, 0, 4), ModBlocks.IRON_ORE_FIELD.get(), random);
        place(level, spawn.offset(-14, 0, 16), ModBlocks.COPPER_ORE_FIELD.get(), random);
        place(level, spawn.offset(2, 0, -20), ModBlocks.COAL_FIELD.get(), random);
        place(level, spawn.offset(-22, 0, -8), ModBlocks.STONE_FIELD.get(), random);
        state.placed = true;
        state.setDirty();
    }

    private static void place(ServerLevel level, BlockPos near, Block block, RandomSource random) {
        level.getChunk(near); // generate the chunk before reading its surface
        BlockPos origin = new BlockPos(near.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near.getX(), near.getZ()), near.getZ());
        int placed = OreFieldFeature.placeField(level, origin, block.defaultBlockState(), SIZE, random);
        Craftorio.LOGGER.info("Placed starter {} with {} blocks at {}", block, placed, origin);
    }

    private static final class State extends SavedData {
        private boolean placed;

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putBoolean("placed", placed);
            return tag;
        }

        private static State load(CompoundTag tag, HolderLookup.Provider registries) {
            State state = new State();
            state.placed = tag.getBoolean("placed");
            return state;
        }
    }
}
