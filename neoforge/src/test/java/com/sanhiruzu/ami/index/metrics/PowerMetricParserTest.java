package com.sanhiruzu.ami.index.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerMetricParserTest {
    @Test
    void parsesExplicitGenerationRateFromTooltipText() {
        PowerStats stats = PowerMetricParser.parseTooltip(
                List.of("Output: 120 FE/t", "Stored Energy: 0 / 100,000 FE"),
                "example:generator Basic Generator"
        ).orElseThrow();

        assertEquals(120.0D, stats.generationFePerTick(), 0.0001D);
        assertTrue(stats.hasGeneration());
    }

    @Test
    void treatsBareFeRateAsGenerationOnlyForGeneratorItems() {
        assertTrue(PowerMetricParser.parseTooltip(
                List.of("40 FE/t"),
                "example:basic_generator Basic Generator"
        ).orElseThrow().hasGeneration());

        assertTrue(PowerMetricParser.parseTooltip(
                List.of("40 FE/t"),
                "example:energy_cable Energy Cable"
        ).isEmpty());
    }

    @Test
    void parsesConsumptionSeparatelyFromGeneration() {
        PowerStats stats = PowerMetricParser.parseTooltip(
                List.of("Consumes 20 RF/t", "Max Output: 80 RF/t"),
                "example:machine Powered Machine"
        ).orElseThrow();

        assertEquals(80.0D, stats.generationFePerTick(), 0.0001D);
        assertEquals(20.0D, stats.consumptionFePerTick(), 0.0001D);
    }
}
