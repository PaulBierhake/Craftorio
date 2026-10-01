package de.craftorio.defense.sim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** All tower kinds, read once from the mod's own data files (see {@code data/craftorio/td_towers}). */
public final class TowerDefs {
    /** Ids in the order the terminal and the guide list them. */
    public static final List<String> IDS = List.of("crossbow_tower", "gun_turret", "mortar_turret", "flamethrower_turret", "frost_tower",
            "glue_turret", "tesla_tower", "laser_tower", "command_post", "supply_depot");

    private static final Map<String, TowerDef> DEFS = load();
    private static final Map<String, TowerProfile> PROFILES = new java.util.concurrent.ConcurrentHashMap<>();

    private TowerDefs() {
    }

    public static TowerDef get(String id) {
        TowerDef def = DEFS.get(id);
        if (def == null) {
            throw new IllegalArgumentException("unknown tower: " + id);
        }
        return def;
    }

    public static List<TowerDef> all() {
        return List.copyOf(DEFS.values());
    }

    /** The profile of a tower with these upgrades; shared, so it must not be changed. */
    public static TowerProfile profile(TowerDef def, int[] tiers) {
        return PROFILES.computeIfAbsent(def.id() + "/" + TowerRules.notation(tiers), key -> TowerProfile.of(def, tiers));
    }

    private static Map<String, TowerDef> load() {
        Map<String, TowerDef> defs = new LinkedHashMap<>();
        for (String id : IDS) {
            String path = "/data/craftorio/td_towers/" + id + ".json";
            try (InputStream stream = TowerDefs.class.getResourceAsStream(path)) {
                if (stream == null) {
                    throw new IllegalStateException("missing tower data " + path);
                }
                defs.put(id, parse(id, JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject()));
            } catch (IOException failure) {
                throw new IllegalStateException("cannot read " + path, failure);
            }
        }
        return Collections.unmodifiableMap(defs);
    }

    private static TowerDef parse(String id, JsonObject json) {
        List<TargetMode> targets = new ArrayList<>();
        for (JsonElement mode : json.getAsJsonArray("targets")) {
            targets.add(TargetMode.byId(mode.getAsString()));
        }
        List<List<TowerDef.Upgrade>> paths = new ArrayList<>();
        for (JsonElement pathElement : json.getAsJsonArray("paths")) {
            List<TowerDef.Upgrade> path = new ArrayList<>();
            for (JsonElement upgradeElement : pathElement.getAsJsonArray()) {
                JsonObject upgrade = upgradeElement.getAsJsonObject();
                List<JsonObject> effects = new ArrayList<>();
                JsonArray array = upgrade.getAsJsonArray("effects");
                for (JsonElement effect : array) {
                    effects.add(effect.getAsJsonObject());
                }
                path.add(new TowerDef.Upgrade(upgrade.get("btd6").getAsString(), upgrade.get("cost").getAsLong(), List.copyOf(effects)));
            }
            paths.add(List.copyOf(path));
        }
        if (paths.size() != TowerDef.PATHS || paths.stream().anyMatch(path -> path.size() != TowerDef.TIERS)) {
            throw new IllegalStateException(id + " needs " + TowerDef.PATHS + " paths of " + TowerDef.TIERS + " tiers");
        }
        return new TowerDef(id, json.get("btd6").getAsString(), json.get("cost").getAsLong(), json.get("range").getAsDouble(),
                json.get("supply").getAsString(), json.get("supply_cost").getAsInt(), List.copyOf(targets), json.has("attack") ? parseAttack(json.getAsJsonObject("attack"), "main") : null,
                List.copyOf(paths));
    }

    /** An attack from its JSON form; fields that are not given keep the defaults of {@link Attack}. */
    static Attack parseAttack(JsonObject json, String id) {
        Attack attack = new Attack();
        attack.id = id;
        attack.kind = AttackKind.valueOf(json.get("kind").getAsString().toUpperCase(Locale.ROOT));
        attack.cooldown = json.get("cooldown").getAsDouble();
        attack.projectiles = json.has("projectiles") ? json.get("projectiles").getAsInt() : 1;
        attack.spread = json.has("spread") ? json.get("spread").getAsDouble() : attack.kind == AttackKind.RING ? 360 : 0;
        attack.randomAngle = json.has("random_angle") ? json.get("random_angle").getAsDouble() : 0;
        attack.pierce = json.get("pierce").getAsInt();
        attack.damage = json.get("damage").getAsDouble();
        attack.damageKind = DamageKind.byId(json.get("damage_kind").getAsString());
        attack.length = json.has("length") ? json.get("length").getAsDouble() : 8;
        attack.radius = json.has("radius") ? json.get("radius").getAsDouble() : 0.2;
        attack.hitsCamo = json.has("hits_camo") && json.get("hits_camo").getAsBoolean();
        attack.skipBoss = json.has("skip_boss") && json.get("skip_boss").getAsBoolean();
        attack.freeze = json.has("freeze") ? json.get("freeze").getAsDouble() : 0;
        attack.freezeLayers = json.has("freeze_layers") ? json.get("freeze_layers").getAsInt() : 2;
        attack.glue = json.has("glue") ? json.get("glue").getAsDouble() : 0;
        attack.glueDuration = json.has("glue_duration") ? json.get("glue_duration").getAsDouble() : 0;
        attack.glueLayers = json.has("glue_layers") ? json.get("glue_layers").getAsInt() : 3;
        attack.noTarget = json.has("no_target") && json.get("no_target").getAsBoolean();
        attack.unlimitedRange = json.has("unlimited_range") && json.get("unlimited_range").getAsBoolean();
        attack.abilityId = json.has("ability_id") ? json.get("ability_id").getAsString() : "";
        attack.brittle = json.has("brittle") ? json.get("brittle").getAsDouble() : 0;
        attack.brittleSeconds = json.has("brittle_seconds") ? json.get("brittle_seconds").getAsDouble() : 0;
        attack.glueBoss = json.has("glue_boss") ? json.get("glue_boss").getAsDouble() : 0;
        attack.glueBossSeconds = json.has("glue_boss_seconds") ? json.get("glue_boss_seconds").getAsDouble() : 0;
        attack.glueLevel = json.has("glue_level") ? json.get("glue_level").getAsInt() : 1;
        attack.camoOnly = json.has("camo_only") && json.get("camo_only").getAsBoolean();
        attack.free = json.has("free") && json.get("free").getAsBoolean();
        attack.soak = json.has("soak") && json.get("soak").getAsBoolean();
        attack.followRange = json.has("follow_range") && json.get("follow_range").getAsBoolean();
        attack.maim = json.has("maim") ? json.get("maim").getAsInt() : 0;
        attack.homing = json.has("homing") && json.get("homing").getAsBoolean();
        attack.stripCamo = json.has("strip_camo") && json.get("strip_camo").getAsBoolean();
        attack.skipFrozen = json.has("skip_frozen") && json.get("skip_frozen").getAsBoolean();
        attack.bossOnly = json.has("boss_only") && json.get("boss_only").getAsBoolean();
        attack.critEvery = json.has("crit_every") ? json.get("crit_every").getAsInt() : 0;
        attack.critDamage = json.has("crit_damage") ? json.get("crit_damage").getAsDouble() : 0;
        attack.stun = json.has("stun") ? json.get("stun").getAsDouble() : 0;
        attack.stunBoss = json.has("stun_boss") ? json.get("stun_boss").getAsDouble() : 0;
        attack.pushback = json.has("pushback") ? json.get("pushback").getAsDouble() : 0;
        attack.pushbackBoss = json.has("pushback_boss") ? json.get("pushback_boss").getAsDouble() : 1;
        attack.bounces = json.has("bounces") ? json.get("bounces").getAsInt() : 0;
        attack.bounceRadius = json.has("bounce_radius") ? json.get("bounce_radius").getAsDouble() : 4;
        attack.burn = json.has("burn") ? json.get("burn").getAsDouble() : 0;
        attack.burnSeconds = json.has("burn_seconds") ? json.get("burn_seconds").getAsDouble() : 0;
        attack.coinsPerHit = json.has("coins_per_hit") ? json.get("coins_per_hit").getAsDouble() : 0;
        attack.scatterMin = json.has("scatter_min") ? json.get("scatter_min").getAsDouble() : 0;
        attack.scatterMax = json.has("scatter_max") ? json.get("scatter_max").getAsDouble() : 0;
        if (json.has("trigger")) {
            attack.trigger = json.get("trigger").getAsString();
        }
        if (json.has("children")) {
            for (JsonElement child : json.getAsJsonArray("children")) {
                JsonObject childJson = child.getAsJsonObject();
                attack.children.add(parseAttack(childJson, childJson.get("id").getAsString()));
            }
        }
        if (json.has("bonus")) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("bonus").entrySet()) {
                attack.bonus.put(entry.getKey(), entry.getValue().getAsDouble());
            }
        }
        return attack;
    }
}
