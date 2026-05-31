package com.sanhiruzu.ami.index.metrics;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PowerMetricParser {
    private static final Pattern RATE_PATTERN = Pattern.compile(
            "(?i)([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([kmg])?\\s*(?:fe|rf)\\s*(?:/\\s*(?:t|tick|ticks)|per\\s*tick)"
    );

    private PowerMetricParser() {
    }

    public static Optional<PowerStats> parseTooltip(Collection<String> tooltipLines, String identity) {
        if (tooltipLines == null || tooltipLines.isEmpty()) {
            return Optional.empty();
        }

        Double generation = null;
        Double consumption = null;

        for (String line : tooltipLines) {
            if (line == null || line.isBlank()) continue;
            String normalizedLine = normalize(line);
            Matcher matcher = RATE_PATTERN.matcher(normalizedLine);
            while (matcher.find()) {
                double value = parseRate(matcher.group(1), matcher.group(2));
                if (value <= 0.0D) continue;

                if (isConsumptionLine(normalizedLine)) {
                    consumption = max(consumption, value);
                } else if (isGenerationLine(normalizedLine)) {
                    generation = max(generation, value);
                }
            }
        }

        PowerStats stats = new PowerStats(null, generation, consumption, "tooltip");
        return stats.hasAny() ? Optional.of(stats) : Optional.empty();
    }

    private static double parseRate(String rawNumber, String suffix) {
        double value = Double.parseDouble(rawNumber.replace(",", ""));
        if (suffix == null || suffix.isBlank()) {
            return value;
        }
        return switch (suffix.toLowerCase(Locale.ROOT)) {
            case "k" -> value * 1_000.0D;
            case "m" -> value * 1_000_000.0D;
            case "g" -> value * 1_000_000_000.0D;
            default -> value;
        };
    }

    private static boolean isGenerationLine(String line) {
        return containsAny(line,
                "generate", "generates", "generator", "generation",
                "produce", "produces", "production",
                "output", "outputs", "max output", "energy out", "power out"
        ) && !containsAny(line, "transfer", "receive", "input", "consume", "consumes", "consumption", "cost", "drain", "usage");
    }

    private static boolean isConsumptionLine(String line) {
        return containsAny(line,
                "consume", "consumes", "consumption",
                "use ", "uses ", "usage", "cost", "input", "drain", "requires", "required"
        ) && !containsAny(line, "output", "generate", "generates", "generation", "produce", "produces");
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static Double max(Double current, double value) {
        return current == null ? value : Math.max(current, value);
    }
}
