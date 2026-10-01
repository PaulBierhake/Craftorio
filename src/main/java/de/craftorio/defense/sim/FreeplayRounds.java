package de.craftorio.defense.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rounds of the endless mode: after the 140 rounds of the standard list they are drawn at random (as in the freeplay of
 * Bloons TD 6), from a seed made of the round number so that every team meets the same round. The enemies' hit points and
 * speed grow with the late-game table ({@link TdSimulation#lateHpFactor}); the budget in RBE grows 3 % a round.
 */
public final class FreeplayRounds {
    /** Average RBE of the standard list's last ten rounds, the budget at round 140. */
    static final double START_BUDGET = 800_000;
    static final double GROWTH = 1.03;
    /** The budget stops growing here (counts are capped as well so a round never needs more than a few thousand enemies). */
    static final double MAX_BUDGET = 5_000_000_000.0;
    static final int MAX_GROUPS = 24;
    static final int MAX_ENEMIES = 3_000;

    private record Kind(String enemy, boolean camo, boolean regrow, boolean fortifiable, int weight, int maxCount) {
    }

    private static final List<Kind> KINDS = List.of(
            new Kind("colossus", false, false, true, 20, 30),
            new Kind("behemoth", false, false, true, 25, 60),
            new Kind("brood_mother", false, false, true, 25, 120),
            new Kind("shadow_hunter", false, false, true, 18, 60),
            new Kind("crystal_golem", true, false, true, 6, 250),
            new Kind("ember_crawler", true, false, false, 3, 150),
            new Kind("ironbreaker", true, true, true, 3, 150));

    private FreeplayRounds() {
    }

    public static double budget(int round) {
        return Math.min(MAX_BUDGET, START_BUDGET * Math.pow(GROWTH, Math.max(0, round - RoundDefs.count())));
    }

    /** A round past the end of the standard list. */
    public static RoundDef round(int number) {
        Random random = new Random(number * 0x9E3779B97F4A7C15L ^ 0x5DEECE66DL);
        double budget = budget(number);
        double hpFactor = TdSimulation.lateHpFactor(number);
        double used = 0;
        int enemies = 0;
        List<RoundDef.Group> groups = new ArrayList<>();
        int totalWeight = KINDS.stream().mapToInt(Kind::weight).sum();
        // every tenth round brings the swarm queen instead (the BAD of the freeplay)
        if (number % 10 == 0) {
            int queens = 1 + (int) Math.min(30, (number - RoundDefs.count()) / 40.0);
            groups.add(new RoundDef.Group("swarm_queen", queens, false, false, random.nextBoolean()));
            used += queens * EnemyDefs.rbe(EnemyDefs.get("swarm_queen"), true, false, hpFactor);
            enemies += queens;
        }
        while (used < budget * 0.95 && groups.size() < MAX_GROUPS && enemies < MAX_ENEMIES) {
            int pick = random.nextInt(totalWeight);
            Kind kind = KINDS.get(0);
            for (Kind candidate : KINDS) {
                pick -= candidate.weight;
                if (pick < 0) {
                    kind = candidate;
                    break;
                }
            }
            boolean fortified = kind.fortifiable && random.nextDouble() < 0.4;
            double each = EnemyDefs.rbe(EnemyDefs.get(kind.enemy), true, fortified, hpFactor);
            int fit = (int) Math.max(1, Math.min(kind.maxCount, (budget - used) / each));
            int count = Math.max(1, Math.min(MAX_ENEMIES - enemies, 1 + random.nextInt(fit)));
            groups.add(new RoundDef.Group(kind.enemy, count, kind.camo, kind.regrow, fortified));
            used += count * each;
            enemies += count;
        }
        double duration = 25 + random.nextInt(26);
        RoundDef round = new RoundDef(number, duration, (int) Math.min(Integer.MAX_VALUE, Math.round(used)), 0, groups);
        return new RoundDef(number, duration, round.rbe(), cash(round), groups);
    }

    private static double cash(RoundDef round) {
        double total = 0;
        for (RoundDef.Group group : round.groups()) {
            total += group.count() * EnemyDefs.cash(EnemyDefs.get(group.enemy()), true);
        }
        return total * RoundRules.incomeFactor(round.round());
    }
}
