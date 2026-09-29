package de.craftorio.team;

import de.craftorio.Craftorio;
import de.craftorio.CraftorioConfig;
import de.craftorio.protection.BlockOwnership;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Keeps the chunks with a team's machines loaded so the factory does not stop while the team is elsewhere (for
 * example in the arena) or a teammate is logged out. Limited per team, and only while a member is online (both in
 * the server config). Tickets are never kept across restarts; they are set again after the first check.
 */
@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class TeamChunkLoader {
    private static final int INTERVAL = 100;

    private static final TicketController CONTROLLER = new TicketController(Craftorio.id("team_factory"),
            (level, helper) -> new ArrayList<>(helper.getEntityTickets().keySet()).forEach(helper::removeAllTickets));

    /** Chunks currently forced by us: dimension → team → chunk keys. */
    private static final Map<ResourceKey<Level>, Map<UUID, Set<Long>>> FORCED = new HashMap<>();

    private TeamChunkLoader() {
    }

    @EventBusSubscriber(modid = Craftorio.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterTicketControllersEvent event) {
            event.register(CONTROLLER);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL == 0) {
            TeamRegistry registry = TeamData.registry(server);
            update(server, teamId -> registry.team(teamId)
                    .map(team -> team.members().stream().anyMatch(member -> server.getPlayerList().getPlayer(member) != null))
                    .orElse(false));
        }
    }

    /** Forces or releases chunks so that each team keeps its busiest chunks loaded (while {@code online} says so). */
    public static void update(MinecraftServer server, Predicate<UUID> online) {
        int limit = limit();
        boolean onlyOnline = onlyWhileOnline();
        TeamRegistry registry = TeamData.registry(server);
        for (ServerLevel level : server.getAllLevels()) {
            Map<UUID, Map<Long, Integer>> weights = limit > 0 && level.dimension() != Level.END
                    ? BlockOwnership.get(level).chunkWeights(stored -> registry.resolve(stored).map(Team::id))
                    : Map.of();
            Map<UUID, Set<Long>> current = FORCED.computeIfAbsent(level.dimension(), key -> new HashMap<>());
            Set<UUID> teams = new HashSet<>(current.keySet());
            teams.addAll(weights.keySet());
            for (UUID team : teams) {
                Set<Long> wanted = new HashSet<>();
                if (weights.containsKey(team) && (!onlyOnline || online.test(team))) {
                    wanted.addAll(ChunkBudget.select(weights.get(team), limit));
                }
                Set<Long> present = current.computeIfAbsent(team, key -> new HashSet<>());
                for (long chunk : new ArrayList<>(present)) {
                    if (!wanted.contains(chunk)) {
                        CONTROLLER.forceChunk(level, team, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, true);
                        present.remove(chunk);
                    }
                }
                for (long chunk : wanted) {
                    if (present.add(chunk)) {
                        CONTROLLER.forceChunk(level, team, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), true, true);
                    }
                }
                if (present.isEmpty()) {
                    current.remove(team);
                }
            }
        }
    }

    /** The chunks currently kept loaded for the team in this level. */
    public static List<ChunkPos> loadedChunks(ServerLevel level, UUID team) {
        return FORCED.getOrDefault(level.dimension(), Map.of()).getOrDefault(team, Set.of()).stream().map(ChunkPos::new).toList();
    }

    private static int limit() {
        try {
            return CraftorioConfig.CHUNKLOADER_CHUNKS.get();
        } catch (IllegalStateException notLoaded) {
            return 0;
        }
    }

    private static boolean onlyWhileOnline() {
        try {
            return CraftorioConfig.CHUNKLOADER_ONLINE_ONLY.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }
}
