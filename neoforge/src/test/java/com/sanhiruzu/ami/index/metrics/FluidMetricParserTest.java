package com.sanhiruzu.ami.index.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidMetricParserTest {
    @Test
    void parsesMillibucketCapacityAsVanillaBucketEquivalent() {
        FluidStats stats = FluidMetricParser.parseTooltip(List.of("Capacity: 16,000 mB")).orElseThrow();

        assertEquals(16.0D, stats.buckets(), 0.0001D);
    }

    @Test
    void ignoresBareFluidAmountsWithoutCapacityContext() {
        assertTrue(FluidMetricParser.parseTooltip(List.of("Contains: Water")).isEmpty());
    }
}
