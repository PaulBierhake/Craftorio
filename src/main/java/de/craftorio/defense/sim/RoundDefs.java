package de.craftorio.defense.sim;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** The round lists, read from {@code data/craftorio/td_rounds}. */
public final class RoundDefs {
    private static final List<RoundDef> ROUNDS = load("/data/craftorio/td_rounds/standard.json");
    /** The harder list of the "Alternate Rounds" challenge (Alternate Bloons Rounds). */
    private static final List<RoundDef> ALTERNATE = load("/data/craftorio/td_rounds/alternate.json");

    private RoundDefs() {
    }

    /** Rounds in the list; later rounds are made up by the endless mode. */
    public static int count() {
        return ROUNDS.size();
    }

    private static final java.util.Map<Integer, RoundDef> ENDLESS = new java.util.concurrent.ConcurrentHashMap<>();

    /** Round {@code number} of the list the challenge uses (the standard one, or the alternate rounds). */
    public static RoundDef get(int number, boolean alternate) {
        if (!alternate || number > ALTERNATE.size()) {
            return get(number);
        }
        if (number < 1) {
            throw new IllegalArgumentException("round numbers start at 1: " + number);
        }
        return ALTERNATE.get(number - 1);
    }

    /** Round {@code number} (from 1); past the end of the list the endless mode draws them ({@link FreeplayRounds}). */
    public static RoundDef get(int number) {
        if (number < 1) {
            throw new IllegalArgumentException("round numbers start at 1: " + number);
        }
        if (number > ROUNDS.size()) {
            return ENDLESS.computeIfAbsent(number, FreeplayRounds::round);
        }
        return ROUNDS.get(number - 1);
    }

    /** RBE of a round calculated from the enemy data (late-game rules from round 81 on). */
    public static double rbe(RoundDef round) {
        boolean late = round.round() >= TdSimulation.LATE_GAME_ROUND;
        double factor = TdSimulation.lateHpFactor(round.round());
        double total = 0;
        for (RoundDef.Group group : round.groups()) {
            total += group.count() * EnemyDefs.rbe(EnemyDefs.get(group.enemy()), late, group.fortified(), factor);
        }
        return total;
    }

    private static List<RoundDef> load(String path) {
        try (InputStream stream = RoundDefs.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("missing round data " + path);
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            List<RoundDef> rounds = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("rounds")) {
                JsonObject json = element.getAsJsonObject();
                List<RoundDef.Group> groups = new ArrayList<>();
                for (JsonElement g : json.getAsJsonArray("groups")) {
                    JsonObject group = g.getAsJsonObject();
                    groups.add(new RoundDef.Group(group.get("enemy").getAsString(), group.get("count").getAsInt(), flag(group, "camo"),
                            flag(group, "regrow"), flag(group, "fortified")));
                }
                rounds.add(new RoundDef(json.get("round").getAsInt(), json.get("duration").getAsDouble(), json.get("rbe").getAsInt(),
                        json.get("cash").getAsDouble(), List.copyOf(groups)));
            }
            return List.copyOf(rounds);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read " + path, failure);
        }
    }

    private static boolean flag(JsonObject json, String name) {
        return json.has(name) && json.get(name).getAsBoolean();
    }
}
