package de.craftorio.world.cave;

import de.craftorio.Craftorio;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Which cave chunks are open. Unlocking queues chunks for carving; one chunk is carved per server tick
 * (nearest to the entrance first) so a new area appears over a few seconds without a lag spike.
 */
@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class CaveAreas extends SavedData {
    private static final String FILE_NAME = "craftorio_cave_areas";

    private final Set<Long> unlocked = new HashSet<>();
    private final Deque<Long> pending = new ArrayDeque<>();
    private CaveShape shape;

    public static CaveAreas get(MinecraftServer server) {
        CaveAreas areas = server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(CaveAreas::new, CaveAreas::load), FILE_NAME);
        if (areas.shape == null) {
            areas.shape = new CaveShape(server.overworld().getSeed());
        }
        return areas;
    }

    public CaveShape shape() {
        return shape;
    }

    public boolean isUnlocked(ChunkPos chunk) {
        return unlocked.contains(chunk.toLong());
    }

    public int pendingCount() {
        return pending.size();
    }

    /** Unlocks the chunks within {@link CaveLayers#UNLOCK_RADIUS} of the entrance; returns how many were new. */
    public int unlockAround(BlockPos entrance) {
        ChunkPos center = new ChunkPos(entrance);
        List<ChunkPos> added = new ArrayList<>();
        for (int x = -CaveLayers.UNLOCK_RADIUS; x <= CaveLayers.UNLOCK_RADIUS; x++) {
            for (int z = -CaveLayers.UNLOCK_RADIUS; z <= CaveLayers.UNLOCK_RADIUS; z++) {
                ChunkPos chunk = new ChunkPos(center.x + x, center.z + z);
                if (unlocked.add(chunk.toLong())) {
                    added.add(chunk);
                }
            }
        }
        added.sort(Comparator.comparingInt(chunk -> chunk.getChessboardDistance(center)));
        added.forEach(chunk -> pending.add(chunk.toLong()));
        setDirty();
        return added.size();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        CaveAreas areas = get(event.getServer());
        Long next = areas.pending.poll();
        if (next != null) {
            ServerLevel level = event.getServer().overworld();
            CaveCarver.carveChunk(level, new ChunkPos(next), areas.shape);
            areas.setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("unlocked", new LongArrayTag(unlocked.stream().mapToLong(Long::longValue).toArray()));
        tag.put("pending", new LongArrayTag(pending.stream().mapToLong(Long::longValue).toArray()));
        return tag;
    }

    private static CaveAreas load(CompoundTag tag, HolderLookup.Provider registries) {
        CaveAreas areas = new CaveAreas();
        for (long chunk : tag.getLongArray("unlocked")) {
            areas.unlocked.add(chunk);
        }
        for (long chunk : tag.getLongArray("pending")) {
            areas.pending.add(chunk);
        }
        return areas;
    }
}
