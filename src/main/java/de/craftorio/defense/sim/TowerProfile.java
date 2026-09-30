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

    /** An activated ability: its id, cooldown and duration in seconds. */
    public record Ability(String id, double cooldown, double duration) {
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

    public Attack attack(String id) {
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
            case "attack" -> applyToAttack(attack(effect.has("attack") ? effect.get("attack").getAsString() : "main"), effect);
            case "range" -> {
                if (range >= 0) {
                    if (effect.has("set")) {
                        range = effect.get("set").getAsDouble();
                    }
                    if (effect.has("add")) {
                        range += effect.get("add").getAsDouble();
                    }
                    if (effect.has("mul")) {
                        range *= effect.get("mul").getAsDouble();
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
            case "ability" -> abilities.add(new Ability(effect.get("id").getAsString(), effect.get("cooldown").getAsDouble(),
                    effect.has("duration") ? effect.get("duration").getAsDouble() : 0));
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
