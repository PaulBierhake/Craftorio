package de.craftorio.team;

import de.craftorio.CraftorioConfig;
import de.craftorio.network.TeamSyncPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Persists the {@link TeamRegistry} with the world and pushes balance changes to online members. */
public final class TeamData extends SavedData {
    private static final String FILE_NAME = "craftorio_teams";

    private final TeamRegistry registry = new TeamRegistry(CraftorioConfig.STARTING_CREDITS::get);
    // Teams changed since the last sync; flushed once per server tick so belts selling every tick don't flood the network.
    private final Set<UUID> pendingSync = new HashSet<>();

    private TeamData() {
        registry.setChangeListener(team -> {
            setDirty();
            pendingSync.add(team.id());
        });
    }

    public static TeamData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(TeamData::new, TeamData::load, null), FILE_NAME);
    }

    public static TeamRegistry registry(MinecraftServer server) {
        return get(server).registry;
    }

    public TeamRegistry registry() {
        return registry;
    }

    /** Sends the current team state to every online member of the teams changed since the last call. */
    public void flushSync(MinecraftServer server) {
        if (pendingSync.isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            registry.teamOf(player.getUUID())
                    .filter(team -> pendingSync.contains(team.id()))
                    .ifPresent(team -> sync(player, team));
        }
        pendingSync.clear();
    }

    public static void sync(ServerPlayer player, Team team) {
        PacketDistributor.sendToPlayer(player, new TeamSyncPayload(team.name(), team.balance()));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag teams = new ListTag();
        for (Team team : registry.teams()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", team.id());
            entry.putString("name", team.name());
            entry.putLong("balance", team.balance());
            ListTag members = new ListTag();
            team.members().forEach(member -> members.add(NbtUtils.createUUID(member)));
            entry.put("members", members);
            teams.add(entry);
        }
        tag.put("teams", teams);
        return tag;
    }

    private static TeamData load(CompoundTag tag, HolderLookup.Provider registries) {
        TeamData data = new TeamData();
        for (Tag element : tag.getList("teams", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            List<UUID> members = new ArrayList<>();
            for (Tag member : entry.getList("members", Tag.TAG_INT_ARRAY)) {
                members.add(UUIDUtil.uuidFromIntArray(((IntArrayTag) member).getAsIntArray()));
            }
            data.registry.restore(entry.getUUID("id"), entry.getString("name"), entry.getLong("balance"), members);
        }
        return data;
    }
}
