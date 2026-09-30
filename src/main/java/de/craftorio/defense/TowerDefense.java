package de.craftorio.defense;

import de.craftorio.Craftorio;
import de.craftorio.defense.arena.ArenaBuilder;
import de.craftorio.defense.arena.ArenaLayout;
import de.craftorio.defense.arena.Arenas;
import de.craftorio.defense.arena.Mutator;
import de.craftorio.defense.arena.Tile;
import de.craftorio.economy.Credits;
import de.craftorio.defense.sim.RoundRules;
import de.craftorio.defense.sim.TowerDef;
import de.craftorio.defense.sim.TowerRules;
import de.craftorio.defense.sim.SimEnemy;
import de.craftorio.defense.sim.TdSimulation;
import de.craftorio.network.TdEnemiesPayload;
import de.craftorio.network.TdPathPayload;
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

import de.craftorio.menu.DepotMenu;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;

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
    /** Ticks between two enemy snapshots for the clients; in between they walk on by themselves. */
    private static final int SNAPSHOT_INTERVAL = 4;
    /** Every this many snapshots the path is sent again, for players who came late. */
    private static final int PATH_EVERY = 10;
    private static final int AUTO_START_DELAY = 100;
    public static final long ENERGY_CAPACITY = 2_000_000;
    public static final int AMMO_CAPACITY = 5_000;
    /** Credits per level that may be exchanged for coins at the terminal, times the level number. */
    public static final long WAR_CHEST_PER_LEVEL = 100;
    /** Units of crude oil the arena reserve holds for flamethrower turrets. */
    public static final int FLUID_CAPACITY = 20_000;

    private final Map<UUID, Zone> zones = new HashMap<>();
    private final Map<Integer, UUID> teamBySlot = new HashMap<>();
    private int nextSlot;
    private int statusIn;
    private int snapshotIn;
    private int pathCounter;
    private long worldSeed;

    public static final class Zone {
        private int slot;
        private int level = 1;
        private boolean auto;
        private boolean built;
        private @Nullable BlockPos home;
        private @Nullable LevelRun run;
        private int autoStartIn = -1;
        /** The team's arena coins ⛁: earned by popping and at the end of rounds, valid across levels. */
        private double coins = RoundRules.START_COINS;
        private Difficulty difficulty = Difficulty.MEDIUM;
        /** Has a level been started? The difficulty can only be made easier from then on. */
        private boolean campaignStarted;
        /** Credits exchanged for coins in the current level (the war chest is limited per level). */
        private long warChestUsed;
        private @Nullable Snapshot snapshot;
        private int lastStars;
        private final List<ItemStack> depot = new ArrayList<>();
        private long energy;
        private final Map<Item, Integer> ammo = new LinkedHashMap<>();
        /** Crude oil for flamethrower turrets, fed by the arena feeder. */
        private int fluid;
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

        /** Whole coins the team has. */
        public long coins() {
            return (long) Math.floor(coins);
        }

        public Difficulty difficulty() {
            return difficulty;
        }

        public boolean campaignStarted() {
            return campaignStarted;
        }

        /** Credits that can still be exchanged for coins in this level. */
        public long warChestLeft() {
            return Math.max(0, WAR_CHEST_PER_LEVEL * level - warChestUsed);
        }

        public long energy() {
            return energy;
        }

        public int ammo(Item item) {
            return ammo.getOrDefault(item, 0);
        }

        public int fluid() {
            return fluid;
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

    /** Test hook: jumps the zone to another level and rebuilds its map. */
    void jumpToLevel(ServerLevel level, Zone zone, int newLevel) {
        zone.level = newLevel;
        ArenaBuilder.buildField(level, zone.slot, layout(zone), arenaSeed(zone) + zone.level);
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
        if (arena != null) {
            ArenaBuilder.addConsole(arena, zone.slot);
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
        if (!player.getInventory().hasAnyMatching(stack -> stack.is(ModItems.PATH_WAND.get()))) {
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

    /** Opens the take-only tower depot of the player's own arena. */
    public void openDepot(ServerPlayer player) {
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
        player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new DepotMenu(id, inventory, depotView(zone)),
                Component.translatable("block.craftorio.tower_depot")));
    }

    /** The depot as a container: only removing is possible; empty entries are dropped when it is closed. */
    public Container depotView(Zone zone) {
        return new Container() {
            @Override
            public int getContainerSize() {
                return DepotMenu.SLOTS;
            }

            @Override
            public boolean isEmpty() {
                return zone.depot.stream().allMatch(ItemStack::isEmpty);
            }

            @Override
            public ItemStack getItem(int index) {
                return index < zone.depot.size() ? zone.depot.get(index) : ItemStack.EMPTY;
            }

            @Override
            public ItemStack removeItem(int index, int count) {
                if (index >= zone.depot.size()) {
                    return ItemStack.EMPTY;
                }
                ItemStack removed = ContainerHelper.removeItem(zone.depot, index, count);
                setChanged();
                return removed;
            }

            @Override
            public ItemStack removeItemNoUpdate(int index) {
                if (index >= zone.depot.size()) {
                    return ItemStack.EMPTY;
                }
                return ContainerHelper.takeItem(zone.depot, index);
            }

            @Override
            public void setItem(int index, ItemStack stack) {
                while (zone.depot.size() <= index) {
                    zone.depot.add(ItemStack.EMPTY);
                }
                zone.depot.set(index, stack);
                setChanged();
            }

            @Override
            public void setChanged() {
                TowerDefense.this.setDirty();
            }

            @Override
            public boolean stillValid(Player player) {
                return Arenas.slotAt(player.blockPosition()) == zone.slot;
            }

            @Override
            public boolean canPlaceItem(int index, ItemStack stack) {
                return false;
            }

            @Override
            public void stopOpen(Player player) {
                zone.depot.removeIf(ItemStack::isEmpty);
                setChanged();
            }

            @Override
            public void clearContent() {
                zone.depot.clear();
            }
        };
    }

    /** Adds to the depot, merging with stacks of the same item and components. */
    static void addToDepot(Zone zone, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (ItemStack present : zone.depot) {
            if (!present.isEmpty() && ItemStack.isSameItemSameComponents(present, rest)) {
                int moved = Math.min(rest.getCount(), present.getMaxStackSize() - present.getCount());
                present.grow(moved);
                rest.shrink(moved);
            }
        }
        if (!rest.isEmpty()) {
            zone.depot.add(rest);
        }
    }

    // --- arena rules

    /** Why the player may not change the path at this position, or null. */
    public @Nullable String pathEditError(ServerPlayer player, BlockPos pos) {
        String error = fieldError(player, pos);
        if (error != null) {
            return error;
        }
        if (zoneAt(Arenas.slotAt(pos)).filter(zone -> zone.run != null).isPresent()) {
            return "craftorio.arena.error.round_running";
        }
        Zone zone = zoneAt(Arenas.slotAt(pos)).orElseThrow();
        return zone.run != null ? "craftorio.td.error.running" : null;
    }

    /** The path block at (or about to be at) this position would create a branch or a 2×2 area. */
    public boolean pathBranches(ServerLevel level, BlockPos pos) {
        int slot = Arenas.slotAt(pos);
        Optional<Zone> zone = zoneAt(slot);
        if (zone.isEmpty()) {
            return false;
        }
        BlockPos portal = Arenas.gate(slot, layoutAt(slot).spawnRow());
        BlockPos core = zone.get().core();
        return !PathTracer.allowsBlock(pos.getX(), pos.getY(), pos.getZ(),
                (x, y, z) -> Arenas.slotAt(new BlockPos(x, y, z)) == slot && level.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.PATH_BLOCK.get()),
                (x, y, z) -> new BlockPos(x, y, z).equals(portal) || new BlockPos(x, y, z).equals(core));
    }

    /** Why a tower may not stand at this position, or null. */
    public @Nullable String towerPlaceError(ServerPlayer player, BlockPos pos, ItemStack stack) {
        String error = towerPlaceError(player, pos);
        return error != null ? error : placementCoinsError(Arenas.slotAt(pos), stack);
    }

    /** A tower from the depot is already paid for; a fresh one costs its base price in coins. */
    @Nullable String placementCoinsError(int slot, ItemStack stack) {
        if (!stack.has(ModDataComponents.TOWER_STATE.get()) && stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                && item.getBlock() instanceof TowerBlock tower) {
            Zone zone = zoneAt(slot).orElse(null);
            if (zone != null && zone.coins() < price(zone.slot, tower.towerType().def().cost())) {
                return "craftorio.tower.place.no_coins";
            }
        }
        return null;
    }

    /** Is this level the one the arenas are in (the GameTest server puts them into its overworld)? */
    public static boolean isArenaLevel(ServerLevel level) {
        return level == arena(level.getServer());
    }

    public @Nullable String towerPlaceError(ServerPlayer player, BlockPos pos) {
        String error = fieldError(player, pos);
        if (error != null) {
            return error;
        }
        int[] tile = Arenas.tileAt(pos);
        Tile type = layoutAt(Arenas.slotAt(pos)).tile(tile[0], tile[1]);
        // Open ground and rough ground (gravel, moss) take towers; only plateaus need their own height.
        boolean ok = (type == Tile.GROUND || type == Tile.ROUGH) && pos.getY() == Arenas.BUILD_Y || type == Tile.HIGH && pos.getY() == Arenas.HIGH_Y;
        return ok ? null : "craftorio.arena.error.no_tower_here";
    }

    /**
     * Lost seals: the team gets every seal of a level it has cleared back while the research that needs it has not been
     * paid yet (and the player carries none). Returns how many were handed out.
     */
    public int claimSeals(ServerPlayer player) {
        Team team = teamOf(player);
        int cleared = zone(team.id()).map(zone -> zone.level - 1).orElse(0);
        var researches = player.server.registryAccess().registryOrThrow(de.craftorio.registry.ModRegistries.RESEARCH);
        int given = 0;
        for (var holder : de.craftorio.research.Researches.sorted(player.server.registryAccess())) {
            if (team.keyItemsPaid(de.craftorio.research.Researches.id(holder))) {
                continue;
            }
            for (var key : holder.value().unlockItems()) {
                ItemStack seal = key.getItems().length == 0 ? ItemStack.EMPTY : key.getItems()[0];
                if (seal.getItem() instanceof de.craftorio.blueprint.KeyMaterialItem material && material.level() <= cleared
                        && !player.getInventory().hasAnyMatching(stack -> stack.is(seal.getItem()))) {
                    player.getInventory().placeItemBackInInventory(new ItemStack(seal.getItem()));
                    given++;
                }
            }
        }
        return given;
    }

    /** Operators: clears tower defense levels for the team without playing them (up to and including {@code upTo}; one level if negative). */
    public Component clearLevels(MinecraftServer server, UUID team, int upTo) {
        ServerLevel arena = arena(server);
        if (arena == null) {
            return Component.translatable("craftorio.td.error.no_zone");
        }
        Zone zone = ensureArena(server, team);
        if (zone.run != null) {
            return Component.translatable("craftorio.td.error.running");
        }
        int target = upTo < 0 ? zone.level : upTo;
        if (target < zone.level) {
            return Component.translatable("craftorio.td.clear.already", target);
        }
        zone.level = target + 1;
        zone.warChestUsed = 0;
        zone.layout = null;
        nextMap(arena, zone);
        setDirty();
        return Component.translatable("craftorio.td.clear.done", target, zone.level);
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

    /** Takes crude oil into the team's arena reserve; returns how many units were accepted. */
    public int feedFluid(UUID team, int amount, boolean simulate) {
        Zone zone = zones.get(team);
        if (zone == null || amount <= 0) {
            return 0;
        }
        int accepted = Math.max(0, Math.min(amount, FLUID_CAPACITY - zone.fluid));
        if (accepted > 0 && !simulate) {
            zone.fluid += accepted;
            setDirty();
        }
        return accepted;
    }

    /** Used by flamethrower turrets in the arena. */
    boolean drawFluid(int slot, int amount) {
        Zone zone = zoneAt(slot).orElse(null);
        if (zone == null || zone.fluid < amount) {
            return false;
        }
        zone.fluid -= amount;
        setDirty();
        return true;
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
                        zone.ammo(ModItems.BOLT.get()), magazines(zone), zone.fluid))
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
                    result.error() == PathTracer.Error.TOO_LONG ? PathTracer.MAX_LENGTH : PathTracer.MIN_LENGTH));
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
        Mutator mutator = mutator(zone);
        zone.snapshot = takeSnapshot(level, zone);
        zone.campaignStarted = true;
        zone.run = new LevelRun(LevelPlan.of(zone.level, 1), check.path(), zone.core(),
                new LevelRun.Arena(zone.slot, layout(zone), mutator), zone.difficulty);
        zone.run.begin(level);
        zone.autoStartIn = -1;
        setDirty();
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

    /** Starts the next round now (as in Bloons TD 6 without a bonus), even while the last one is still going. */
    public Component callWave(MinecraftServer server, UUID team) {
        Zone zone = zones.get(team);
        if (zone == null || zone.run == null) {
            return Component.translatable("craftorio.td.error.not_running");
        }
        if (zone.run.callNextWave() <= 0) {
            return Component.translatable("craftorio.td.call.not_now");
        }
        return Component.translatable("craftorio.td.call.done");
    }

    /** The team's difficulty: any before the first level starts, afterwards only an easier one. */
    public Component setDifficulty(UUID team, Difficulty wanted) {
        Zone zone = zones.get(team);
        if (zone == null) {
            return Component.translatable("craftorio.td.error.no_zone");
        }
        if (zone.run != null) {
            return Component.translatable("craftorio.td.error.running");
        }
        if (wanted == zone.difficulty) {
            return Component.translatable("craftorio.td.difficulty.set", Component.translatable("craftorio.td.difficulty." + wanted.name().toLowerCase()));
        }
        if (!zone.difficulty.mayChangeTo(wanted, zone.campaignStarted)) {
            return Component.translatable("craftorio.td.difficulty.locked");
        }
        zone.difficulty = wanted;
        setDirty();
        return Component.translatable("craftorio.td.difficulty.set", Component.translatable("craftorio.td.difficulty." + wanted.name().toLowerCase()));
    }

    /** The next difficulty: any in turn before the first level; afterwards the next easier one, until the easiest is reached. */
    public Component cycleDifficulty(UUID team) {
        Zone zone = zones.get(team);
        if (zone == null) {
            return Component.translatable("craftorio.td.error.no_zone");
        }
        if (!zone.campaignStarted) {
            return setDifficulty(team, zone.difficulty.next());
        }
        return zone.difficulty.ordinal() == 0 ? Component.translatable("craftorio.td.difficulty.locked")
                : setDifficulty(team, Difficulty.byOrdinal(zone.difficulty.ordinal() - 1));
    }

    /**
     * The war chest: exchanges credits for coins 1:1, at most 100 per level number and level (level 20: 2,000 coins).
     * The factory helps like a small farm without making the arena trivial.
     */
    public Component exchangeCredits(MinecraftServer server, UUID team, long amount) {
        Zone zone = zones.get(team);
        if (zone == null) {
            return Component.translatable("craftorio.td.error.no_zone");
        }
        long allowed = Math.min(amount, zone.warChestLeft());
        if (allowed <= 0) {
            return Component.translatable("craftorio.td.warchest.full", zone.level);
        }
        TeamRegistry registry = TeamData.registry(server);
        if (registry.team(team).map(Team::balance).orElse(0L) < allowed) {
            allowed = registry.team(team).map(Team::balance).orElse(0L);
        }
        if (allowed <= 0 || !registry.withdraw(team, allowed)) {
            return Component.translatable("craftorio.td.warchest.no_credits");
        }
        zone.coins += allowed;
        zone.warChestUsed += allowed;
        setDirty();
        return Component.translatable("craftorio.td.warchest.done", Credits.format(allowed), allowed);
    }

    /** Takes coins from the arena's team; false if it does not have that much. */
    boolean spend(int slot, long amount) {
        Zone zone = zoneAt(slot).orElse(null);
        if (zone == null || amount < 0 || zone.coins < amount) {
            return false;
        }
        zone.coins -= amount;
        setDirty();
        return true;
    }

    /** Gives coins to the arena's team (selling towers). */
    void earn(int slot, long amount) {
        zoneAt(slot).ifPresent(zone -> {
            zone.coins += amount;
            setDirty();
        });
    }

    /** A price in coins with the team's difficulty applied. */
    long price(int slot, long basePrice) {
        return zoneAt(slot).map(zone -> TowerRules.price(basePrice, zone.difficulty.priceFactor())).orElse(basePrice);
    }

    long upgradePrice(int slot, TowerDef.Upgrade upgrade) {
        return price(slot, upgrade.cost());
    }

    /** Operators and the retry: sets the coins directly. */
    void setCoins(Zone zone, double coins) {
        zone.coins = coins;
        setDirty();
    }

    /** What a level starts with: the coins, the depot and every tower on the field with its state, to come back to when the level is lost. */
    private record Snapshot(double coins, List<ItemStack> depot, List<TowerSnapshot> towers) {
    }

    private record TowerSnapshot(BlockPos pos, net.minecraft.world.level.block.state.BlockState state, CompoundTag data) {
    }

    private Snapshot takeSnapshot(ServerLevel level, Zone zone) {
        List<TowerSnapshot> towers = new ArrayList<>();
        for (BlockEntity blockEntity : blockEntitiesInArena(level, zone.slot)) {
            if (blockEntity instanceof TowerBlockEntity tower) {
                towers.add(new TowerSnapshot(tower.getBlockPos(), tower.getBlockState(), tower.saveWithFullMetadata(level.registryAccess())));
            }
        }
        return new Snapshot(zone.coins, zone.depot.stream().map(ItemStack::copy).toList(), towers);
    }

    /** A lost level is tried again from the state it started in: coins, depot and towers come back; the path stays. */
    private void restoreSnapshot(ServerLevel level, Zone zone) {
        Snapshot snapshot = zone.snapshot;
        zone.snapshot = null;
        if (snapshot == null) {
            return;
        }
        for (BlockEntity blockEntity : blockEntitiesInArena(level, zone.slot)) {
            if (blockEntity instanceof TowerBlockEntity tower) {
                tower.dropAmmo();
                level.setBlock(tower.getBlockPos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
        for (TowerSnapshot tower : snapshot.towers()) {
            level.setBlock(tower.pos(), tower.state(), 3);
            BlockEntity blockEntity = level.getBlockEntity(tower.pos());
            if (blockEntity != null) {
                blockEntity.loadWithComponents(tower.data(), level.registryAccess());
            }
        }
        zone.coins = snapshot.coins();
        zone.depot.clear();
        zone.depot.addAll(snapshot.depot());
        setDirty();
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

    /** Packs all towers of the field into the depot (with level and ammunition) and builds the next map. */
    void nextMap(ServerLevel level, Zone zone) {
        for (BlockEntity blockEntity : blockEntitiesInArena(level, zone.slot)) {
            if (blockEntity instanceof TowerBlockEntity tower) {
                ItemStack ammo = tower.ammo().getStackInSlot(0).copy();
                tower.ammo().setStackInSlot(0, ItemStack.EMPTY);
                addToDepot(zone, towerItem(tower.type(), tower.tiers(), tower.paid()));
                if (!ammo.isEmpty()) {
                    addToDepot(zone, ammo);
                }
            }
        }
        ArenaBuilder.buildField(level, zone.slot, layout(zone), arenaSeed(zone) + zone.level);
        setDirty();
    }

    /** A tower item that remembers its upgrades and what was paid for it; placing it again is free. */
    public static ItemStack towerItem(TowerType type, int[] tiers, long paid) {
        ItemStack stack = new ItemStack(ModBlocks.tower(type).get());
        stack.set(ModDataComponents.TOWER_STATE.get(), new ModDataComponents.TowerState(tiers[0], tiers[1], tiers[2], paid));
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
        boolean sendSnapshot = --snapshotIn <= 0;
        if (sendSnapshot) {
            snapshotIn = SNAPSHOT_INTERVAL;
        }
        boolean sendPath = sendSnapshot && ++pathCounter % PATH_EVERY == 0;
        for (Map.Entry<UUID, Zone> entry : new ArrayList<>(zones.entrySet())) {
            UUID team = entry.getKey();
            Zone zone = entry.getValue();
            List<ServerPlayer> online = onlineMembers(server, team);
            if (zone.run != null && !online.isEmpty()) {
                LevelRun.Outcome outcome = zone.run.tick(level);
                zone.coins += zone.run.takeCoins();
                if (outcome != LevelRun.Outcome.RUNNING) {
                    finish(server, level, team, zone, outcome == LevelRun.Outcome.WON);
                } else {
                    syncEnemies(level, zone, sendSnapshot, sendPath);
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
                if (zone.run != null) {
                    setDirty(); // the coins changed
                }
                TdStatusPayload status = status(level, zone);
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

    /** Tells the players in the arena about the path and where the enemies are. */
    private void syncEnemies(ServerLevel level, Zone zone, boolean snapshot, boolean path) {
        if (!snapshot && !path) {
            return;
        }
        List<ServerPlayer> watchers = level.players().stream().filter(player -> Arenas.slotAt(player.blockPosition()) == zone.slot).toList();
        if (watchers.isEmpty()) {
            return;
        }
        LevelRun run = zone.run;
        if (path) {
            TdPathPayload payload = new TdPathPayload(zone.slot, run.points().stream().map(p -> new float[]{(float) p.x, (float) p.y, (float) p.z}).toList());
            watchers.forEach(player -> send(player, payload));
        }
        if (snapshot) {
            TdEnemiesPayload payload = enemiesPayload(zone.slot, run.simulation());
            watchers.forEach(player -> send(player, payload));
        }
    }

    static TdEnemiesPayload enemiesPayload(int slot, TdSimulation sim) {
        List<SimEnemy> enemies = sim.enemies();
        int n = enemies.size();
        int[] ids = new int[n];
        byte[] kinds = new byte[n];
        byte[] flags = new byte[n];
        float[] distances = new float[n];
        // the simulation keeps its enemies in the order of their ids
        for (int i = 0; i < n; i++) {
            SimEnemy enemy = enemies.get(i);
            ids[i] = enemy.id();
            kinds[i] = (byte) enemy.def().index();
            flags[i] = (byte) enemy.flags();
            distances[i] = (float) enemy.distance();
        }
        return new TdEnemiesPayload(slot, (float) sim.speed(), ids, kinds, flags, distances);
    }

    private void finish(MinecraftServer server, ServerLevel level, UUID team, Zone zone, boolean won) {
        LevelPlan plan = zone.run.plan();
        int lives = zone.run.lives();
        zone.run.end(level);
        zone.run = null;
        TdEnemiesPayload none = TdEnemiesPayload.empty(zone.slot);
        level.players().stream().filter(player -> Arenas.slotAt(player.blockPosition()) == zone.slot).forEach(player -> send(player, none));
        if (won) {
            Mutator mutator = mutator(zone);
            long reward = Math.round(plan.reward() * mutator.rewardFactor() * zone.difficulty.rewardFactor());
            int stars = LevelPlan.stars(lives, zone.difficulty.lives());
            zone.snapshot = null;
            zone.warChestUsed = 0;
            long total = reward + LevelPlan.starBonus(reward, stars);
            TeamData.registry(server).deposit(team, total);
            ItemStack key = keyItem(plan.keyReward());
            if (!key.isEmpty()) {
                addToDepot(zone, key);
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
            restoreSnapshot(level, zone);
            broadcast(server, team, Component.translatable("craftorio.td.lost", plan.level()).withStyle(ChatFormatting.RED));
        }
        setDirty();
    }

    /** Magazines of every kind in the arena reserve. */
    private static int magazines(Zone zone) {
        int total = 0;
        for (Magazine kind : Magazine.values()) {
            total += zone.ammo(kind.item());
        }
        return total;
    }

    private static ItemStack keyItem(LevelPlan.KeyReward reward) {
        return switch (reward) {
            case NONE -> ItemStack.EMPTY;
            case BRONZE_SEAL -> new ItemStack(ModItems.BRONZE_SEAL.get());
            case SILVER_SEAL -> new ItemStack(ModItems.SILVER_SEAL.get());
            case GOLD_SEAL -> new ItemStack(ModItems.GOLD_SEAL.get());
            case PLATINUM_SEAL -> new ItemStack(ModItems.PLATINUM_SEAL.get());
            case DIAMOND_SEAL -> new ItemStack(ModItems.DIAMOND_SEAL.get());
            case STAR_SEAL -> new ItemStack(ModItems.STAR_SEAL.get());
        };
    }

    private TdStatusPayload status(ServerLevel level, Zone zone) {
        LevelRun run = zone.run;
        LevelPlan plan = run != null ? run.plan() : LevelPlan.of(zone.level, 1);
        int previewWave = run == null ? 0 : run.upcomingWave();
        List<Integer> preview = new ArrayList<>();
        if (previewWave < plan.rounds().size()) {
            LevelPlan.summary(plan.rounds().get(previewWave)).forEach((code, count) -> {
                preview.add(code);
                preview.add(count);
            });
        }
        ArenaLayout layout = layout(zone);
        return new TdStatusPayload(true, zone.level, run != null, run == null ? 0 : run.wave(), plan.rounds().size(),
                run == null ? zone.difficulty.lives() : run.lives(), run == null ? 0 : run.enemiesLeft(), zone.auto, zone.coins(),
                layout.theme().ordinal(), mutator(zone).ordinal(), zone.lastStars, preview, zone.energy,
                zone.ammo(ModItems.BOLT.get()), magazines(zone), run != null && run.canCallWave(), unsupplied(level, zone),
                zone.difficulty.ordinal(), zone.difficulty.lives(), zone.warChestLeft(), zone.campaignStarted);
    }

    private int unsupplied(ServerLevel level, Zone zone) {
        int count = 0;
        for (BlockEntity blockEntity : blockEntitiesInArena(level, zone.slot)) {
            if (blockEntity instanceof TowerBlockEntity tower && tower.lacksSupply(zone)) {
                count++;
            }
        }
        return count;
    }

    private static void send(ServerPlayer player, TdStatusPayload payload) {
        if (player.connection.hasChannel(TdStatusPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static void send(ServerPlayer player, TdPathPayload payload) {
        if (player.connection.hasChannel(TdPathPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static void send(ServerPlayer player, TdEnemiesPayload payload) {
        if (player.connection.hasChannel(TdEnemiesPayload.TYPE)) {
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
            entry.putDouble("coins", zone.coins);
            entry.putString("difficulty", zone.difficulty.name());
            entry.putBoolean("campaign_started", zone.campaignStarted);
            entry.putLong("war_chest_used", zone.warChestUsed);
            if (zone.home != null) {
                entry.put("home", NbtUtils.writeBlockPos(zone.home));
            }
            ListTag depot = new ListTag();
            zone.depot.forEach(stack -> depot.add(stack.saveOptional(registries)));
            entry.put("depot", depot);
            entry.putLong("energy", zone.energy);
            entry.putInt("bolts", zone.ammo(ModItems.BOLT.get()));
            entry.putInt("cartridges", zone.ammo(ModItems.MAGAZINE.get()));
            entry.putInt("ap_magazines", zone.ammo(ModItems.AP_MAGAZINE.get()));
            entry.putInt("uranium_magazines", zone.ammo(ModItems.URANIUM_MAGAZINE.get()));
            entry.putInt("fluid", zone.fluid);
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
            // Arenas from before the coins start with about half of what Bloons TD 6 would have paid up to their level.
            zone.coins = entry.contains("coins") ? entry.getDouble("coins") : Coins.migratedStart(zone.level);
            try {
                zone.difficulty = Difficulty.valueOf(entry.getString("difficulty"));
            } catch (IllegalArgumentException missing) {
                zone.difficulty = Difficulty.MEDIUM;
            }
            zone.campaignStarted = entry.contains("campaign_started") ? entry.getBoolean("campaign_started") : zone.level > 1;
            zone.warChestUsed = entry.getLong("war_chest_used");
            zone.home = NbtUtils.readBlockPos(entry, "home").orElse(null);
            for (Tag stack : entry.getList("depot", Tag.TAG_COMPOUND)) {
                ItemStack parsed = ItemStack.parseOptional(registries, (CompoundTag) stack);
                if (!parsed.isEmpty()) {
                    zone.depot.add(parsed);
                }
            }
            zone.energy = entry.getLong("energy");
            zone.ammo.put(ModItems.BOLT.get(), entry.getInt("bolts"));
            zone.ammo.put(ModItems.MAGAZINE.get(), entry.getInt("cartridges"));
            zone.ammo.put(ModItems.AP_MAGAZINE.get(), entry.getInt("ap_magazines"));
            zone.ammo.put(ModItems.URANIUM_MAGAZINE.get(), entry.getInt("uranium_magazines"));
            zone.fluid = entry.getInt("fluid");
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
