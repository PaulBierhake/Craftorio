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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which chunks of the cave and mine layer are open. Unlocking queues chunks for carving; one chunk is carved per
 * server tick (nearest to the entrance first) so a new area appears over a few seconds without a lag spike.
 */
@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class CaveAreas extends SavedData {
    private static final String FILE_NAME = "craftorio_cave_areas";

    private final Map<Layer, Set<Long>> unlocked = new EnumMap<>(Layer.class);
    private final Map<Layer, Deque<Long>> pending = new EnumMap<>(Layer.class);
    private final Map<Layer, CaveShape> shapes = new EnumMap<>(Layer.class);

    private CaveAreas() {
        for (Layer layer : Layer.values()) {
            unlocked.put(layer, new HashSet<>());
            pending.put(layer, new ArrayDeque<>());
        }
    }

    public static CaveAreas get(MinecraftServer server) {
        CaveAreas areas = server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(CaveAreas::new, CaveAreas::load), FILE_NAME);
        if (areas.shapes.isEmpty()) {
            for (Layer layer : Layer.values()) {
                areas.shapes.put(layer, CaveShape.of(server.overworld().getSeed(), layer));
            }
        }
        return areas;
    }

    public CaveShape shape(Layer layer) {
        return shapes.get(layer);
    }

    public boolean isUnlocked(Layer layer, ChunkPos chunk) {
        return unlocked.get(layer).contains(chunk.toLong());
    }

    /** Unlocks the chunks within {@link CaveLayers#UNLOCK_RADIUS} of the entrance; returns how many were new. */
    public int unlockAround(Layer layer, BlockPos entrance) {
        ChunkPos center = new ChunkPos(entrance);
        List<ChunkPos> added = new ArrayList<>();
        for (int x = -CaveLayers.UNLOCK_RADIUS; x <= CaveLayers.UNLOCK_RADIUS; x++) {
            for (int z = -CaveLayers.UNLOCK_RADIUS; z <= CaveLayers.UNLOCK_RADIUS; z++) {
                ChunkPos chunk = new ChunkPos(center.x + x, center.z + z);
                if (unlocked.get(layer).add(chunk.toLong())) {
                    added.add(chunk);
                }
            }
        }
        added.sort(Comparator.comparingInt(chunk -> chunk.getChessboardDistance(center)));
        added.forEach(chunk -> pending.get(layer).add(chunk.toLong()));
        setDirty();
        return added.size();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        CaveAreas areas = get(event.getServer());
        for (Layer layer : Layer.values()) {
            Long next = areas.pending.get(layer).poll();
            if (next != null) {
                ServerLevel level = event.getServer().overworld();
                CaveCarver.carveChunk(level, layer, new ChunkPos(next), areas.shapes.get(layer));
                areas.setDirty();
                return; // one chunk per tick in total
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (Layer layer : Layer.values()) {
            String suffix = layer == Layer.CAVES ? "" : "_" + layer.name().toLowerCase();
            tag.put("unlocked" + suffix, new LongArrayTag(unlocked.get(layer).stream().mapToLong(Long::longValue).toArray()));
            tag.put("pending" + suffix, new LongArrayTag(pending.get(layer).stream().mapToLong(Long::longValue).toArray()));
        }
        return tag;
    }

    private static CaveAreas load(CompoundTag tag, HolderLookup.Provider registries) {
        CaveAreas areas = new CaveAreas();
        for (Layer layer : Layer.values()) {
            String suffix = layer == Layer.CAVES ? "" : "_" + layer.name().toLowerCase();
            for (long chunk : tag.getLongArray("unlocked" + suffix)) {
                areas.unlocked.get(layer).add(chunk);
            }
            for (long chunk : tag.getLongArray("pending" + suffix)) {
                areas.pending.get(layer).add(chunk);
            }
        }
        return areas;
    }
}
