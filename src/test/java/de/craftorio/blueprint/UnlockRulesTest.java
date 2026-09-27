package de.craftorio.blueprint;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static de.craftorio.blueprint.UnlockRules.Status.AVAILABLE;
import static de.craftorio.blueprint.UnlockRules.Status.MISSING_KEY_ITEMS;
import static de.craftorio.blueprint.UnlockRules.Status.MISSING_PREREQUISITE;
import static de.craftorio.blueprint.UnlockRules.Status.NOT_ENOUGH_CREDITS;
import static de.craftorio.blueprint.UnlockRules.Status.UNLOCKED;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UnlockRulesTest {
    @Test
    void freeAndBoughtBlueprintsAreUnlocked() {
        assertEquals(UNLOCKED, UnlockRules.check("belt", true, Set.of(), List.of(), 0, 0, true));
        assertEquals(UNLOCKED, UnlockRules.check("press", false, Set.of("press"), List.of("generator"), 0, 400, false));
    }

    @Test
    void checksPrerequisitesThenKeyItemsThenCredits() {
        assertEquals(MISSING_PREREQUISITE, UnlockRules.check("press", false, Set.of(), List.of("generator"), 1000, 400, true));
        assertEquals(MISSING_KEY_ITEMS, UnlockRules.check("upgrade", false, Set.of("press"), List.of("press"), 5000, 2000, false));
        assertEquals(NOT_ENOUGH_CREDITS, UnlockRules.check("press", false, Set.of("generator"), List.of("generator"), 399, 400, true));
        assertEquals(AVAILABLE, UnlockRules.check("press", false, Set.of("generator"), List.of("generator"), 400, 400, true));
    }
}
