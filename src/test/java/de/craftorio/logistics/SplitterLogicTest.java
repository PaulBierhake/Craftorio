package de.craftorio.logistics;

import de.craftorio.logistics.SplitterLogic.Output;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SplitterLogicTest {
    private static final EnumSet<Output> ALL = EnumSet.allOf(Output.class);

    @Test
    void itemsAlternateBetweenAllConnectedOutputs() {
        assertEquals(List.of(Output.FRONT, Output.LEFT, Output.RIGHT), SplitterLogic.order(ALL, false, false, Output.FRONT, 0));
        assertEquals(List.of(Output.LEFT, Output.RIGHT, Output.FRONT), SplitterLogic.order(ALL, false, false, Output.FRONT, 1));
        assertEquals(List.of(Output.RIGHT, Output.FRONT, Output.LEFT), SplitterLogic.order(ALL, false, false, Output.FRONT, 2));
    }

    @Test
    void anOutputWithNothingConnectedIsNeverOffered() {
        EnumSet<Output> sides = EnumSet.of(Output.LEFT, Output.RIGHT);
        // Six items with nothing in front: three to each side.
        int left = 0;
        int right = 0;
        for (int item = 0; item < 6; item++) {
            Output first = SplitterLogic.order(sides, false, false, Output.FRONT, item).get(0);
            if (first == Output.LEFT) {
                left++;
            } else {
                right++;
            }
        }
        assertEquals(3, left);
        assertEquals(3, right);
        assertEquals(List.of(), SplitterLogic.order(EnumSet.noneOf(Output.class), false, false, Output.FRONT, 0));
    }

    @Test
    void aFilterSendsMatchesWhereChosenAndAlternatesTheRest() {
        assertEquals(List.of(Output.LEFT), SplitterLogic.order(ALL, true, true, Output.LEFT, 5));
        assertEquals(List.of(Output.FRONT, Output.RIGHT), SplitterLogic.order(ALL, true, false, Output.LEFT, 0));
        assertEquals(List.of(Output.RIGHT, Output.FRONT), SplitterLogic.order(ALL, true, false, Output.LEFT, 1));
    }

    @Test
    void nothingJamsWhenTheChosenOutputIsMissing() {
        EnumSet<Output> noFront = EnumSet.of(Output.LEFT, Output.RIGHT);
        assertEquals(List.of(Output.LEFT, Output.RIGHT), SplitterLogic.order(noFront, true, true, Output.FRONT, 0), "matches fall back");
        assertEquals(List.of(Output.LEFT), SplitterLogic.order(EnumSet.of(Output.LEFT), true, false, Output.LEFT, 0), "rest falls back to the only output");
    }
}
