package de.craftorio.defense.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays the rounds of the standard list headless: a straight path with towers along it, bought in a fixed order as soon as
 * the coins allow, rounds one after the other. Used for the reference sets that must clear the rounds without a leak.
 */
final class BalanceSimulator {
    /** How far a good spot for a tower is looked for: the path it covers within this distance counts. */
    private static final double SPOT_RADIUS = 4.0;

    /** A purchase: a new tower in a slot ({@code path} -1) or the next tier of a path of the tower there. */
    record Step(String tower, int slot, int path) {
        static Step place(String tower, int slot) {
            return new Step(tower, slot, -1);
        }

        static Step up(int slot, int path) {
            return new Step(null, slot, path);
        }
    }

    static final class Slot {
        final double x;
        final double z;
        TowerUnit unit;
        String id;
        final DepotEconomy depot = new DepotEconomy();
        Buffs outside = Buffs.NONE;
        final int[] tiers = new int[3];

        Slot(double x, double z) {
            this.x = x;
            this.z = z;
        }
    }

    record Result(int firstLeakRound, int leaked, long coins, int rounds) {
        boolean clean() {
            return leaked == 0;
        }
    }

    private final List<Slot> slots = new ArrayList<>();
    private final TdSimulation sim;
    private double coins = RoundRules.START_COINS;
    private int next;
    private final List<Step> plan;
    private long spent;
    final java.util.Map<String, Integer> leaks = new java.util.TreeMap<>();
    int shots;
    int hits;

    private final List<double[]> points = new ArrayList<>();
    private final java.util.Set<Long> onPath = new java.util.HashSet<>();

    /** A layout of the game: the route of cells; towers go on the free spot that covers most of it with their range. */
    BalanceSimulator(List<int[]> route, List<Step> plan, int slotCount) {
        this.plan = plan;
        for (int[] cell : route) {
            points.add(new double[]{cell[0] + 0.5, 0, cell[1] + 0.5});
            onPath.add(cell[0] * 1000L + cell[1]);
        }
        sim = new TdSimulation(new TdPath(points));
        sim.setExternalSpeed(1.1);
        sim.onLeak(enemy -> leaks.merge(enemy.def().id(), 1, Integer::sum));
        for (int i = 0; i < slotCount; i++) {
            slots.add(null);
        }
    }

    /** Largest discount of the command posts that reach the slot (a new tower is assumed to be built where the best spot is: not known yet). */
    private double discountNear(Slot target) {
        double best = 0;
        for (Slot other : slots) {
            if (other == null || other.unit == null || !other.id.equals("command_post")) {
                continue;
            }
            TowerProfile profile = other.unit.profile();
            if (target == null || Math.hypot(other.x - target.x, other.z - target.z) <= profile.range) {
                best = Math.max(best, profile.number("discount"));
            }
        }
        return best;
    }

    /** What the auras of the other towers give a tower (as {@link de.craftorio.defense.LevelRun#aurasFor}). */
    private Buffs aurasFor(Slot target) {
        Buffs total = Buffs.NONE;
        for (Slot other : slots) {
            if (other == null || other.unit == null) {
                continue;
            }
            for (TowerProfile.Aura aura : other.unit.profile().auras) {
                if (other == target && !aura.self() || !aura.appliesTo(target.id)) {
                    continue;
                }
                double reach = aura.radius() == 0 ? other.unit.profile().range : aura.radius();
                if (reach < 0 || Math.hypot(other.x - target.x, other.z - target.z) <= reach) {
                    total = total.merge(aura.buff());
                }
            }
        }
        return total;
    }

    /** The free spot (not on the path, 2 blocks from other towers) with the most path within {@code range}; unlimited range takes the worst. */
    private Slot spot(double range) {
        Slot best = null;
        double bestScore = 0;
        boolean worst = range <= 0;
        double radius = worst ? 3 : range;
        for (int x = 0; x < de.craftorio.defense.arena.ArenaLayout.SIZE; x++) {
            for (int z = 0; z < de.craftorio.defense.arena.ArenaLayout.SIZE; z++) {
                if (onPath.contains(x * 1000L + z)) {
                    continue;
                }
                boolean crowded = false;
                for (Slot other : slots) {
                    crowded |= other != null && Math.hypot(other.x - x - 0.5, other.z - z - 0.5) < 2;
                }
                if (crowded) {
                    continue;
                }
                double covered = 0;
                for (double[] point : points) {
                    if (Math.hypot(point[0] - x - 0.5, point[2] - z - 0.5) <= radius) {
                        covered++;
                    }
                }
                if (worst) {
                    covered = covered > 0 ? 1000 - covered : 0;
                }
                if (covered > bestScore) {
                    bestScore = covered;
                    best = new Slot(x + 0.5, z + 0.5);
                }
            }
        }
        return best;
    }

    String describeSlots() {
        StringBuilder sb = new StringBuilder();
        for (Slot slot : slots) {
            if (slot == null) {
                continue;
            }
            sb.append(String.format("(%.1f,%.1f) %s%n", slot.x, slot.z, slot.id));
        }
        return sb.toString();
    }

    /** Gives the team coins (what-if experiments: how strong must a defence be at all?). */
    void addCoins(double extra) {
        coins += extra;
    }

    long spent() {
        return spent;
    }

    private void buy() {
        while (next < plan.size()) {
            Step step = plan.get(next);
            Slot slot = slots.get(step.slot());
            if (slot == null && step.path() >= 0) {
                throw new IllegalStateException("no tower in slot " + step.slot());
            }
            TowerDef def = TowerDefs.get(step.tower() != null ? step.tower() : slot.id);
            int tier = step.path() < 0 ? 0 : slot.tiers[step.path()] + 1;
            long price = step.path() < 0 ? def.cost() : def.upgrade(step.path(), tier).cost();
            if (tier <= 3) {
                price = TowerRules.discounted(price, step.path() < 0 ? discountNear(null) : discountNear(slot));
            }
            if (price > coins) {
                return;
            }
            coins -= price;
            spent += price;
            if (step.path() < 0) {
                slot = spot(def.range());
                slots.set(step.slot(), slot);
                slot.id = step.tower();
                slot.unit = new TowerUnit(def, slot.x, 0, slot.z, step.slot());
            } else {
                slot.tiers[step.path()]++;
                slot.unit.setTiers(slot.tiers.clone());
            }
            next++;
        }
    }

    /** Plays rounds {@code from} to {@code to}; stops at the end of the first round that leaks when {@code stopAtLeak}. */
    Result play(int from, int to, boolean stopAtLeak) {
        int leaked = 0;
        int first = -1;
        for (int round = from; round <= to; round++) {
            buy();
            RoundDef def = RoundDefs.get(round);
            sim.setRound(round);
            List<RoundDef.Spawn> spawns = def.spawns();
            int index = 0;
            int tick = 0;
            int idle = 0;
            while (index < spawns.size() || sim.count() > 0) {
                while (index < spawns.size() && spawns.get(index).tick() <= tick) {
                    RoundDef.Group group = spawns.get(index++).group();
                    sim.spawn(group.enemy(), group.camo(), group.regrow(), group.fortified());
                }
                sim.tick();
                for (Slot slot : slots) {
                    if (slot != null && slot.unit != null) {
                        abilities(slot);
                        if (tick % 10 == 0) {
                            slot.outside = aurasFor(slot);
                        }
                        for (var shot : slot.unit.tick(sim, TowerUnit.Supply.FREE, 1, slot.outside)) {
                            shots++;
                            hits += shot.hits().size();
                        }
                        coins += slot.unit.takeCoins();
                    }
                }
                coins += sim.takeCoins();
                int lost = sim.takeLeaked();
                if (lost > 0) {
                    leaked += lost;
                    if (first < 0) {
                        first = round;
                    }
                }
                tick++;
                if (tick > 20 * 60 * 20) {
                    throw new IllegalStateException("round " + round + " does not end");
                }
            }
            coins += RoundRules.roundBonus(round);
            for (Slot slot : slots) {
                if (slot != null && slot.unit != null && slot.id.equals("supply_depot")) {
                    coins += slot.depot.roundEnd(slot.unit.profile(), false);
                }
                if (slot != null && slot.unit != null && slot.id.equals("command_post")) {
                    coins += slot.unit.profile().number("income");
                }
            }
            if (first >= 0 && stopAtLeak) {
                return new Result(first, leaked, (long) coins, round);
            }
        }
        return new Result(first, leaked, (long) coins, to);
    }

    /** A player uses assassins and similar abilities when there is something to hit: an attack that only hits MOAB-class enemies needs one on the path. */
    private boolean worthUsing(TowerUnit unit, TowerProfile.Ability ability) {
        for (Attack attack : unit.profile().attacks) {
            if (attack.abilityId.equals(ability.id()) && attack.bossOnly) {
                return sim.enemies().stream().anyMatch(enemy -> enemy.def().boss());
            }
        }
        return true;
    }

    private void abilities(Slot slot) {
        TowerUnit unit = slot.unit;
        for (int i = 0; i < unit.abilityCount(); i++) {
            if (!unit.profile().abilities.get(i).passive() && sim.count() > 0 && worthUsing(unit, unit.profile().abilities.get(i))) {
                TowerProfile.Ability used = unit.activate(i, 1);
                if (used != null) {
                    if (used.buff() != null) {
                        unit.grantBuff("ability:" + used.id(), (int) Math.round(used.duration() * TdUnits.TICKS_PER_SECOND), used.buff());
                    }
                    switch (used.id()) {
                        case "supply_drop" -> coins += unit.profile().number("supply_drop");
                        case "bank_withdraw" -> coins += slot.depot.withdraw();
                        case "imf_loan", "monkey_nomics" -> coins += slot.depot.loan(unit.profile());
                        default -> {
                        }
                    }
                }
            }
        }
    }
}
