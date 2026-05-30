package com.sanhiruzu.ami.client.tooltip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AmiTooltipRendererPositionTest {
    @Test
    void rightPreferredTooltipMovesLeftWhenRightEdgeWouldOverlapCursor() {
        assertEquals(38, TooltipPositioning.chooseX(170, 150, 100, false));
    }

    @Test
    void leftPreferredTooltipMovesRightWhenLeftSideDoesNotFit() {
        assertEquals(32, TooltipPositioning.chooseX(170, 20, 100, true));
    }

    @Test
    void clampsOnScreenWhenNeitherSideFits() {
        assertEquals(16, TooltipPositioning.chooseX(170, 85, 150, false));
    }
}
