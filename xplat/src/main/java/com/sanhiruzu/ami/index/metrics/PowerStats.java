package com.sanhiruzu.ami.index.metrics;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public record PowerStats(
        @Nullable Integer capacityFe,
        @Nullable Double generationFePerTick,
        @Nullable Double consumptionFePerTick,
        String source
) {
    public static PowerStats empty() {
        return new PowerStats(null, null, null, "");
    }

    public boolean hasAny() {
        return capacityFe != null || generationFePerTick != null || consumptionFePerTick != null;
    }

    public boolean hasCapacity() {
        return capacityFe != null && capacityFe > 0;
    }

    public boolean hasGeneration() {
        return generationFePerTick != null && generationFePerTick > 0.0D;
    }

    public boolean hasConsumption() {
        return consumptionFePerTick != null && consumptionFePerTick > 0.0D;
    }

    public PowerStats merge(PowerStats other) {
        if (other == null || !other.hasAny()) {
            return this;
        }
        if (!hasAny()) {
            return other;
        }
        return new PowerStats(
                maxPositive(capacityFe, other.capacityFe),
                maxPositive(generationFePerTick, other.generationFePerTick),
                maxPositive(consumptionFePerTick, other.consumptionFePerTick),
                mergeSources(source, other.source)
        );
    }

    @Nullable
    private static Integer maxPositive(@Nullable Integer first, @Nullable Integer second) {
        if (first == null || first <= 0) return second;
        if (second == null || second <= 0) return first;
        return Math.max(first, second);
    }

    @Nullable
    private static Double maxPositive(@Nullable Double first, @Nullable Double second) {
        if (first == null || first <= 0.0D) return second;
        if (second == null || second <= 0.0D) return first;
        return Math.max(first, second);
    }

    private static String mergeSources(String first, String second) {
        Set<String> sources = new LinkedHashSet<>();
        addSources(sources, first);
        addSources(sources, second);
        return String.join(",", sources);
    }

    private static void addSources(Set<String> sources, String raw) {
        if (raw == null || raw.isBlank()) return;
        for (String part : raw.split(",")) {
            String source = part.trim();
            if (!source.isBlank()) sources.add(source);
        }
    }
}
