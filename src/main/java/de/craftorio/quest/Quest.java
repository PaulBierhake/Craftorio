package de.craftorio.quest;

import java.util.Map;
import java.util.Set;

/**
 * One step of the guide: a goal measured on the team's progress and a credit reward that can be collected once
 * the goal is reached. Pure data so the guide can be unit tested.
 *
 * @param target item id for {@link Kind#SELL}, blueprint id for {@link Kind#UNLOCK} and {@link Kind#BUILD}, unused otherwise
 */
public record Quest(String id, Kind kind, String target, long amount, long reward) {
    public enum Kind {
        /** Total credits earned. */
        EARN,
        /** Items of {@code target} sold. */
        SELL,
        /** Blueprint {@code target} bought. */
        UNLOCK,
        /** Blueprint {@code target} built at a workbench (times). */
        BUILD,
        /** Tower defense levels completed. */
        TD_LEVEL
    }

    /** What the guide measures. */
    public record Progress(long totalEarned, Map<String, Long> sold, Set<String> unlocked, Map<String, Long> built, int tdLevelsCompleted) {
    }

    public long progress(Progress progress) {
        long value = switch (kind) {
            case EARN -> progress.totalEarned();
            case SELL -> progress.sold().getOrDefault(target, 0L);
            case UNLOCK -> progress.unlocked().contains(target) ? 1 : 0;
            case BUILD -> progress.built().getOrDefault(target, 0L);
            case TD_LEVEL -> progress.tdLevelsCompleted();
        };
        return Math.min(value, amount);
    }

    public boolean done(Progress progress) {
        return progress(progress) >= amount;
    }
}
