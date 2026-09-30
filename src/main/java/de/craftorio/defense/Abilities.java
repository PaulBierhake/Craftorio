package de.craftorio.defense;

import de.craftorio.defense.sim.TowerProfile;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * What the activated abilities of the towers do. Abilities that only buff the tower for a while (Fan Club, Call to Arms)
 * need no handler: the attacks ask {@code TowerUnit.abilityIsActive}; the others register one here.
 */
public final class Abilities {
    /** Does what the ability does, on the run the tower fights on. */
    @FunctionalInterface
    public interface Handler {
        void activate(TowerBlockEntity tower, LevelRun run, TowerProfile.Ability ability);
    }

    private static final Map<String, Handler> HANDLERS = new HashMap<>();

    private Abilities() {
    }

    public static void register(String id, Handler handler) {
        HANDLERS.put(id, handler);
    }

    static @Nullable Handler handler(String id) {
        return HANDLERS.get(id);
    }
}
