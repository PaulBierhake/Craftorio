package de.craftorio.team;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import de.craftorio.CraftorioConfig;
import de.craftorio.network.TeamSyncPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import de.craftorio.quest.QuestActions;
import java.util.Set;
import java.util.UUID;

/** Persists the {@link TeamRegistry} with the world and pushes balance changes to online members. */
public final class TeamData extends SavedData {
    private static final String FILE_NAME = "craftorio_teams";

    private final TeamRegistry registry;
    // Teams changed since the last sync; flushed once per server tick so belts selling every tick don't flood the network.
    private final Set<UUID> pendingSync = new HashSet<>();

    private TeamData(MinecraftServer server) {
        registry = new TeamRegistry(CraftorioConfig.STARTING_CREDITS::get, () -> server.overworld().getGameTime());
        // A solo player's defense zone moves with them into the team they join.
        registry.setMergeListener((dissolved, into) -> de.craftorio.defense.TowerDefense.get(server).mergeTeams(dissolved, into));
        registry.setChangeListener(team -> {
            setDirty();
            pendingSync.add(team.id());
        });
    }

    public static TeamData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(() -> new TeamData(server), (tag, registries) -> load(server, tag)), FILE_NAME);
    }

    public static TeamRegistry registry(MinecraftServer server) {
        return get(server).registry;
    }

    public TeamRegistry registry() {
        return registry;
    }

    /** True if the player may spend their team's credits; otherwise tells them why not. */
    public static boolean maySpend(ServerPlayer player) {
        Team team = registry(player.server).ensureTeam(player.getUUID(), player.getGameProfile().getName());
        if (team.canSpend(player.getUUID())) {
            return true;
        }
        player.displayClientMessage(Component.translatable("craftorio.team.error.no_spend_permission").withStyle(ChatFormatting.RED), true);
        return false;
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
        // Players without the mod's channel (e.g. test or fake players) must not receive it – that would crash the tick.
        if (!player.connection.hasChannel(TeamSyncPayload.TYPE)) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new TeamSyncPayload(team.name(), team.balance(), List.copyOf(team.unlocked()), List.copyOf(team.claimedQuests()),
                QuestActions.progressList(player.server, team)));
    }

    private static Map<String, Long> readLongs(CompoundTag tag) {
        Map<String, Long> values = new HashMap<>();
        tag.getAllKeys().forEach(key -> values.put(key, tag.getLong(key)));
        return values;
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
            ListTag unlocked = new ListTag();
            team.unlocked().forEach(id -> unlocked.add(StringTag.valueOf(id)));
            entry.put("unlocked", unlocked);
            entry.putLong("total_earned", team.totalEarned());
            entry.putLong("total_spent", team.totalSpent());
            CompoundTag sales = new CompoundTag();
            team.sales().forEach((item, stat) -> {
                CompoundTag saleTag = new CompoundTag();
                saleTag.putLong("count", stat.count());
                saleTag.putLong("credits", stat.credits());
                sales.put(item, saleTag);
            });
            entry.put("sales", sales);
            entry.putBoolean("members_can_spend", team.membersCanSpend());
            ListTag claimed = new ListTag();
            team.claimedQuests().forEach(id -> claimed.add(StringTag.valueOf(id)));
            entry.put("claimed_quests", claimed);
            CompoundTag built = new CompoundTag();
            team.built().forEach(built::putLong);
            entry.put("built", built);
            teams.add(entry);
        }
        tag.put("teams", teams);
        CompoundTag merged = new CompoundTag();
        registry.mergedTeams().forEach((from, into) -> merged.putUUID(from.toString(), into));
        tag.put("merged", merged);
        return tag;
    }

    private static TeamData load(MinecraftServer server, CompoundTag tag) {
        TeamData data = new TeamData(server);
        for (Tag element : tag.getList("teams", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            List<UUID> members = new ArrayList<>();
            for (Tag member : entry.getList("members", Tag.TAG_INT_ARRAY)) {
                members.add(UUIDUtil.uuidFromIntArray(((IntArrayTag) member).getAsIntArray()));
            }
            List<String> unlocked = entry.getList("unlocked", Tag.TAG_STRING).stream().map(Tag::getAsString).toList();
            Map<String, Team.Sales> sales = new HashMap<>();
            CompoundTag salesTag = entry.getCompound("sales");
            for (String item : salesTag.getAllKeys()) {
                CompoundTag saleTag = salesTag.getCompound(item);
                sales.put(item, new Team.Sales(saleTag.getLong("count"), saleTag.getLong("credits")));
            }
            data.registry.restore(entry.getUUID("id"), entry.getString("name"), entry.getLong("balance"), members,
                    unlocked, entry.getLong("total_earned"), entry.getLong("total_spent"), sales);
            data.registry.restoreSettings(entry.getUUID("id"),
                    !entry.contains("members_can_spend") || entry.getBoolean("members_can_spend"),
                    entry.getList("claimed_quests", Tag.TAG_STRING).stream().map(Tag::getAsString).toList(),
                    readLongs(entry.getCompound("built")));
        }
        CompoundTag merged = tag.getCompound("merged");
        for (String from : merged.getAllKeys()) {
            data.registry.restoreMerge(UUID.fromString(from), merged.getUUID(from));
        }
        return data;
    }
}
