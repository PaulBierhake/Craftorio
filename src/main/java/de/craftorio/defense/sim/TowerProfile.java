package de.craftorio.defense.sim;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a tower of a kind with a given set of upgrades can do: the base values with the effects of every bought
 * upgrade applied, path after path, tier after tier.
 */
public final class TowerProfile {
    /** Targeting range in blocks, negative for unlimited. */
    public double range;
    public final List<Attack> attacks = new ArrayList<>();
    /** The tower sees camouflaged enemies. */
    public boolean detectsCamo;
    /** Named behaviours that code switches on ("spike_o_pult" and the like). */
    public final Set<String> flags = new LinkedHashSet<>();
    /** Named numbers (income per round, radius of an aura, ...). */
    public final Map<String, Double> numbers = new LinkedHashMap<>();
    public final List<Ability> abilities = new ArrayList<>();
    /** Buffs this tower gives other towers around it (villages, the elite sniper) as long as it stands. */
    public final List<Aura> auras = new ArrayList<>();

    /**
     * An aura: the buff, the radius in blocks (negative: everywhere on the level) and which towers get it (a tower id, or
     * empty for all towers, including the one that gives it if {@code self}).
     */
    public record Aura(String id, Buffs buff, double radius, String towers, boolean self) {
        /** Primary towers: the ones the village's training upgrades are for. */
        public static final java.util.Set<String> PRIMARY = java.util.Set.of("crossbow_tower", "flamethrower_turret", "mortar_turret",
                "frost_tower", "glue_turret");

        /** Does the aura reach a tower of this kind ({@code towers}: empty for all, "primary", or tower ids separated by commas)? */
        public boolean appliesTo(String towerId) {
            if (towers.isEmpty()) {
                return true;
            }
            if (towers.equals("primary")) {
                return PRIMARY.contains(towerId);
            }
            return java.util.Arrays.asList(towers.split(",")).contains(towerId);
        }
    }

    /** An activated ability: its id, cooldown and duration in seconds. */
    public record Ability(String id, double cooldown, double duration, Buffs buff, double shareRadius, int shareMax, boolean passive,
                          String shareTowers) {
        public Ability(String id, double cooldown, double duration) {
            this(id, cooldown, duration, null, 0, 0, false, "");
        }
    }

    public static TowerProfile of(TowerDef def, int[] tiers) {
        TowerProfile profile = new TowerProfile();
        profile.range = def.range();
        if (def.attack() != null) {
            profile.attacks.add(def.attack().copy());
        }
        for (int path = 0; path < TowerDef.PATHS; path++) {
            for (int tier = 1; tier <= tiers[path]; tier++) {
                for (JsonObject effect : def.upgrade(path, tier).effects()) {
                    profile.apply(effect);
                }
            }
        }
        return profile;
    }

    /** An attack by id; "main.shrapnel" is the child "shrapnel" of the attack "main". */
    public Attack attack(String id) {
        int dot = id.indexOf('.');
        if (dot > 0) {
            Attack parent = attack(id.substring(0, dot));
            for (Attack child : parent.children) {
                if (child.id.equals(id.substring(dot + 1))) {
                    return child;
                }
            }
            throw new IllegalArgumentException("no child attack " + id);
        }
        for (Attack attack : attacks) {
            if (attack.id.equals(id)) {
                return attack;
            }
        }
        throw new IllegalArgumentException("no attack " + id);
    }

    public Attack main() {
        return attacks.get(0);
    }

    /** The share of the price a sold tower pays back (70 % unless an upgrade changes it). */
    public double sellShare() {
        return numbers.getOrDefault("sell_share", RoundRules.SELL_SHARE);
    }

    public double number(String name) {
        return numbers.getOrDefault(name, 0.0);
    }

    public boolean has(String flag) {
        return flags.contains(flag);
    }

    /**
     * Effect types: {@code attack} (set/add/mul on the fields of an attack, bonus damage), {@code range} (add/mul),
     * {@code camo}, {@code flag}, {@code number}, {@code ability}, {@code new_attack} and {@code replace_attack}.
     */
    public void apply(JsonObject effect) {
        String type = effect.get("type").getAsString();
        switch (type) {
            case "attack" -> {
                Attack attack = attack(effect.has("attack") ? effect.get("attack").getAsString() : "main");
                applyToAttack(attack, effect);
                if (effect.has("radius_from_range")) {
                    attack.radius = range + effect.get("radius_from_range").getAsDouble();
                }
            }
            case "range" -> {
                if (range >= 0) {
                    double before = range;
                    if (effect.has("set")) {
                        range = effect.get("set").getAsDouble();
                    }
                    if (effect.has("add")) {
                        range += effect.get("add").getAsDouble();
                    }
                    if (effect.has("mul")) {
                        range *= effect.get("mul").getAsDouble();
                    }
                    for (Attack attack : attacks) {
                        if (attack.followRange) {
                            if (attack.kind == AttackKind.AURA) {
                                attack.radius += range - before;
                            } else {
                                attack.length += range - before;
                            }
                        }
                    }
                }
            }
            case "camo" -> detectsCamo = true;
            case "flag" -> flags.add(effect.get("name").getAsString());
            case "number" -> {
                String name = effect.get("name").getAsString();
                double value = numbers.getOrDefault(name, 0.0);
                if (effect.has("set")) {
                    value = effect.get("set").getAsDouble();
                }
                if (effect.has("add")) {
                    value += effect.get("add").getAsDouble();
                }
                if (effect.has("mul")) {
                    value *= effect.get("mul").getAsDouble();
                }
                numbers.put(name, value);
            }
            case "ability" -> {
                String id = effect.get("id").getAsString();
                abilities.removeIf(ability -> ability.id().equals(id));
                abilities.add(new Ability(id, effect.get("cooldown").getAsDouble(),
                        effect.has("duration") ? effect.get("duration").getAsDouble() : 0,
                        effect.has("buff") ? buff(effect.getAsJsonObject("buff")) : null,
                        effect.has("share_radius") ? effect.get("share_radius").getAsDouble() : 0,
                        effect.has("share_max") ? effect.get("share_max").getAsInt() : 0,
                        effect.has("passive") && effect.get("passive").getAsBoolean(),
                        effect.has("share_towers") ? effect.get("share_towers").getAsString() : ""));
            }
            case "child" -> {
                Attack parent = attack(effect.has("attack") ? effect.get("attack").getAsString() : "main");
                String id = effect.get("id").getAsString();
                parent.children.removeIf(child -> child.id.equals(id));
                Attack child = TowerDefs.parseAttack(effect.getAsJsonObject("child"), id);
                if (effect.has("trigger")) {
                    child.trigger = effect.get("trigger").getAsString();
                }
                parent.children.add(child);
            }
            case "remove_child" -> attack(effect.has("attack") ? effect.get("attack").getAsString() : "main").children
                    .removeIf(child -> child.id.equals(effect.get("id").getAsString()));
            case "aura" -> {
                String id = effect.get("id").getAsString();
                auras.removeIf(aura -> aura.id().equals(id));
                auras.add(new Aura(id, buff(effect.getAsJsonObject("buff")), effect.has("radius") ? effect.get("radius").getAsDouble() : -1,
                        effect.has("towers") ? effect.get("towers").getAsString() : "", !effect.has("self") || effect.get("self").getAsBoolean()));
            }
            case "remove_aura" -> auras.removeIf(aura -> aura.id().equals(effect.get("id").getAsString()));
            case "remove_ability" -> abilities.removeIf(ability -> ability.id().equals(effect.get("id").getAsString()));
            case "remove_attack" -> attacks.removeIf(attack -> attack.id.equals(effect.get("id").getAsString()));
            case "new_attack" -> attacks.add(TowerDefs.parseAttack(effect.getAsJsonObject("attack"), effect.get("id").getAsString()));
            case "replace_attack" -> {
                String id = effect.has("id") ? effect.get("id").getAsString() : "main";
                Attack replacement = TowerDefs.parseAttack(effect.getAsJsonObject("attack"), id);
                for (int i = 0; i < attacks.size(); i++) {
                    if (attacks.get(i).id.equals(id)) {
                        attacks.set(i, replacement);
                        return;
                    }
                }
                attacks.add(replacement);
            }
            default -> throw new IllegalArgumentException("unknown effect type " + type);
        }
    }

    /** A buff from JSON: cooldown (factor), range (factor), pierce, damage, camo, kind and radius. */
    public static Buffs buff(JsonObject json) {
        return new Buffs(json.has("cooldown") ? json.get("cooldown").getAsDouble() : 1, json.has("range") ? json.get("range").getAsDouble() : 1,
                json.has("pierce") ? json.get("pierce").getAsInt() : 0, json.has("damage") ? json.get("damage").getAsDouble() : 0,
                json.has("camo") && json.get("camo").getAsBoolean(), json.has("kind") ? DamageKind.byId(json.get("kind").getAsString()) : null,
                json.has("radius") ? json.get("radius").getAsDouble() : 0);
    }

    private static void applyToAttack(Attack attack, JsonObject effect) {
        for (String operation : new String[]{"set", "add", "mul"}) {
            if (!effect.has(operation)) {
                continue;
            }
            for (Map.Entry<String, JsonElement> entry : effect.getAsJsonObject(operation).entrySet()) {
                change(attack, entry.getKey(), operation, entry.getValue());
            }
        }
        if (effect.has("bonus")) {
            for (Map.Entry<String, JsonElement> entry : effect.getAsJsonObject("bonus").entrySet()) {
                attack.bonus.merge(entry.getKey(), entry.getValue().getAsDouble(), Double::sum);
            }
        }
    }

    private static void change(Attack attack, String field, String operation, JsonElement value) {
        switch (field) {
            case "kind" -> attack.kind = AttackKind.valueOf(value.getAsString().toUpperCase(java.util.Locale.ROOT));
            case "damage_kind" -> attack.damageKind = DamageKind.byId(value.getAsString());
            case "hits_camo" -> attack.hitsCamo = value.getAsBoolean();
            case "homing" -> attack.homing = value.getAsBoolean();
            case "no_target" -> attack.noTarget = value.getAsBoolean();
            case "unlimited_range" -> attack.unlimitedRange = value.getAsBoolean();
            case "ability_id" -> attack.abilityId = value.getAsString();
            case "maim" -> attack.maim = (int) Math.round(number(attack.maim, operation, value));
            case "brittle" -> attack.brittle = number(attack.brittle, operation, value);
            case "brittle_seconds" -> attack.brittleSeconds = number(attack.brittleSeconds, operation, value);
            case "glue_boss" -> attack.glueBoss = number(attack.glueBoss, operation, value);
            case "glue_boss_seconds" -> attack.glueBossSeconds = number(attack.glueBossSeconds, operation, value);
            case "glue_level" -> attack.glueLevel = (int) Math.round(number(attack.glueLevel, operation, value));
            case "camo_only" -> attack.camoOnly = value.getAsBoolean();
            case "free" -> attack.free = value.getAsBoolean();
            case "soak" -> attack.soak = value.getAsBoolean();
            case "follow_range" -> attack.followRange = value.getAsBoolean();
            case "strip_camo" -> attack.stripCamo = value.getAsBoolean();
            case "skip_frozen" -> attack.skipFrozen = value.getAsBoolean();
            case "boss_only" -> attack.bossOnly = value.getAsBoolean();
            case "trigger" -> attack.trigger = value.getAsString();
            case "crit_every" -> attack.critEvery = (int) Math.round(number(attack.critEvery, operation, value));
            case "crit_damage" -> attack.critDamage = number(attack.critDamage, operation, value);
            case "stun" -> attack.stun = number(attack.stun, operation, value);
            case "stun_boss" -> attack.stunBoss = number(attack.stunBoss, operation, value);
            case "pushback" -> attack.pushback = number(attack.pushback, operation, value);
            case "pushback_boss" -> attack.pushbackBoss = number(attack.pushbackBoss, operation, value);
            case "bounces" -> attack.bounces = (int) Math.round(number(attack.bounces, operation, value));
            case "bounce_radius" -> attack.bounceRadius = number(attack.bounceRadius, operation, value);
            case "burn" -> attack.burn = number(attack.burn, operation, value);
            case "burn_seconds" -> attack.burnSeconds = number(attack.burnSeconds, operation, value);
            case "coins_per_hit" -> attack.coinsPerHit = number(attack.coinsPerHit, operation, value);
            case "scatter_min" -> attack.scatterMin = number(attack.scatterMin, operation, value);
            case "scatter_max" -> attack.scatterMax = number(attack.scatterMax, operation, value);
            case "skip_boss" -> attack.skipBoss = value.getAsBoolean();
            case "cooldown" -> attack.cooldown = number(attack.cooldown, operation, value);
            case "projectiles" -> attack.projectiles = (int) Math.round(number(attack.projectiles, operation, value));
            case "spread" -> attack.spread = number(attack.spread, operation, value);
            case "random_angle" -> attack.randomAngle = number(attack.randomAngle, operation, value);
            case "pierce" -> attack.pierce = (int) Math.round(number(attack.pierce, operation, value));
            case "damage" -> attack.damage = number(attack.damage, operation, value);
            case "length" -> attack.length = number(attack.length, operation, value);
            case "radius" -> attack.radius = number(attack.radius, operation, value);
            case "freeze" -> attack.freeze = number(attack.freeze, operation, value);
            case "freeze_layers" -> attack.freezeLayers = (int) Math.round(number(attack.freezeLayers, operation, value));
            case "glue" -> attack.glue = number(attack.glue, operation, value);
            case "glue_duration" -> attack.glueDuration = number(attack.glueDuration, operation, value);
            case "glue_layers" -> attack.glueLayers = (int) Math.round(number(attack.glueLayers, operation, value));
            default -> throw new IllegalArgumentException("unknown attack field " + field);
        }
    }

    private static double number(double current, String operation, JsonElement value) {
        double amount = value.getAsDouble();
        return switch (operation) {
            case "add" -> current + amount;
            case "mul" -> current * amount;
            default -> amount;
        };
    }
}
