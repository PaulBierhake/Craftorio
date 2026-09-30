package de.craftorio.client;

import de.craftorio.defense.sim.EnemyDef;
import de.craftorio.defense.sim.EnemyDefs;
import de.craftorio.defense.sim.TdPath;
import de.craftorio.network.TdEnemiesPayload;
import de.craftorio.network.TdPathPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The enemies of the arenas as last reported by the server, walking on between snapshots. Plain Java so common code can reference it. */
public final class ClientTdEnemies {
    /** One enemy, positioned for the frame being drawn. */
    public record View(int id, EnemyDef def, int flags, double x, double y, double z, float yaw) {
    }

    private static final class Arena {
        TdPath path;
        TdEnemiesPayload snapshot = TdEnemiesPayload.empty(0);
        long receivedAt;
    }

    private static final Map<Integer, Arena> ARENAS = new ConcurrentHashMap<>();

    private ClientTdEnemies() {
    }

    public static void updatePath(TdPathPayload payload) {
        Arena arena = ARENAS.computeIfAbsent(payload.slot(), slot -> new Arena());
        if (arena.path != null && arena.path.size() == payload.points().size()) {
            return; // the same path, sent again for players who came late
        }
        List<double[]> points = new ArrayList<>(payload.points().size());
        payload.points().forEach(point -> points.add(new double[]{point[0], point[1], point[2]}));
        arena.path = points.size() >= 2 ? new TdPath(points) : null;
    }

    public static void updateEnemies(TdEnemiesPayload payload, long gameTime) {
        Arena arena = ARENAS.computeIfAbsent(payload.slot(), slot -> new Arena());
        arena.snapshot = payload;
        arena.receivedAt = gameTime;
    }

    public static void clear() {
        ARENAS.clear();
    }

    /** Every enemy at its place {@code gameTime} (with the fraction of the tick). */
    public static List<View> views(double gameTime) {
        List<View> views = new ArrayList<>();
        double[] at = new double[3];
        double[] ahead = new double[3];
        for (Arena arena : ARENAS.values()) {
            TdEnemiesPayload snapshot = arena.snapshot;
            if (arena.path == null || snapshot.size() == 0) {
                continue;
            }
            double elapsed = Math.max(0, Math.min(40, gameTime - arena.receivedAt));
            for (int i = 0; i < snapshot.size(); i++) {
                EnemyDef def = EnemyDefs.byIndex(snapshot.kinds()[i]);
                double distance = Math.min(arena.path.length(), snapshot.distances()[i] + def.blocksPerTick() * snapshot.speed() * elapsed);
                int segment = arena.path.position(distance, 0, at);
                arena.path.position(Math.min(arena.path.length(), distance + 0.5), segment, ahead);
                float yaw = (float) Math.toDegrees(Math.atan2(ahead[2] - at[2], ahead[0] - at[0])) - 90F;
                views.add(new View(snapshot.ids()[i], def, snapshot.flags()[i], at[0], at[1], at[2], yaw));
            }
        }
        return views;
    }
}
