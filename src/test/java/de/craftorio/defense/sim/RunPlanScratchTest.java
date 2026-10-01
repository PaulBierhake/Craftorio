package de.craftorio.defense.sim;

import de.craftorio.defense.arena.ArenaLayout;
import org.junit.jupiter.api.Test;

import java.util.List;

/** Scratch: plays a plan from the environment (PLAN, TO, LAYOUTS, SLOTS) and prints the leaks per layout. */
class RunPlanScratchTest {
    @Test
    void run() {
        String plan = System.getenv("PLAN");
        if (plan == null) {
            return;
        }
        String chain = System.getenv("CHAIN");
        if (chain != null) {
            String[] tokens = plan.split(" ");
            int layouts2 = Integer.parseInt(System.getenv().getOrDefault("LAYOUTS", "4"));
            int from = Integer.parseInt(System.getenv().getOrDefault("FROM", "0"));
            for (int at = from; at <= tokens.length; at++) {
                List<String> list = new java.util.ArrayList<>(java.util.Arrays.asList(tokens));
                list.addAll(at, java.util.Arrays.asList(chain.split(" ")));
                double sum = 0;
                StringBuilder detail = new StringBuilder();
                for (int level = 1; level <= layouts2; level++) {
                    BalanceSimulator s = new BalanceSimulator(ArenaLayout.generate(1234, level).route(), ReferenceSetsTest.plan(String.join(" ", list)), 16);
                    var r = s.play(1, 100, true);
                    int round = r.clean() ? 101 : r.firstLeakRound();
                    sum += round;
                    detail.append(round).append(' ');
                }
                System.out.println("RUN at " + at + " avg " + String.format("%.1f", sum / layouts2) + " : " + detail);
            }
            return;
        }
        int to = Integer.parseInt(System.getenv().getOrDefault("TO", "100"));
        int layouts = Integer.parseInt(System.getenv().getOrDefault("LAYOUTS", "4"));
        int slots = Integer.parseInt(System.getenv().getOrDefault("SLOTS", "12"));
        for (int level = 1; level <= layouts; level++) {
            long start = System.currentTimeMillis();
            BalanceSimulator s = new BalanceSimulator(ArenaLayout.generate(1234, level).route(), ReferenceSetsTest.plan(plan), slots);
            int fromRound = Integer.parseInt(System.getenv().getOrDefault("FROM_ROUND", "1"));
            if (System.getenv("COINS") != null) {
                s.addCoins(Double.parseDouble(System.getenv("COINS")));
            }
            var r = s.play(fromRound, to, true);
            System.out.println("RUN level " + level + " " + r + " spent " + s.spent() + " in " + (System.currentTimeMillis() - start) + " ms " + s.leaks);
        }
    }
}
