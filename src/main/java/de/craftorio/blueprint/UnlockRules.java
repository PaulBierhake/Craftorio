package de.craftorio.blueprint;

import java.util.Collection;
import java.util.Set;

/** Whether a team may unlock a blueprint right now. Pure logic so the rules can be unit tested. */
public final class UnlockRules {
    private UnlockRules() {
    }

    public enum Status {
        /** Free starter blueprint or already bought. */
        UNLOCKED,
        MISSING_PREREQUISITE,
        MISSING_KEY_ITEMS,
        NOT_ENOUGH_CREDITS,
        AVAILABLE
    }

    public static Status check(String id, boolean free, Set<String> unlocked, Collection<String> requires,
                               long balance, long cost, boolean hasKeyItems) {
        if (free || unlocked.contains(id)) {
            return Status.UNLOCKED;
        }
        for (String required : requires) {
            if (!unlocked.contains(required)) {
                return Status.MISSING_PREREQUISITE;
            }
        }
        if (!hasKeyItems) {
            return Status.MISSING_KEY_ITEMS;
        }
        if (balance < cost) {
            return Status.NOT_ENOUGH_CREDITS;
        }
        return Status.AVAILABLE;
    }
}
