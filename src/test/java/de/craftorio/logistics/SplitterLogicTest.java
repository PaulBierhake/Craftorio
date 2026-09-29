package de.craftorio.logistics;

import de.craftorio.logistics.SplitterLogic.Output;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SplitterLogicTest {
    @Test
    void itemsAlternateBetweenAllOutputs() {
        assertEquals(List.of(Output.FRONT, Output.LEFT, Output.RIGHT), SplitterLogic.order(false, false, null, 0));
        assertEquals(List.of(Output.LEFT, Output.RIGHT, Output.FRONT), SplitterLogic.order(false, false, null, 1));
        assertEquals(List.of(Output.RIGHT, Output.FRONT, Output.LEFT), SplitterLogic.order(false, false, null, 2));
        assertEquals(List.of(Output.FRONT, Output.LEFT, Output.RIGHT), SplitterLogic.order(false, false, null, 3));
    }

    @Test
    void aPriorityOutputIsTriedFirstAndTheOthersFollow() {
        assertEquals(List.of(Output.LEFT, Output.FRONT, Output.RIGHT), SplitterLogic.order(false, false, Output.LEFT, 0));
        assertEquals(Output.RIGHT, SplitterLogic.order(false, false, Output.RIGHT, 5).get(0));
        assertEquals(3, SplitterLogic.order(false, false, Output.RIGHT, 5).size());
    }

    @Test
    void aFilterSendsMatchesForwardAndAlternatesTheRest() {
        assertEquals(List.of(Output.FRONT), SplitterLogic.order(true, true, null, 7));
        assertEquals(List.of(Output.LEFT, Output.RIGHT), SplitterLogic.order(true, false, null, 0));
        assertEquals(List.of(Output.RIGHT, Output.LEFT), SplitterLogic.order(true, false, null, 1));
        assertEquals(List.of(Output.FRONT), SplitterLogic.order(true, true, Output.LEFT, 0), "a filter beats the priority");
    }
}
