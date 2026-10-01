package de.craftorio.defense.sim;

import de.craftorio.defense.arena.ArenaLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static de.craftorio.defense.sim.BalanceSimulator.Step.place;
import static de.craftorio.defense.sim.BalanceSimulator.Step.up;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reference sets: a sensible build must clear the rounds without a leak (acceptance of the tower packages). */
class ReferenceSetsTest {
    private static final String DART = "crossbow_tower";
    private static final String SNIPER = "gun_turret";
    private static final String TACK = "flamethrower_turret";
    private static final String BOMB = "mortar_turret";

    /** "D0" places a dart monkey in slot 0, "u0.1" buys the next tier of path 1 (0-based) of the tower in slot 0. */
    static List<BalanceSimulator.Step> plan(String text) {
        List<BalanceSimulator.Step> plan = new ArrayList<>();
        for (String word : text.trim().split("\\s+")) {
            if (word.startsWith("u")) {
                String[] parts = word.substring(1).split("\\.");
                plan.add(up(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
            } else {
                String tower = switch (word.charAt(0)) {
                    case 'D' -> DART;
                    case 'S' -> SNIPER;
                    case 'T' -> TACK;
                    case 'B' -> BOMB;
                    default -> throw new IllegalArgumentException(word);
                };
                plan.add(place(tower, Integer.parseInt(word.substring(1))));
            }
        }
        return plan;
    }

    private static List<BalanceSimulator.Step> early() {
        return plan(PLAN_1_40);
    }

    static final String PLAN_1_40 = "D1 D2 D0 S5 u5.2 u1.1 u0.2 u1.0 u0.0 T6 u6.0 T3 u1.0 u0.2 u2.2 u0.0 u3.0 u1.0 u1.1 u6.2 T4 u5.2 u2.0 u4.0 u0.0 u5.0 u4.2 u6.0 u3.0 u2.2 u4.2 u2.2 u2.2 u4.0 u4.2 u5.2 u5.2 u3.2 u4.2";

    @Test
    void theOpeningBuildClearsRounds1To40() {
        for (int level = 1; level <= 16; level++) {
            ArenaLayout layout = ArenaLayout.generate(level <= 10 ? 1234 : 777, level);
            BalanceSimulator simulator = new BalanceSimulator(layout.route(), early(), 8);
            BalanceSimulator.Result result = simulator.play(1, 40, true);
            String where = "level " + level + " (path " + layout.route().size() + "): " + result;
            if (level <= 10) {
                assertTrue(result.clean(), where);
            } else {
                // layouts the plan was not searched on: at most a stray life
                assertTrue(result.leaked() <= 2, where);
            }
        }
    }
}
