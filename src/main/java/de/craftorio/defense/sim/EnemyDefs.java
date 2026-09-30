package de.craftorio.defense.sim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All enemy kinds, read once from the mod's own data files (see {@code data/craftorio/td_enemies}). */
public final class EnemyDefs {
    /** Ids in the order of {@link EnemyDef#index()}, from the weakest to the swarm queen. */
    public static final List<String> IDS = List.of("red_crawler", "blue_crawler", "green_crawler", "yellow_crawler", "pink_crawler",
            "soot_crawler", "frost_crawler", "ember_crawler", "ironbreaker", "twilight_crawler", "shimmer_crawler", "crystal_golem",
            "brood_mother", "behemoth", "colossus", "shadow_hunter", "swarm_queen");

    private static final Map<String, EnemyDef> DEFS = load();
    private static final Map<String, Double> RBE = new HashMap<>();
    private static final Map<String, String> PARENTS = new HashMap<>();

    private EnemyDefs() {
    }

    public static EnemyDef get(String id) {
        EnemyDef def = DEFS.get(id);
        if (def == null) {
            throw new IllegalArgumentException("unknown enemy: " + id);
        }
        return def;
    }

    public static EnemyDef byIndex(int index) {
        return DEFS.get(IDS.get(Math.floorMod(index, IDS.size())));
    }

    public static boolean exists(String id) {
        return DEFS.containsKey(id);
    }

    public static List<EnemyDef> all() {
        return List.copyOf(DEFS.values());
    }

    private static Map<String, EnemyDef> load() {
        Map<String, EnemyDef> defs = new LinkedHashMap<>();
        for (String id : IDS) {
            String path = "/data/craftorio/td_enemies/" + id + ".json";
            try (InputStream stream = EnemyDefs.class.getResourceAsStream(path)) {
                if (stream == null) {
                    throw new IllegalStateException("missing enemy data " + path);
                }
                defs.put(id, parse(id, JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject()));
            } catch (IOException failure) {
                throw new IllegalStateException("cannot read " + path, failure);
            }
        }
        return Collections.unmodifiableMap(defs);
    }

    private static EnemyDef parse(String id, JsonObject json) {
        EnumSet<DamageKind> immune = EnumSet.noneOf(DamageKind.class);
        if (json.has("immune")) {
            json.getAsJsonArray("immune").forEach(kind -> immune.add(DamageKind.byId(kind.getAsString())));
        }
        EnemyDef.Late late = null;
        if (json.has("late")) {
            JsonObject l = json.getAsJsonObject("late");
            late = new EnemyDef.Late(l.has("hp") ? l.get("hp").getAsDouble() : json.get("hp").getAsDouble(),
                    l.has("fortified_hp") ? l.get("fortified_hp").getAsDouble() : json.get("fortified_hp").getAsDouble(),
                    l.has("children") ? strings(l.getAsJsonArray("children")) : strings(json.getAsJsonArray("children")),
                    l.has("cash") ? l.get("cash").getAsInt() : 1, l.has("leak") ? l.get("leak").getAsInt() : 0,
                    l.has("fortified_leak") ? l.get("fortified_leak").getAsInt() : 0);
        }
        return new EnemyDef(id, json.get("index").getAsInt(), json.get("btd6").getAsString(), json.get("hp").getAsDouble(),
                json.get("fortified_hp").getAsDouble(), json.get("speed").getAsDouble(), strings(json.getAsJsonArray("children")),
                immune, json.get("boss").getAsBoolean(), json.has("camo") && json.get("camo").getAsBoolean(),
                json.has("child_mods") ? strings(json.getAsJsonArray("child_mods")) : List.of(), late,
                json.has("radius") ? json.get("radius").getAsDouble() : 0.45);
    }

    private static List<String> strings(JsonArray array) {
        List<String> list = new ArrayList<>();
        for (JsonElement element : array) {
            list.add(element.getAsString());
        }
        return list;
    }

    /**
     * Red-blue-equivalents of an enemy with all its children (the BTD6 RBE): hit points plus those of everything that
     * comes out of it. Fortified MOAB-class enemies pass fortification on to fortifiable children.
     */
    public static double rbe(EnemyDef def, boolean lateGame, boolean fortified) {
        return rbe(def, lateGame, fortified, 1);
    }

    /** As {@link #rbe(EnemyDef, boolean, boolean)}; the hit points of MOAB-class layers are multiplied by {@code bossFactor} (late game). */
    public static synchronized double rbe(EnemyDef def, boolean lateGame, boolean fortified, double bossFactor) {
        boolean fort = fortified && def.canBeFortified();
        String key = def.id() + (lateGame ? "/late" : "") + (fort ? "/fort" : "") + "/" + Math.round(bossFactor * 1000);
        Double known = RBE.get(key);
        if (known != null) {
            return known;
        }
        double total = def.hp(lateGame, fort) * (def.boss() ? bossFactor : 1);
        for (String child : def.children(lateGame)) {
            total += rbe(get(child), lateGame, fort && def.boss(), bossFactor);
        }
        RBE.put(key, total);
        return total;
    }

    /**
     * Lives a leaked, undamaged enemy costs: its hit points plus everything that comes out of it (the RBE, with the
     * unscaled hit points); in the late game the super crystal golem costs a fixed 65 (75 fortified), see the wiki's Freeplay page.
     */
    public static synchronized int leak(EnemyDef def, boolean lateGame, boolean fortified) {
        boolean fort = fortified && def.canBeFortified();
        if (lateGame && def.late() != null && def.late().leak() > 0) {
            return fort ? def.late().fortifiedLeak() : def.late().leak();
        }
        double total = def.hp(lateGame, fort);
        for (String child : def.children(lateGame)) {
            total += leak(get(child), lateGame, fort && def.boss());
        }
        return (int) Math.round(total);
    }

    /** Coins popping an enemy with all its children pays (before the income factor). */
    public static int cash(EnemyDef def, boolean lateGame) {
        int total = def.popCash(lateGame);
        for (String child : def.children(lateGame)) {
            total += cash(get(child), lateGame);
        }
        return total;
    }

    /** Layers popped to destroy the enemy completely: one per enemy in the family tree (each pop pays 1 coin). */
    public static int layers(EnemyDef def, boolean lateGame) {
        int total = 1;
        for (String child : def.children(lateGame)) {
            total += layers(get(child), lateGame);
        }
        return total;
    }

    /**
     * The enemy a regrowing {@code current} turns into next when it heals one layer, on the way back to {@code top}
     * (the form it was spawned in); null if {@code current} cannot be reached from {@code top}.
     */
    public static synchronized String regrowStep(String top, String current) {
        if (top.equals(current)) {
            return null;
        }
        String key = top + ">" + current;
        if (PARENTS.containsKey(key)) {
            return PARENTS.get(key);
        }
        String parent = null;
        ArrayDeque<String> queue = new ArrayDeque<>(List.of(top));
        while (!queue.isEmpty() && parent == null) {
            String candidate = queue.poll();
            for (String child : get(candidate).children(false)) {
                if (child.equals(current)) {
                    parent = candidate;
                    break;
                }
                queue.add(child);
            }
        }
        PARENTS.put(key, parent);
        return parent;
    }
}
