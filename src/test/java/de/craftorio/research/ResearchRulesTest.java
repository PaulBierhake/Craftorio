package de.craftorio.research;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchRulesTest {
    @Test
    void statusFollowsResearchedAndQueue() {
        Set<String> done = Set.of("a");
        List<String> queue = List.of("b", "c");

        assertEquals(ResearchRules.Status.DONE, ResearchRules.status("a", List.of(), done, queue));
        assertEquals(ResearchRules.Status.ACTIVE, ResearchRules.status("b", List.of("a"), done, queue));
        assertEquals(ResearchRules.Status.QUEUED, ResearchRules.status("c", List.of("b"), done, queue));
        assertEquals(ResearchRules.Status.AVAILABLE, ResearchRules.status("d", List.of("a"), done, queue));
        assertEquals(ResearchRules.Status.LOCKED, ResearchRules.status("e", List.of("d"), done, queue));
    }

    @Test
    void chainsMayBeQueuedInOrder() {
        Set<String> done = Set.of("a");
        assertTrue(ResearchRules.canQueue("b", List.of("a"), done, List.of()));
        assertFalse(ResearchRules.canQueue("c", List.of("b"), done, List.of()), "prerequisite neither done nor queued");
        assertTrue(ResearchRules.canQueue("c", List.of("b"), done, List.of("b")));
        assertFalse(ResearchRules.canQueue("b", List.of("a"), done, List.of("b")), "already queued");
        assertFalse(ResearchRules.canQueue("a", List.of(), done, List.of()), "already researched");
    }

    @Test
    void removingAResearchDropsWhatDependedOnIt() {
        Map<String, List<String>> requires = Map.of("b", List.of("a"), "c", List.of("b"), "d", List.of("x"));
        List<String> kept = ResearchRules.pruneQueue(List.of("c", "d"), id -> requires.getOrDefault(id, List.of()), Set.of("x"));

        assertEquals(List.of("d"), kept, "c lost b, d only needs the finished x");
    }
}
