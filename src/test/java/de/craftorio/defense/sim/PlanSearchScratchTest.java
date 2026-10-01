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
    static final int SLOTS = System.getenv("SLOTS") != null ? Integer.parseInt(System.getenv("SLOTS")) : 7;

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
                var r = s.play(FROM, TO, true);
                return r.clean() ? 0 : (TO - r.firstLeakRound() + 1) * 10 + Math.min(r.leaked(), 100) / 10.0;
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
                int kind = random.nextInt(9);
                if (kind == 0 && candidate.size() > 4) {
                    candidate.remove(random.nextInt(candidate.size()));
                } else if (kind == 1 && candidate.size() > 2) {
                    int i = random.nextInt(candidate.size() - 1);
                    String t = candidate.get(i);
                    candidate.set(i, candidate.get(i + 1));
                    candidate.set(i + 1, t);
                } else if (kind == 2 && candidate.size() > 4) {
                    // move a token somewhere else
                    String t = candidate.remove(random.nextInt(candidate.size()));
                    candidate.add(random.nextInt(candidate.size() + 1), t);
                } else if (kind == 3) {
                    // a whole chain of upgrades of one path, at one place
                    int slot = random.nextInt(SLOTS);
                    int path = random.nextInt(3);
                    int at = random.nextInt(candidate.size() + 1);
                    for (int n = 1 + random.nextInt(5); n > 0; n--) {
                        candidate.add(at, "u" + slot + "." + path);
                    }
                } else if (kind == 7 || kind == 8) {
                    // a macro: a tower with a chain of upgrades of one path (farm, ice, mortar, sniper, ...)
                    boolean[] used = new boolean[SLOTS];
                    for (String word : candidate) {
                        if (!word.startsWith("u")) {
                            used[Integer.parseInt(word.substring(1))] = true;
                        }
                    }
                    List<Integer> free = new ArrayList<>();
                    for (int i = 0; i < SLOTS; i++) {
                        if (!used[i]) {
                            free.add(i);
                        }
                    }
                    if (!free.isEmpty()) {
                        int slot = free.get(random.nextInt(free.size()));
                        char tower = TOWERS.charAt(random.nextInt(TOWERS.length()));
                        int path = random.nextInt(3);
                        int length = tower == 'N' ? 3 : 1 + random.nextInt(5);
                        List<String> block = new ArrayList<>();
                        block.add(tower + "" + slot);
                        for (int n = 0; n < length; n++) {
                            block.add("u" + slot + "." + (tower == 'N' ? 0 : path));
                        }
                        if (tower == 'N') {
                            block.add("u" + slot + ".1");
                            block.add("u" + slot + ".1");
                        }
                        candidate.addAll(random.nextInt(candidate.size() + 1), block);
                    }
                } else if (kind == 4 && candidate.size() > 6) {
                    // move a block of up to four neighbouring tokens
                    int length = 1 + random.nextInt(4);
                    int from = random.nextInt(candidate.size() - length + 1);
                    List<String> block = new ArrayList<>(candidate.subList(from, from + length));
                    candidate.subList(from, from + length).clear();
                    candidate.addAll(random.nextInt(candidate.size() + 1), block);
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
