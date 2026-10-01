package de.craftorio.defense.sim;

import de.craftorio.defense.arena.ArenaLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/** Scratch: finds purchase orders that clear the rounds. Run with SEARCH=1. */
class PlanSearchScratchTest {
    static final String TOWERS = System.getenv("TOWERS") != null ? System.getenv("TOWERS") : "DSTB";
    static final int TO = System.getenv("TO") != null ? Integer.parseInt(System.getenv("TO")) : 40;
    static final int FROM = System.getenv("FROM") != null ? Integer.parseInt(System.getenv("FROM")) : 1;
    static final int SLOTS = 7;

    static boolean valid(List<String> plan) {
        int[][] tiers = new int[SLOTS][3];
        boolean[] placed = new boolean[SLOTS];
        for (String word : plan) {
            if (word.startsWith("u")) {
                String[] parts = word.substring(1).split("\\.");
                int slot = Integer.parseInt(parts[0]);
                int path = Integer.parseInt(parts[1]);
                if (!placed[slot] || !TowerRules.canUpgrade(tiers[slot], path)) {
                    return false;
                }
                tiers[slot][path]++;
            } else {
                int slot = Integer.parseInt(word.substring(1));
                if (placed[slot]) {
                    return false;
                }
                placed[slot] = true;
            }
        }
        return true;
    }

    static double fitness(List<String> plan, int layouts) {
        String text = String.join(" ", plan);
        return IntStream.rangeClosed(1, layouts).parallel().mapToDouble(level -> {
            ArenaLayout layout = ArenaLayout.generate(1234, level);
            BalanceSimulator s = new BalanceSimulator(layout.route(), ReferenceSetsTest.plan(text), SLOTS);
            try {
                var r = s.play(FROM, TO, false);
                return Math.min(r.leaked(), 300);
            } catch (IllegalStateException e) {
                return 300;
            }
        }).sum();
    }

    @Test
    void search() {
        if (System.getenv("SEARCH") == null) {
            return;
        }
        Random random = new Random(Long.parseLong(System.getenv().getOrDefault("SEED", "1")));
        List<String> best = new ArrayList<>(List.of(System.getenv().getOrDefault("START", "D0 D1 D2 D3").split(" ")));
        int layouts = Integer.parseInt(System.getenv().getOrDefault("LAYOUTS", "6"));
        double bestScore = fitness(best, layouts);
        long end = System.currentTimeMillis() + Long.parseLong(System.getenv().getOrDefault("SECONDS", "120")) * 1000;
        int iterations = 0;
        while (System.currentTimeMillis() < end && bestScore > 0) {
            iterations++;
            List<String> candidate = new ArrayList<>(best);
            int mutations = 1 + random.nextInt(3);
            for (int m = 0; m < mutations; m++) {
                int kind = random.nextInt(4);
                if (kind == 0 && candidate.size() > 4) {
                    candidate.remove(random.nextInt(candidate.size()));
                } else if (kind == 1 && candidate.size() > 2) {
                    int i = random.nextInt(candidate.size() - 1);
                    String t = candidate.get(i);
                    candidate.set(i, candidate.get(i + 1));
                    candidate.set(i + 1, t);
                } else {
                    String word;
                    if (random.nextInt(6) == 0) {
                        word = TOWERS.charAt(random.nextInt(TOWERS.length())) + "" + random.nextInt(SLOTS);
                    } else {
                        word = "u" + random.nextInt(SLOTS) + "." + random.nextInt(3);
                    }
                    candidate.add(random.nextInt(candidate.size() + 1), word);
                }
            }
            if (!valid(candidate)) {
                continue;
            }
            double score = fitness(candidate, layouts);
            if (score <= bestScore) {
                if (score < bestScore) {
                    System.out.println("PLAN " + score + " [" + iterations + "] " + String.join(" ", candidate));
                }
                best = candidate;
                bestScore = score;
            }
        }
        System.out.println("PLAN FINAL " + bestScore + " after " + iterations + " : " + String.join(" ", best));
    }
}
