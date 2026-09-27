package de.craftorio.defense;

import de.craftorio.Craftorio;
import de.craftorio.defense.arena.ArenaBuilder;
import de.craftorio.defense.arena.ArenaLayout;
import de.craftorio.defense.arena.Arenas;
import de.craftorio.defense.arena.Mutator;
import de.craftorio.defense.arena.Tile;
import de.craftorio.economy.Credits;
import de.craftorio.network.TdStatusPayload;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModDataComponents;
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
import net.minecraft.world.item.Item;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The arenas of all teams (one per team, each in its own slot of the arena dimension) and their running levels.
 * Every won level brings a new map: towers are packed into the tower depot, the field is rebuilt with a new
 * layout and the player lays a new path. Arenas are persisted; a level in progress is not, so a restart simply
 * aborts it without penalty.
 */
@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class TowerDefense extends SavedData {
    private static final String FILE_NAME = "craftorio_tower_defense";
    private static final int STATUS_INTERVAL = 20;
    private static final int AUTO_START_DELAY = 100;
    public static final long ENERGY_CAPACITY = 2_000_000;
    public static final int AMMO_CAPACITY = 5_000;

    private final Map<UUID, Zone> zones = new HashMap<>();
    private final Map<Integer, UUID> teamBySlot = new HashMap<>();
    private int nextSlot;
    private int statusIn;
    private long worldSeed;

    public static final class Zone {
        private int slot;
        private int level = 1;
        private boolean auto;
        private boolean built;
        private @Nullable BlockPos home;
        private @Nullable LevelRun run;
        private int autoStartIn = -1;
        private long repairCost;
        private int lastStars;
        private final List<ItemStack> depot = new ArrayList<>();
        private long energy;
        private final Map<Item, Integer> ammo = new LinkedHashMap<>();
        private @Nullable ArenaLayout layout;
        private int layoutLevel;

        public int slot() {
            return slot;
        }

        public BlockPos core() {
            return Arenas.core(slot);
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

        public long energy() {
            return energy;
        }

        public int ammo(Item item) {
            return ammo.getOrDefault(item, 0);
        }

        public int depotSize() {
            return depot.size();
        }
    }

    public static TowerDefense get(MinecraftServer server) {
        TowerDefense data = server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(TowerDefense::new, TowerDefense::load), FILE_NAME);
        data.worldSeed = server.overworld().getSeed();
        return data;
    }

    public static @Nullable ServerLevel arena(MinecraftServer server) {
        ServerLevel arena = server.getLevel(Arenas.DIMENSION);
        // The GameTest server only creates the vanilla dimensions; its tests use arenas in its overworld.
        return arena == null && server instanceof net.minecraft.gametest.framework.GameTestServer ? server.overworld() : arena;
    }

    public Optional<Zone> zone(UUID team) {
        return Optional.ofNullable(zones.get(team));
    }

    public Optional<Zone> zoneAt(int slot) {
        UUID team = teamBySlot.get(slot);
        return team == null ? Optional.empty() : Optional.ofNullable(zones.get(team));
    }

    public Optional<UUID> teamAt(int slot) {
        return Optional.ofNullable(teamBySlot.get(slot));
    }

    private long arenaSeed(Zone zone) {
        return worldSeed * 31 + zone.slot * 0x632BE59BD9B4E019L;
    }

    /** The map of the zone's current level. */
    public ArenaLayout layout(Zone zone) {
        if (zone.layout == null || zone.layoutLevel != zone.level) {
            zone.layout = ArenaLayout.generate(arenaSeed(zone), zone.level);
            zone.layoutLevel = zone.level;
        }
        return zone.layout;
    }

    public ArenaLayout layoutAt(int slot) {
        return zoneAt(slot).map(this::layout).orElseGet(() -> ArenaLayout.generate(slot, 1));
    }

    public Mutator mutator(Zone zone) {
        return Mutator.forLevel(arenaSeed(zone), zone.level);
    }

    /** Mutator of the level running in this slot, if any. */
    public Mutator activeMutator(int slot) {
        return zoneAt(slot).filter(zone -> zone.run != null).map(this::mutator).orElse(Mutator.NONE);
    }

    /**
     * A dissolved team's arena passes to the team that took it over. If both have one, the arena with the higher
     * level stays; the other slot is left empty.
     */
    public void mergeTeams(UUID dissolved, UUID into) {
        Zone zone = zones.remove(dissolved);
        if (zone == null) {
            return;
        }
        teamBySlot.remove(zone.slot);
        Zone existing = zones.get(into);
        if (existing == null || zone.level > existing.level) {
            if (existing != null) {
                teamBySlot.remove(existing.slot);
            }
            zones.put(into, zone);
            teamBySlot.put(zone.slot, into);
        }
        setDirty();
    }

    // --- arena access

    /** Creates the team's arena on first use and builds the frame and the current map. */
    public Zone ensureArena(MinecraftServer server, UUID team) {
        Zone zone = zones.get(team);
        if (zone == null) {
            zone = new Zone();
            zone.slot = nextSlot++;
            zones.put(team, zone);
            teamBySlot.put(zone.slot, team);
            setDirty();
        }
        ServerLevel arena = arena(server);
        if (!zone.built && arena != null) {
            ArenaBuilder.buildFrame(arena, zone.slot);
            ArenaBuilder.buildField(arena, zone.slot, layout(zone), arenaSeed(zone) + zone.level);
            zone.built = true;
            setDirty();
        }
        return zone;
    }

    /** Takes a player through an arena gate into the team's arena and hands out a path wand if they have none. */
    public void enter(ServerPlayer player, BlockPos gate) {
        ServerLevel arena = arena(player.server);
        if (arena == null) {
            return;
        }
        Team team = teamOf(player);
        Zone zone = ensureArena(player.server, team.id());
        zone.home = gate.immutable();
        setDirty();
        Vec3 arrival = Arenas.arrival(zone.slot);
        player.teleportTo(arena, arrival.x, arrival.y, arrival.z, 180F, 20F);
        if (!player.getInventory().contains(new ItemStack(ModItems.PATH_WAND.get()))) {
            player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.PATH_WAND.get()));
        }
        ArenaLayout layout = layout(zone);
        player.sendSystemMessage(Component.translatable("craftorio.arena.welcome", zone.level,
                Component.translatable("craftorio.arena.theme." + layout.theme().name().toLowerCase()),
                Component.translatable("craftorio.arena.theme." + layout.theme().name().toLowerCase() + ".rule"))
                .withStyle(ChatFormatting.GOLD));
        Mutator mutator = mutator(zone);
        if (mutator != Mutator.NONE) {
            player.sendSystemMessage(Component.translatable("craftorio.arena.mutator." + mutator.name().toLowerCase())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Back to the arena gate the team last entered through, or to the world spawn. */
    public void leave(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos target = zone(teamOf(player).id()).map(zone -> zone.home).orElse(null);
        Vec3 pos = target != null ? Vec3.atBottomCenterOf(target.above()) : Vec3.atBottomCenterOf(overworld.getSharedSpawnPos());
        player.teleportTo(overworld, pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
    }

    /** Hands out the packed-up towers and rewards waiting in the depot. */
    public void takeDepot(ServerPlayer player) {
        int slot = Arenas.slotAt(player.blockPosition());
        Zone zone = zones.get(teamOf(player).id());
        if (zone == null || zone.slot != slot) {
            player.displayClientMessage(Component.translatable("craftorio.arena.error.foreign").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (zone.depot.isEmpty()) {
            player.displayClientMessage(Component.translatable("craftorio.arena.depot.empty"), true);
            return;
        }
        int count = zone.depot.size();
        zone.depot.forEach(stack -> player.getInventory().placeItemBackInInventory(stack));
        zone.depot.clear();
        setDirty();
        player.displayClientMessage(Component.translatable("craftorio.arena.depot.taken", count).withStyle(ChatFormatting.GREEN), true);
    }

    // --- arena rules

    /** Why the player may not change the path at this position, or null. */
    public @Nullable String pathEditError(ServerPlayer player, BlockPos pos) {
        String error = fieldError(player, pos);
        if (error != null) {
            return error;
        }
        Zone zone = zoneAt(Arenas.slotAt(pos)).orElseThrow();
        return zone.run != null ? "craftorio.td.error.running" : null;
    }

    /** Why a tower may not stand at this position, or null. */
    public @Nullable String towerPlaceError(ServerPlayer player, BlockPos pos) {
        String error = fieldError(player, pos);
        if (error != null) {
            return error;
        }
        int[] tile = Arenas.tileAt(pos);
        Tile type = layoutAt(Arenas.slotAt(pos)).tile(tile[0], tile[1]);
        boolean ok = type == Tile.GROUND && pos.getY() == Arenas.BUILD_Y || type == Tile.HIGH && pos.getY() == Arenas.HIGH_Y;
        return ok ? null : "craftorio.arena.error.no_tower_here";
    }

    private @Nullable String fieldError(ServerPlayer player, BlockPos pos) {
        if (!Arenas.isArena(player.level())) {
            return "craftorio.arena.error.not_arena";
        }
        if (Arenas.tileAt(pos) == null) {
            return "craftorio.arena.error.outside_field";
        }
        Optional<UUID> owner = teamAt(Arenas.slotAt(pos));
        return owner.isPresent() && owner.get().equals(teamOf(player).id()) ? null : "craftorio.arena.error.foreign";
    }

    // --- supplies from the arena feeder

    /** Takes energy into the team's arena reserve; returns how much was accepted. */
    public int feedEnergy(UUID team, int amount) {
        Zone zone = zones.get(team);
        if (zone == null || amount <= 0) {
            return 0;
        }
        int accepted = (int) Math.min(amount, ENERGY_CAPACITY - zone.energy);
        if (accepted > 0) {
            zone.energy += accepted;
            setDirty();
        }
        return accepted;
    }

    /** Takes ammunition into the team's arena reserve; returns how many items were accepted. */
    public int feedAmmo(UUID team, ItemStack stack, boolean simulate) {
        Zone zone = zones.get(team);
        if (zone == null) {
            return 0;
        }
        int accepted = Math.min(stack.getCount(), AMMO_CAPACITY - zone.ammo(stack.getItem()));
        if (accepted > 0 && !simulate) {
            zone.ammo.merge(stack.getItem(), accepted, Integer::sum);
            setDirty();
        }
        return Math.max(0, accepted);
    }

    /** Used by energy towers in the arena when their own buffer is empty. */
    boolean drawEnergy(int slot, int amount) {
        Zone zone = zoneAt(slot).orElse(null);
        if (zone == null || zone.energy < amount) {
            return false;
        }
        zone.energy -= amount;
        setDirty();
        return true;
    }

    /** Used by ammunition towers in the arena when their own slot is empty. */
    boolean drawAmmo(int slot, Item item) {
        Zone zone = zoneAt(slot).orElse(null);
        if (zone == null || zone.ammo(item) <= 0) {
            return false;
        }
        zone.ammo.merge(item, -1, Integer::sum);
        setDirty();
        return true;
    }

    public Component reserves(UUID team) {
        return zone(team).map(zone -> (Component) Component.translatable("craftorio.arena.feeder.status", zone.energy, ENERGY_CAPACITY,
                        zone.ammo(ModItems.BOLT.get()), zone.ammo(ModItems.CARTRIDGE.get())))
                .orElse(Component.translatable("craftorio.arena.feeder.no_arena"));
    }

    // --- actions (terminal buttons)

    /** Checks the path from the open gate to the core; returns the traced path as walk points or an error message. */
    public PathCheck checkPath(ServerLevel level, Zone zone) {
        BlockPos portal = Arenas.gate(zone.slot, layout(zone).spawnRow());
        BlockPos core = zone.core();
        PathTracer.Result result = PathTracer.trace(new int[]{portal.getX(), portal.getY(), portal.getZ()},
                (x, y, z) -> {
                    BlockPos pos = new BlockPos(x, y, z);
                    return Arenas.tileAt(pos) != null && Arenas.slotAt(pos) == zone.slot && level.getBlockState(pos).is(ModBlocks.PATH_BLOCK.get());
                },
                (x, y, z) -> Math.abs(x - core.getX()) + Math.abs(z - core.getZ()) == 1 && Math.abs(y - core.getY()) <= 1);
        if (!result.ok()) {
            return new PathCheck(null, Component.translatable("craftorio.td.error.path." + result.error().name().toLowerCase(),
                    PathTracer.MIN_LENGTH));
        }
        List<Vec3> points = new ArrayList<>();
        points.add(new Vec3(portal.getX() + 0.5, portal.getY() + 1, portal.getZ() + 0.5));
        for (int[] block : result.path()) {
            points.add(new Vec3(block[0] + 0.5, block[1] + 1, block[2] + 0.5));
        }
        points.add(new Vec3(core.getX() + 0.5, core.getY() + 1, core.getZ() + 0.5));
        return new PathCheck(points, Component.translatable("craftorio.td.path_ok", result.path().size()));
    }

    public record PathCheck(@Nullable List<Vec3> path, Component message) {
    }

    public Component start(MinecraftServer server, UUID team) {
        Zone zone = zones.get(team);
        ServerLevel level = arena(server);
        if (zone == null || level == null) {
            return Component.translatable("craftorio.td.error.no_zone");
        }
        if (zone.run != null) {
            return Component.translatable("craftorio.td.error.running");
        }
        PathCheck check = checkPath(level, zone);
        if (check.path() == null) {
            return check.message();
        }
        int players = Math.max(1, onlineMembers(server, team).size());
        Mutator mutator = mutator(zone);
        zone.run = new LevelRun(LevelPlan.of(zone.level, players), check.path(), zone.core(),
                new LevelRun.Arena(zone.slot, layout(zone), mutator));
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

    /** Sends the next wave now; the skipped waiting time is paid out as bonus credits. */
    public Component callWave(MinecraftServer server, UUID team) {
        Zone zone = zones.get(team);
        if (zone == null || zone.run == null) {
            return Component.translatable("craftorio.td.error.not_running");
        }
        int skipped = zone.run.callNextWave();
        if (skipped <= 0) {
            return Component.translatable("craftorio.td.call.not_now");
        }
        long bonus = LevelPlan.earlyCallBonus(zone.level, skipped);
        if (bonus > 0) {
            TeamData.registry(server).deposit(team, bonus);
        }
        return Component.translatable("craftorio.td.call.done", Credits.format(bonus));
    }

    /** Repairs every damaged tower and rebuilds every ruin in the arena, if the team can pay for all of it. */
    public Component repairAll(MinecraftServer server, UUID team) {
        Zone zone = zones.get(team);
        ServerLevel level = arena(server);
        if (zone == null || level == null) {
            return Component.translatable("craftorio.td.error.no_zone");
        }
        long cost = repairCost(level, zone.slot);
        if (cost == 0) {
            return Component.translatable("craftorio.td.repair.nothing");
        }
        if (!TeamData.registry(server).withdraw(team, cost)) {
            return Component.translatable("craftorio.td.repair.too_expensive", Credits.format(cost));
        }
        for (BlockEntity blockEntity : blockEntitiesInArena(level, zone.slot)) {
            if (blockEntity instanceof TowerBlockEntity tower) {
                tower.repair();
            } else if (blockEntity instanceof TowerRuinBlockEntity ruin) {
                ruin.rebuild();
            }
        }
        zone.repairCost = 0;
        return Component.translatable("craftorio.td.repair.done", Credits.format(cost));
    }

    static long repairCost(ServerLevel level, int slot) {
        long cost = 0;
        for (BlockEntity blockEntity : blockEntitiesInArena(level, slot)) {
            if (blockEntity instanceof TowerBlockEntity tower) {
                cost += tower.repairCost();
            } else if (blockEntity instanceof TowerRuinBlockEntity ruin) {
                cost += ruin.rebuildCost();
            }
        }
        return cost;
    }

    static List<BlockPos> findTowers(ServerLevel level, BlockPos min, BlockPos max) {
        List<BlockPos> towers = new ArrayList<>();
        for (BlockEntity blockEntity : blockEntitiesIn(level, min, max)) {
            if (blockEntity instanceof TowerBlockEntity) {
                towers.add(blockEntity.getBlockPos());
            }
        }
        return towers;
    }

    static BlockPos arenaMin(int slot) {
        return Arenas.field(slot, Arenas.MIN_X, Arenas.MIN_Z, 0);
    }

    static BlockPos arenaMax(int slot) {
        return Arenas.field(slot, Arenas.MAX_X, Arenas.MAX_Z, 0);
    }

    private static List<BlockEntity> blockEntitiesInArena(ServerLevel level, int slot) {
        return blockEntitiesIn(level, arenaMin(slot), arenaMax(slot));
    }

    private static List<BlockEntity> blockEntitiesIn(ServerLevel level, BlockPos min, BlockPos max) {
        List<BlockEntity> found = new ArrayList<>();
        ChunkPos minChunk = new ChunkPos(min);
        ChunkPos maxChunk = new ChunkPos(max);
        for (int x = minChunk.x; x <= maxChunk.x; x++) {
            for (int z = minChunk.z; z <= maxChunk.z; z++) {
                if (level.hasChunk(x, z)) {
                    for (BlockEntity blockEntity : level.getChunk(x, z).getBlockEntities().values()) {
                        BlockPos pos = blockEntity.getBlockPos();
                        if (pos.getX() >= min.getX() && pos.getX() <= max.getX() && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ()) {
                            found.add(blockEntity);
                        }
                    }
                }
            }
        }
        return found;
    }

    /** Packs all towers and ruins of the field into the depot (with level, health and ammunition) and builds the next map. */
    void nextMap(ServerLevel level, Zone zone) {
        for (BlockEntity blockEntity : blockEntitiesInArena(level, zone.slot)) {
            if (blockEntity instanceof TowerBlockEntity tower) {
                ItemStack ammo = tower.ammo().getStackInSlot(0).copy();
                tower.ammo().setStackInSlot(0, ItemStack.EMPTY);
                zone.depot.add(towerItem(tower.type(), tower.upgradeLevel(), tower.health()));
                if (!ammo.isEmpty()) {
                    zone.depot.add(ammo);
                }
            } else if (blockEntity instanceof TowerRuinBlockEntity ruin) {
                zone.depot.add(towerItem(ruin.towerType(), ruin.upgradeLevel(), 0));
            }
        }
        ArenaBuilder.buildField(level, zone.slot, layout(zone), arenaSeed(zone) + zone.level);
        setDirty();
    }

    public static ItemStack towerItem(TowerType type, int upgradeLevel, int health) {
        ItemStack stack = new ItemStack(ModBlocks.tower(type).get());
        stack.set(ModDataComponents.TOWER_STATE.get(), new ModDataComponents.TowerState(upgradeLevel, health));
        return stack;
    }

    // --- ticking

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        get(event.getServer()).tick(event.getServer());
    }

    private void tick(MinecraftServer server) {
        ServerLevel level = arena(server);
        if (level == null) {
            return;
        }
        boolean sendStatus = --statusIn <= 0;
        if (sendStatus) {
            statusIn = STATUS_INTERVAL;
        }
        for (Map.Entry<UUID, Zone> entry : new ArrayList<>(zones.entrySet())) {
            UUID team = entry.getKey();
            Zone zone = entry.getValue();
            List<ServerPlayer> online = onlineMembers(server, team);
            if (zone.run != null && !online.isEmpty()) {
                LevelRun.Outcome outcome = zone.run.tick(level);
                if (outcome != LevelRun.Outcome.RUNNING) {
                    finish(server, level, team, zone, outcome == LevelRun.Outcome.WON);
                }
            } else if (zone.run == null && zone.autoStartIn > 0 && --zone.autoStartIn == 0) {
                // A new map needs a new path first; auto-start waits until one is laid.
                if (checkPath(level, zone).path() != null) {
                    start(server, team);
                } else {
                    zone.autoStartIn = AUTO_START_DELAY;
                }
            }
            if (sendStatus) {
                zone.repairCost = zone.run == null ? repairCost(level, zone.slot) : zone.repairCost;
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

    private void finish(MinecraftServer server, ServerLevel level, UUID team, Zone zone, boolean won) {
        LevelPlan plan = zone.run.plan();
        int lives = zone.run.lives();
        zone.run.end(level);
        zone.run = null;
        if (won) {
            Mutator mutator = mutator(zone);
            long reward = Math.round(plan.reward() * mutator.rewardFactor());
            int stars = LevelPlan.stars(lives);
            long total = reward + LevelPlan.starBonus(reward, stars);
            TeamData.registry(server).deposit(team, total);
            ItemStack key = keyItem(plan.keyReward());
            if (!key.isEmpty()) {
                zone.depot.add(key);
                broadcast(server, team, Component.translatable("craftorio.td.key_reward", key.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            broadcast(server, team, Component.translatable("craftorio.td.won_stars", plan.level(), "★".repeat(stars) + "☆".repeat(3 - stars),
                    Credits.format(total)).withStyle(ChatFormatting.GREEN));
            zone.lastStars = stars;
            zone.level++;
            nextMap(level, zone);
            ArenaLayout next = layout(zone);
            broadcast(server, team, Component.translatable("craftorio.arena.new_map",
                    Component.translatable("craftorio.arena.theme." + next.theme().name().toLowerCase())).withStyle(ChatFormatting.GOLD));
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

    private TdStatusPayload status(Zone zone) {
        LevelRun run = zone.run;
        LevelPlan plan = run != null ? run.plan() : LevelPlan.of(zone.level, 1);
        int previewWave = run == null ? 0 : run.upcomingWave();
        List<Integer> preview = new ArrayList<>();
        if (previewWave < plan.waves().size()) {
            LevelPlan.summary(plan.waves().get(previewWave)).forEach((type, count) -> {
                preview.add(type.ordinal());
                preview.add(count);
            });
        }
        ArenaLayout layout = layout(zone);
        return new TdStatusPayload(true, zone.level, run != null, run == null ? 0 : run.wave(), plan.waves().size(),
                run == null ? LevelPlan.LIVES : run.lives(), run == null ? 0 : run.enemiesLeft(), zone.auto, zone.repairCost,
                layout.theme().ordinal(), mutator(zone).ordinal(), zone.lastStars, preview, zone.energy,
                zone.ammo(ModItems.BOLT.get()), zone.ammo(ModItems.CARTRIDGE.get()), run != null && run.canCallWave());
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

    private static Team teamOf(ServerPlayer player) {
        return TeamData.registry(player.server).ensureTeam(player.getUUID(), player.getGameProfile().getName());
    }

    // --- persistence

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        zones.forEach((team, zone) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("team", team);
            entry.putInt("slot", zone.slot);
            entry.putInt("level", zone.level);
            entry.putBoolean("auto", zone.auto);
            entry.putBoolean("built", zone.built);
            entry.putInt("last_stars", zone.lastStars);
            if (zone.home != null) {
                entry.put("home", NbtUtils.writeBlockPos(zone.home));
            }
            ListTag depot = new ListTag();
            zone.depot.forEach(stack -> depot.add(stack.saveOptional(registries)));
            entry.put("depot", depot);
            entry.putLong("energy", zone.energy);
            entry.putInt("bolts", zone.ammo(ModItems.BOLT.get()));
            entry.putInt("cartridges", zone.ammo(ModItems.CARTRIDGE.get()));
            list.add(entry);
        });
        tag.put("arenas", list);
        tag.putInt("next_slot", nextSlot);
        return tag;
    }

    private static TowerDefense load(CompoundTag tag, HolderLookup.Provider registries) {
        TowerDefense data = new TowerDefense();
        data.nextSlot = tag.getInt("next_slot");
        for (Tag element : tag.getList("arenas", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            Zone zone = new Zone();
            zone.slot = entry.getInt("slot");
            zone.level = Math.max(1, entry.getInt("level"));
            zone.auto = entry.getBoolean("auto");
            zone.built = entry.getBoolean("built");
            zone.lastStars = entry.getInt("last_stars");
            zone.home = NbtUtils.readBlockPos(entry, "home").orElse(null);
            for (Tag stack : entry.getList("depot", Tag.TAG_COMPOUND)) {
                ItemStack parsed = ItemStack.parseOptional(registries, (CompoundTag) stack);
                if (!parsed.isEmpty()) {
                    zone.depot.add(parsed);
                }
            }
            zone.energy = entry.getLong("energy");
            zone.ammo.put(ModItems.BOLT.get(), entry.getInt("bolts"));
            zone.ammo.put(ModItems.CARTRIDGE.get(), entry.getInt("cartridges"));
            UUID team = entry.getUUID("team");
            data.zones.put(team, zone);
            data.teamBySlot.put(zone.slot, team);
            data.nextSlot = Math.max(data.nextSlot, zone.slot + 1);
        }
        // Zones from before the arenas (overworld zone cores) keep their level and get an arena on next use.
        for (Tag element : tag.getList("zones", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            UUID team = entry.getUUID("team");
            if (!data.zones.containsKey(team)) {
                Zone zone = new Zone();
                zone.slot = data.nextSlot++;
                zone.level = Math.max(1, entry.getInt("level"));
                data.zones.put(team, zone);
                data.teamBySlot.put(zone.slot, team);
            }
        }
        return data;
    }
}
