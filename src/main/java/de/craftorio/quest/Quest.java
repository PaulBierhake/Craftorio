package de.craftorio.quest;

import java.util.Map;
import java.util.Set;

/**
 * One step of the guide: a goal measured on the team's progress and a credit reward that can be collected once
 * the goal is reached. Pure data so the guide can be unit tested.
 *
 * @param rewardItem optional item paid out with the credits (id, empty for none), {@code rewardCount} of it
 * @param target item id for {@link Kind#SELL} and {@link Kind#MINE}, blueprint id for {@link Kind#UNLOCK} and {@link Kind#BUILD}, unused otherwise
 */
public record Quest(String id, Kind kind, String target, long amount, long reward, String rewardItem, int rewardCount) {
    public Quest(String id, Kind kind, String target, long amount, long reward) {
        this(id, kind, target, amount, reward, "", 0);
    }

    /** The same goal with an item as (additional) reward: {@code rewardItem} is an item id such as {@code craftorio:iron_gear}. */
    public Quest withItem(String item, int count) {
        return new Quest(id, kind, target, amount, reward, item, count);
    }

    public boolean hasRewardItem() {
        return !rewardItem.isEmpty() && rewardCount > 0;
    }

    /** Hand-mined resources are counted in the team's build statistics under this prefix plus the item id. */
    public static final String MINED_PREFIX = "mined:";

    public enum Kind {
        /** Total credits earned. */
        EARN,
        /** Items of {@code target} mined by hand from ore fields. */
        MINE,
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
            case MINE -> progress.built().getOrDefault(MINED_PREFIX + target, 0L);
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
