package de.craftorio.research;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Where a research stands for a team. Pure logic so the rules can be unit tested. */
public final class ResearchRules {
    private ResearchRules() {
    }

    public enum Status {
        DONE,
        /** First in the queue: the labs work on it. */
        ACTIVE,
        QUEUED,
        /** All prerequisites are finished; it can be queued. */
        AVAILABLE,
        LOCKED
    }

    /** Every finished mining productivity research adds 10 % ore to every drill of the team (wiki 1.1: levels 1 to 3). */
    public static double miningProductivity(Collection<String> researched) {
        int levels = 0;
        for (int level = 1; level <= 3; level++) {
            if (researched.contains("craftorio:mining_productivity_" + level)) {
                levels++;
            }
        }
        return 0.1 * levels;
    }

    public static Status status(String id, Collection<String> requires, Set<String> researched, List<String> queue) {
        if (researched.contains(id)) {
            return Status.DONE;
        }
        int position = queue.indexOf(id);
        if (position == 0) {
            return Status.ACTIVE;
        }
        if (position > 0) {
            return Status.QUEUED;
        }
        return requires.stream().allMatch(researched::contains) ? Status.AVAILABLE : Status.LOCKED;
    }

    /**
     * May the research be added to the queue? Its prerequisites must be finished or queued before it, the way
     * Factorio allows queueing a chain of researches.
     */
    public static boolean canQueue(String id, Collection<String> requires, Set<String> researched, List<String> queue) {
        if (researched.contains(id) || queue.contains(id)) {
            return false;
        }
        return requires.stream().allMatch(required -> researched.contains(required) || queue.contains(required));
    }

    /** Drops queued researches whose prerequisites are neither finished nor queued before them (after a dequeue). */
    public static List<String> pruneQueue(List<String> queue, java.util.function.Function<String, Collection<String>> requires, Set<String> researched) {
        List<String> kept = new java.util.ArrayList<>();
        for (String id : queue) {
            if (requires.apply(id).stream().allMatch(required -> researched.contains(required) || kept.contains(required))) {
                kept.add(id);
            }
        }
        return kept;
    }
}
