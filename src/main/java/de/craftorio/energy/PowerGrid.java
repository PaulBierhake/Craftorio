package de.craftorio.energy;

import de.craftorio.Craftorio;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Power networks of one level. Poles are wired when closer than the shorter wire range of the two ({@link PoleTier}); every block with an FE
 * storage within the supply radius of a pole joins that pole's network. Generators ({@link PowerSource})
 * supply, everything else consumes. Membership is rebuilt when poles change and once a second (to notice newly
 * placed machines); energy is distributed every tick.
 */
@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class PowerGrid {
    private static final int REBUILD_INTERVAL = 20;
    private static final Map<ServerLevel, PowerGrid> GRIDS = new WeakHashMap<>();

    private final ServerLevel level;
    private final Map<BlockPos, PoleTier> poles = new LinkedHashMap<>();
    private final List<Network> networks = new ArrayList<>();
    private final Map<BlockPos, Network> networkByPole = new HashMap<>();
    private boolean dirty = true;
    private int rebuildIn;

    private PowerGrid(ServerLevel level) {
        this.level = level;
    }

    public static PowerGrid of(ServerLevel level) {
        return GRIDS.computeIfAbsent(level, PowerGrid::new);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            PowerGrid grid = GRIDS.get(serverLevel);
            if (grid != null) {
                grid.tick();
            }
        }
    }

    void addPole(BlockPos pos, PoleTier tier) {
        if (poles.put(pos.immutable(), tier) != tier) {
            dirty = true;
        }
    }

    void removePole(BlockPos pos) {
        if (poles.remove(pos) != null) {
            dirty = true;
        }
    }

    private void tick() {
        if (dirty || --rebuildIn <= 0) {
            rebuild();
        }
        for (Network network : networks) {
            network.distribute(level);
        }
    }

    private void rebuild() {
        dirty = false;
        rebuildIn = REBUILD_INTERVAL;
        networks.clear();
        networkByPole.clear();
        List<BlockPos> poleList = new ArrayList<>(poles.keySet());
        List<int[]> coordinates = poleList.stream().map(pos -> new int[]{pos.getX(), pos.getY(), pos.getZ()}).toList();
        double[] ranges = poleList.stream().mapToDouble(pos -> poles.get(pos).wireRange()).toArray();
        int[] groups = PoleGrouping.group(coordinates, ranges);
        Map<Integer, Network> byGroup = new HashMap<>();
        for (int i = 0; i < poleList.size(); i++) {
            BlockPos pole = poleList.get(i);
            Network network = byGroup.computeIfAbsent(groups[i], group -> new Network());
            network.poles.add(pole);
            networkByPole.put(pole, network);
            int radius = poles.get(pole).supplyRadius();
            for (BlockPos member : BlockPos.betweenClosed(pole.offset(-radius, -radius, -radius), pole.offset(radius, radius, radius))) {
                if (level.isLoaded(member) && !poles.containsKey(member)
                        && level.getCapability(Capabilities.EnergyStorage.BLOCK, member, null) != null) {
                    BlockEntity blockEntity = level.getBlockEntity(member);
                    (blockEntity instanceof PowerSource ? network.sources : network.consumers).add(member.immutable());
                }
            }
        }
        networks.addAll(byGroup.values());
        for (int i = 0; i < poleList.size(); i++) {
            if (level.getBlockEntity(poleList.get(i)) instanceof PowerPoleBlockEntity pole) {
                List<BlockPos> wires = new ArrayList<>();
                for (int j = 0; j < poleList.size(); j++) {
                    // Each wire is stored on one end only so it is drawn once.
                    if (j > i && PoleGrouping.connected(coordinates.get(i), coordinates.get(j), ranges[i], ranges[j])) {
                        wires.add(poleList.get(j));
                    }
                }
                pole.setWires(wires);
            }
        }
    }

    public Component describe(BlockPos pole) {
        Network network = networkByPole.get(pole);
        if (network == null) {
            return Component.translatable("craftorio.power.no_network");
        }
        int satisfaction = network.lastDemand == 0 ? 100 : (int) (100 * network.lastTransferred / network.lastDemand);
        return Component.translatable("craftorio.power.network", network.poles.size(), network.sources.size(),
                network.consumers.size(), network.lastTransferred, network.lastSupply, satisfaction);
    }

    private static final class Network {
        final Set<BlockPos> poles = new HashSet<>();
        final Set<BlockPos> sources = new LinkedHashSet<>();
        final Set<BlockPos> consumers = new LinkedHashSet<>();
        long lastSupply;
        long lastDemand;
        long lastTransferred;

        void distribute(Level level) {
            List<IEnergyStorage> from = storages(level, sources);
            List<IEnergyStorage> to = storages(level, consumers);
            long[] supply = new long[from.size()];
            long[] demand = new long[to.size()];
            for (int i = 0; i < supply.length; i++) {
                supply[i] = from.get(i).extractEnergy(Integer.MAX_VALUE, true);
            }
            for (int i = 0; i < demand.length; i++) {
                demand[i] = to.get(i).receiveEnergy(Integer.MAX_VALUE, true);
            }
            PowerDistribution.Result result = PowerDistribution.distribute(supply, demand);
            for (int i = 0; i < supply.length; i++) {
                if (result.taken()[i] > 0) {
                    from.get(i).extractEnergy((int) result.taken()[i], false);
                }
            }
            for (int i = 0; i < demand.length; i++) {
                if (result.given()[i] > 0) {
                    to.get(i).receiveEnergy((int) result.given()[i], false);
                }
            }
            lastSupply = sum(supply);
            lastDemand = sum(demand);
            lastTransferred = result.transferred();
        }

        private static List<IEnergyStorage> storages(Level level, Set<BlockPos> positions) {
            List<IEnergyStorage> storages = new ArrayList<>(positions.size());
            for (BlockPos pos : positions) {
                if (level.isLoaded(pos)) {
                    IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
                    if (storage != null) {
                        storages.add(storage);
                    }
                }
            }
            return storages;
        }

        private static long sum(long[] values) {
            long total = 0;
            for (long value : values) {
                total += value;
            }
            return total;
        }
    }
}
