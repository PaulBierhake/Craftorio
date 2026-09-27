package de.craftorio.defense;

import de.craftorio.Craftorio;
import de.craftorio.economy.Credits;
import de.craftorio.network.TdStatusPayload;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModItems;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.team.TeamRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The defense zones of all teams (one per team, in the overworld) and their running levels. Zones are persisted;
 * a level in progress is not, so a restart simply aborts it without penalty.
 */
@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class TowerDefense extends SavedData {
    /** Towers, portal and path must lie within this many blocks (horizontally) of the zone core. */
    public static final int ZONE_RADIUS = 32;
    private static final String FILE_NAME = "craftorio_tower_defense";
    private static final int STATUS_INTERVAL = 20;
    private static final int AUTO_START_DELAY = 100;

    private final Map<UUID, Zone> zones = new HashMap<>();
    private int statusIn;

    public static final class Zone {
        private BlockPos core;
        private @Nullable BlockPos portal;
        private int level = 1;
        private boolean auto;
        private @Nullable LevelRun run;
        private int autoStartIn = -1;
        private long repairCost;

        public BlockPos core() {
            return core;
        }

        public int level() {
            return level;
        }

        public boolean auto() {
            return auto;
        }

        public Optional<LevelRun> run() {
            return Optional.ofNullable(run);
        }
    }

    public static TowerDefense get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(TowerDefense::new, TowerDefense::load), FILE_NAME);
    }

    public Optional<Zone> zone(UUID team) {
        return Optional.ofNullable(zones.get(team));
    }

    public static boolean inZone(BlockPos core, BlockPos pos) {
        return Math.abs(core.getX() - pos.getX()) <= ZONE_RADIUS && Math.abs(core.getZ() - pos.getZ()) <= ZONE_RADIUS;
    }

    /**
     * A dissolved team's zone passes to the team that took it over. If both have one, the zone with the higher level
     * stays; the other zone core remains as an inactive block that can be broken.
     */
    public void mergeTeams(UUID dissolved, UUID into) {
        Zone zone = zones.remove(dissolved);
        if (zone == null) {
            return;
        }
        Zone existing = zones.get(into);
        if (existing == null || zone.level > existing.level) {
            zones.put(into, zone);
        }
        setDirty();
    }

    // --- zone blocks

    boolean placeCore(UUID team, BlockPos pos) {
        if (zones.containsKey(team)) {
            return false;
        }
        Zone zone = new Zone();
        zone.core = pos.immutable();
        zones.put(team, zone);
        setDirty();
        return true;
    }

    void removeCore(ServerLevel level, BlockPos pos) {
        zones.entrySet().removeIf(entry -> {
            if (!entry.getValue().core.equals(pos)) {
                return false;
            }
            if (entry.getValue().run != null) {
                entry.getValue().run.end(level);
            }
            return true;
        });
        setDirty();
    }

    void setPortal(UUID team, @Nullable BlockPos pos) {
        Zone zone = zones.get(team);
        if (zone != null) {
            zone.portal = pos == null ? null : pos.immutable();
            setDirty();
        }
    }

    void removePortal(BlockPos pos) {
        zones.values().stream().filter(zone -> pos.equals(zone.portal)).forEach(zone -> zone.portal = null);
        setDirty();
    }

    // --- actions (terminal buttons, zone core)

    /** Checks portal and path; returns the traced path as walk points or an error message. */
    public PathCheck checkPath(ServerLevel level, Zone zone) {
        if (zone.portal == null) {
            return new PathCheck(null, Component.translatable("craftorio.td.error.no_portal"));
        }
        BlockPos portal = zone.portal;
        PathTracer.Result result = PathTracer.trace(new int[]{portal.getX(), portal.getY(), portal.getZ()},
                (x, y, z) -> {
                    BlockPos pos = new BlockPos(x, y, z);
                    return inZone(zone.core, pos) && level.getBlockState(pos).is(ModBlocks.PATH_BLOCK.get());
                },
                (x, y, z) -> Math.abs(x - zone.core.getX()) + Math.abs(z - zone.core.getZ()) == 1 && Math.abs(y - zone.core.getY()) <= 1);
        if (!result.ok()) {
            return new PathCheck(null, Component.translatable("craftorio.td.error.path." + result.error().name().toLowerCase(),
                    PathTracer.MIN_LENGTH));
        }
        List<Vec3> points = new ArrayList<>();
        for (int[] block : result.path()) {
            points.add(new Vec3(block[0] + 0.5, block[1] + 1, block[2] + 0.5));
        }
        points.add(new Vec3(zone.core.getX() + 0.5, zone.core.getY() + 1, zone.core.getZ() + 0.5));
        return new PathCheck(points, Component.translatable("craftorio.td.path_ok", result.path().size()));
    }

    public record PathCheck(@Nullable List<Vec3> path, Component message) {
    }

    public Component start(MinecraftServer server, UUID team) {
        Zone zone = zones.get(team);
        if (zone == null) {
            return Component.translatable("craftorio.td.error.no_zone");
        }
        if (zone.run != null) {
            return Component.translatable("craftorio.td.error.running");
        }
        ServerLevel level = server.overworld();
        PathCheck check = checkPath(level, zone);
        if (check.path() == null) {
            return check.message();
        }
        int players = Math.max(1, onlineMembers(server, team).size());
        zone.run = new LevelRun(LevelPlan.of(zone.level, players), check.path(), zone.core);
        zone.run.begin(level);
        zone.autoStartIn = -1;
        broadcast(server, team, Component.translatable("craftorio.td.started", zone.level).withStyle(ChatFormatting.GOLD));
        return Component.translatable("craftorio.td.started", zone.level);
    }

    public void toggleAuto(UUID team) {
        Zone zone = zones.get(team);
        if (zone != null) {
            zone.auto = !zone.auto;
            setDirty();
        }
    }

    /** Repairs every damaged tower and rebuilds every ruin in the zone, if the team can pay for all of it. */
    public Component repairAll(MinecraftServer server, UUID team) {
        Zone zone = zones.get(team);
        if (zone == null) {
            return Component.translatable("craftorio.td.error.no_zone");
        }
        ServerLevel level = server.overworld();
        long cost = repairCost(level, zone.core);
        if (cost == 0) {
            return Component.translatable("craftorio.td.repair.nothing");
        }
        if (!TeamData.registry(server).withdraw(team, cost)) {
            return Component.translatable("craftorio.td.repair.too_expensive", Credits.format(cost));
        }
        for (BlockEntity blockEntity : blockEntitiesInZone(level, zone.core)) {
            if (blockEntity instanceof TowerBlockEntity tower) {
                tower.repair();
            } else if (blockEntity instanceof TowerRuinBlockEntity ruin) {
                ruin.rebuild();
            }
        }
        zone.repairCost = 0;
        return Component.translatable("craftorio.td.repair.done", Credits.format(cost));
    }

    static long repairCost(ServerLevel level, BlockPos core) {
        long cost = 0;
        for (BlockEntity blockEntity : blockEntitiesInZone(level, core)) {
            if (blockEntity instanceof TowerBlockEntity tower) {
                cost += tower.repairCost();
            } else if (blockEntity instanceof TowerRuinBlockEntity ruin) {
                cost += ruin.rebuildCost();
            }
        }
        return cost;
    }

    static List<BlockPos> findTowers(ServerLevel level, BlockPos core) {
        List<BlockPos> towers = new ArrayList<>();
        for (BlockEntity blockEntity : blockEntitiesInZone(level, core)) {
            if (blockEntity instanceof TowerBlockEntity) {
                towers.add(blockEntity.getBlockPos());
            }
        }
        return towers;
    }

    private static List<BlockEntity> blockEntitiesInZone(ServerLevel level, BlockPos core) {
        List<BlockEntity> found = new ArrayList<>();
        ChunkPos min = new ChunkPos(core.offset(-ZONE_RADIUS, 0, -ZONE_RADIUS));
        ChunkPos max = new ChunkPos(core.offset(ZONE_RADIUS, 0, ZONE_RADIUS));
        for (int x = min.x; x <= max.x; x++) {
            for (int z = min.z; z <= max.z; z++) {
                if (level.hasChunk(x, z)) {
                    for (BlockEntity blockEntity : level.getChunk(x, z).getBlockEntities().values()) {
                        if (inZone(core, blockEntity.getBlockPos())) {
                            found.add(blockEntity);
                        }
                    }
                }
            }
        }
        return found;
    }

    // --- ticking

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        get(event.getServer()).tick(event.getServer());
    }

    private void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        boolean sendStatus = --statusIn <= 0;
        if (sendStatus) {
            statusIn = STATUS_INTERVAL;
        }
        for (Map.Entry<UUID, Zone> entry : zones.entrySet()) {
            UUID team = entry.getKey();
            Zone zone = entry.getValue();
            List<ServerPlayer> online = onlineMembers(server, team);
            if (zone.run != null && !online.isEmpty()) {
                LevelRun.Outcome outcome = zone.run.tick(level);
                if (outcome != LevelRun.Outcome.RUNNING) {
                    finish(server, team, zone, outcome == LevelRun.Outcome.WON);
                }
            } else if (zone.run == null && zone.autoStartIn > 0 && --zone.autoStartIn == 0) {
                start(server, team);
            }
            if (sendStatus) {
                zone.repairCost = zone.run == null ? repairCost(level, zone.core) : zone.repairCost;
                TdStatusPayload status = status(zone);
                online.forEach(player -> send(player, status));
            }
        }
        if (sendStatus) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                TeamData.registry(server).teamOf(player.getUUID())
                        .filter(team -> !zones.containsKey(team.id()))
                        .ifPresent(team -> send(player, TdStatusPayload.NONE));
            }
        }
    }

    private void finish(MinecraftServer server, UUID team, Zone zone, boolean won) {
        ServerLevel level = server.overworld();
        LevelPlan plan = zone.run.plan();
        zone.run.end(level);
        zone.run = null;
        if (won) {
            TeamData.registry(server).deposit(team, plan.reward());
            ItemStack key = keyItem(plan.keyReward());
            if (!key.isEmpty()) {
                ItemEntity drop = new ItemEntity(level, zone.core.getX() + 0.5, zone.core.getY() + 1.2, zone.core.getZ() + 0.5, key);
                drop.setUnlimitedLifetime();
                level.addFreshEntity(drop);
                broadcast(server, team, Component.translatable("craftorio.td.key_reward", key.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            broadcast(server, team, Component.translatable("craftorio.td.won", plan.level(), Credits.format(plan.reward()))
                    .withStyle(ChatFormatting.GREEN));
            zone.level++;
            if (zone.auto) {
                zone.autoStartIn = AUTO_START_DELAY;
            }
        } else {
            broadcast(server, team, Component.translatable("craftorio.td.lost", plan.level()).withStyle(ChatFormatting.RED));
        }
        setDirty();
    }

    private static ItemStack keyItem(LevelPlan.KeyReward reward) {
        return switch (reward) {
            case NONE -> ItemStack.EMPTY;
            case DRILL_CORE -> new ItemStack(ModItems.DRILL_CORE.get());
            case RESONANCE_CRYSTAL -> new ItemStack(ModItems.RESONANCE_CRYSTAL.get());
            case DEEP_CORE -> new ItemStack(ModItems.DEEP_CORE.get());
            case STAR_SHARD -> new ItemStack(ModItems.STAR_SHARD.get());
        };
    }

    private static TdStatusPayload status(Zone zone) {
        LevelRun run = zone.run;
        return new TdStatusPayload(true, zone.level, run != null, run == null ? 0 : run.wave(),
                run == null ? 0 : run.plan().waves().size(), run == null ? LevelPlan.LIVES : run.lives(),
                run == null ? 0 : run.enemiesLeft(), zone.auto, zone.repairCost);
    }

    private static void send(ServerPlayer player, TdStatusPayload payload) {
        if (player.connection.hasChannel(TdStatusPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    static List<ServerPlayer> onlineMembers(MinecraftServer server, UUID team) {
        TeamRegistry registry = TeamData.registry(server);
        return registry.team(team).map(Team::members).orElse(Set.of()).stream()
                .map(server.getPlayerList()::getPlayer)
                .filter(Objects::nonNull)
                .toList();
    }

    static void broadcast(MinecraftServer server, UUID team, Component message) {
        onlineMembers(server, team).forEach(player -> player.sendSystemMessage(message));
    }

    // --- persistence

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        zones.forEach((team, zone) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("team", team);
            entry.put("core", NbtUtils.writeBlockPos(zone.core));
            if (zone.portal != null) {
                entry.put("portal", NbtUtils.writeBlockPos(zone.portal));
            }
            entry.putInt("level", zone.level);
            entry.putBoolean("auto", zone.auto);
            list.add(entry);
        });
        tag.put("zones", list);
        return tag;
    }

    private static TowerDefense load(CompoundTag tag, HolderLookup.Provider registries) {
        TowerDefense data = new TowerDefense();
        for (Tag element : tag.getList("zones", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            Zone zone = new Zone();
            zone.core = NbtUtils.readBlockPos(entry, "core").orElse(BlockPos.ZERO);
            zone.portal = NbtUtils.readBlockPos(entry, "portal").orElse(null);
            zone.level = Math.max(1, entry.getInt("level"));
            zone.auto = entry.getBoolean("auto");
            data.zones.put(entry.getUUID("team"), zone);
        }
        return data;
    }
}
