package de.craftorio.protection;

import de.craftorio.CraftorioConfig;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.world.OreFieldBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Which team placed a protected block, per dimension. Protected are all Craftorio blocks except world blocks (ore
 * fields, rubble) and every block with a block entity (chests, furnaces, ...). Entries whose block is gone are
 * dropped lazily; a new placement simply overwrites the old owner.
 */
public final class BlockOwnership extends SavedData {
    private static final String FILE_NAME = "craftorio_block_owners";

    private final Map<Long, UUID> owners = new HashMap<>();

    public static BlockOwnership get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(BlockOwnership::new, BlockOwnership::load), FILE_NAME);
    }

    public static boolean enabled() {
        return CraftorioConfig.PROTECTION.get();
    }

    public static boolean isProtectable(BlockState state) {
        if (state.isAir() || state.getBlock() instanceof OreFieldBlock) {
            return false;
        }
        if (state.hasBlockEntity()) {
            return true;
        }
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().equals(de.craftorio.Craftorio.MOD_ID) && state.getBlock().defaultDestroyTime() >= 0;
    }

    public void claim(BlockPos pos, UUID team) {
        owners.put(pos.asLong(), team);
        setDirty();
    }

    public void release(BlockPos pos) {
        if (owners.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    /** The team that currently owns the block at {@code pos}, following team mergers. */
    public Optional<Team> owner(ServerLevel level, BlockPos pos) {
        UUID stored = owners.get(pos.asLong());
        if (stored == null) {
            return Optional.empty();
        }
        if (!isProtectable(level.getBlockState(pos))) {
            release(pos);
            return Optional.empty();
        }
        return TeamData.registry(level.getServer()).resolve(stored);
    }

    /** False if the block at {@code pos} belongs to a team other than {@code team}. */
    public boolean mayAccess(ServerLevel level, BlockPos pos, UUID team) {
        return !enabled() || owner(level, pos).map(owner -> owner.id().equals(team)).orElse(true);
    }

    /** False if both blocks have owners and they differ – automation must not move items between teams. */
    public static boolean sameOwner(ServerLevel level, BlockPos a, BlockPos b) {
        if (!enabled()) {
            return true;
        }
        BlockOwnership ownership = get(level);
        Optional<Team> first = ownership.owner(level, a);
        Optional<Team> second = ownership.owner(level, b);
        return first.isEmpty() || second.isEmpty() || first.get().id().equals(second.get().id());
    }

    /**
     * How many claimed blocks each team has per chunk (chunk key → count), with owners resolved through
     * {@code resolve} (team mergers); owners it does not know are left out.
     */
    public Map<UUID, Map<Long, Integer>> chunkWeights(Function<UUID, Optional<UUID>> resolve) {
        Map<UUID, Map<Long, Integer>> result = new HashMap<>();
        owners.forEach((pos, stored) -> resolve.apply(stored).ifPresent(team -> result
                .computeIfAbsent(team, key -> new HashMap<>())
                .merge(ChunkPos.asLong(BlockPos.getX(pos) >> 4, BlockPos.getZ(pos) >> 4), 1, Integer::sum)));
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        owners.forEach((pos, team) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", pos);
            entry.putUUID("team", team);
            list.add(entry);
        });
        tag.put("owners", list);
        return tag;
    }

    private static BlockOwnership load(CompoundTag tag, HolderLookup.Provider registries) {
        BlockOwnership ownership = new BlockOwnership();
        for (Tag element : tag.getList("owners", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            ownership.owners.put(entry.getLong("pos"), entry.getUUID("team"));
        }
        return ownership;
    }
}
