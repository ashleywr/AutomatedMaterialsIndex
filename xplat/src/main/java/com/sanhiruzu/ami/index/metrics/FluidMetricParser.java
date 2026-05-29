package com.sanhiruzu.ami.index.metrics;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FluidMetricParser {
    private static final Pattern FLUID_PATTERN = Pattern.compile(
            "(?i)([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*(m?b|buckets?|liters?|l)\\b"
    );

    private FluidMetricParser() {
    }

    public static Optional<FluidStats> parseTooltip(Collection<String> tooltipLines) {
        if (tooltipLines == null || tooltipLines.isEmpty()) {
            return Optional.empty();
        }

        double bestBuckets = 0.0D;
        for (String line : tooltipLines) {
            if (line == null || line.isBlank()) continue;
            String normalized = line.toLowerCase(Locale.ROOT);
            if (!isCapacityLine(normalized)) continue;

            Matcher matcher = FLUID_PATTERN.matcher(normalized);
            while (matcher.find()) {
                double buckets = toBuckets(matcher.group(1), matcher.group(2));
                bestBuckets = Math.max(bestBuckets, buckets);
            }
        }

        return bestBuckets > 0.0D ? Optional.of(new FluidStats(bestBuckets, "tooltip")) : Optional.empty();
    }

    private static boolean isCapacityLine(String line) {
        return line.contains("capacity")
                || line.contains("stores")
                || line.contains("holds")
                || line.contains("tank")
                || line.contains("fluid")
                || line.contains("bucket")
                || line.contains("mb");
    }

    private static double toBuckets(String rawNumber, String rawUnit) {
        double value = Double.parseDouble(rawNumber.replace(",", ""));
        String unit = rawUnit.toLowerCase(Locale.ROOT);
        if (unit.equals("mb")) {
            return value / 1000.0D;
        }
        return value;
    }
}
