package de.craftorio.module;

import de.craftorio.Craftorio;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The beacons of every level, so that machines do not have to search for them: a beacon registers when it loads and
 * leaves when it is removed or unloaded. Machines look the beacons up once per second (see {@link ModuleState}).
 */
@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class Beacons {
    /** A beacon reaches 9×9 blocks around it, one block up and down. */
    public static final int RANGE = 4;
    public static final int RANGE_VERTICAL = 1;

    private static final Map<ResourceKey<Level>, Set<BlockPos>> BEACONS = new ConcurrentHashMap<>();

    private Beacons() {
    }

    static void add(Level level, BlockPos pos) {
        BEACONS.computeIfAbsent(level.dimension(), key -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    static void remove(Level level, BlockPos pos) {
        Set<BlockPos> set = BEACONS.get(level.dimension());
        if (set != null) {
            set.remove(pos);
        }
    }

    public static int count(Level level) {
        Set<BlockPos> set = BEACONS.get(level.dimension());
        return set == null ? 0 : set.size();
    }

    /** Does a beacon at {@code beacon} cover a machine at {@code machine}? */
    public static boolean covers(BlockPos beacon, BlockPos machine) {
        return Math.abs(beacon.getX() - machine.getX()) <= RANGE && Math.abs(beacon.getZ() - machine.getZ()) <= RANGE
                && Math.abs(beacon.getY() - machine.getY()) <= RANGE_VERTICAL;
    }

    /** What all running beacons in range give to a machine at {@code pos}: they add up, each at half strength. */
    public static ModuleEffects effectAt(Level level, BlockPos pos) {
        Set<BlockPos> set = BEACONS.get(level.dimension());
        ModuleEffects total = ModuleEffects.NONE;
        if (set == null) {
            return total;
        }
        for (BlockPos beacon : set) {
            if (!beacon.equals(pos) && covers(beacon, pos) && level.getBlockEntity(beacon) instanceof BeaconBlockEntity entity && entity.active()) {
                total = total.plus(entity.transmitted());
            }
        }
        return total;
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        BEACONS.clear();
    }
}
