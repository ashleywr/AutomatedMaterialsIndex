package com.sanhiruzu.ami.index.providers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreativeStackVariantExpanderTest {
    @Test
    void tooltipResourceDetectionReadsStoredAmountsNotItemNames() {
        assertTrue(CreativeStackVariantExpander.tooltipIndicatesPositiveStoredResource(List.of(
                "Energy: 80,000 / 80,000 FE"
        )));
        assertTrue(CreativeStackVariantExpander.tooltipIndicatesPositiveStoredResource(List.of(
                "Fluid: Lava 1000 mB"
        )));

        assertFalse(CreativeStackVariantExpander.tooltipIndicatesPositiveStoredResource(List.of(
                "Energy: 0 / 80,000 FE"
        )));
        assertFalse(CreativeStackVariantExpander.tooltipIndicatesPositiveStoredResource(List.of(
                "Capacity: 80,000 FE"
        )));
        assertFalse(CreativeStackVariantExpander.tooltipIndicatesPositiveStoredResource(List.of(
                "Advanced Energy Cube"
        )));
    }
}
