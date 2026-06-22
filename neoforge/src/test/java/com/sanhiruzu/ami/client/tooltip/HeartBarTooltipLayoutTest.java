package com.sanhiruzu.ami.client.tooltip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartBarTooltipLayoutTest {
    @Test
    void computesHeartCountsAndLabelsForEvenHealth() {
        HeartBarTooltipLayout layout = new HeartBarTooltipLayout(20);

        assertEquals(20, layout.shownHalfHearts());
        assertEquals(10, layout.heartCount());
        assertEquals("ami.tooltip.heart.even", layout.healthLabelKey());
        assertEquals(10, layout.healthLabelHearts());
        assertFalse(layout.hasOverflow());
    }

    @Test
    void computesHalfHeartLabelForOddHealth() {
        HeartBarTooltipLayout layout = new HeartBarTooltipLayout(7);

        assertEquals(7, layout.shownHalfHearts());
        assertEquals(4, layout.heartCount());
        assertEquals("ami.tooltip.heart.odd", layout.healthLabelKey());
        assertEquals(3, layout.healthLabelHearts());
    }

    @Test
    void capsRenderedHeartsAndReportsOverflow() {
        HeartBarTooltipLayout layout = new HeartBarTooltipLayout(34);

        assertEquals(20, layout.shownHalfHearts());
        assertEquals(10, layout.heartCount());
        assertTrue(layout.hasOverflow());
        assertEquals(7, layout.overflowHearts());
        assertEquals(102, layout.overflowLabelXOffset());
    }
}
