package com.sanhiruzu.ami.index.resolvers;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves numeric UQL filters such as >dps:10 and <storage:4096.
 */
public final class NumericMetadataResolver {
    private final List<SearchNode> nodes = new ArrayList<>();

    public void addNode(SearchNode node) {
        nodes.add(node);
    }

    public Map<NodeType, List<SearchNode>> resolve(String token) {
        Optional<NumericFilter> parsed = NumericFilter.parse(token);
        if (parsed.isEmpty()) return new LinkedHashMap<>();

        NumericFilter filter = parsed.get();
        Map<NodeType, List<SearchNode>> result = new LinkedHashMap<>();
        for (SearchNode node : nodes) {
            double value = parseDouble(node.meta(filter.metadataKey(), ""));
            if (Double.isNaN(value) || !filter.matches(value)) continue;
            result.computeIfAbsent(node.type(), ignored -> new ArrayList<>()).add(node);
        }
        return result;
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) return Double.NaN;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    private record NumericFilter(char operator, String metadataKey, double threshold) {
        private static Optional<NumericFilter> parse(String rawToken) {
            if (rawToken == null || rawToken.isBlank()) return Optional.empty();

            String token = rawToken.trim();
            char operator = token.charAt(0);
            if (operator != '>' && operator != '<' && operator != '=') {
                return Optional.empty();
            }

            String body = token.substring(1).trim();
            if (body.isEmpty()) return Optional.empty();

            String field = "storage";
            String number = body;
            int sep = body.indexOf(':');
            if (sep >= 0) {
                field = body.substring(0, sep).trim();
                number = body.substring(sep + 1).trim();
            }

            String metadataKey = metadataKeyFor(field);
            if (metadataKey == null || number.isEmpty()) return Optional.empty();

            try {
                return Optional.of(new NumericFilter(operator, metadataKey, Double.parseDouble(number)));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }

        private static String metadataKeyFor(String field) {
            String normalized = field.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
            return switch (normalized) {
                case "dps", "damage", "damagepersecond" -> SearchNodeKeys.DPS;
                case "storage", "capacity", "esm", "items" -> SearchNodeKeys.ESM_CAPACITY;
                default -> null;
            };
        }

        private boolean matches(double value) {
            return switch (operator) {
                case '>' -> value >= threshold;
                case '<' -> value <= threshold;
                case '=' -> Math.abs(value - threshold) < 0.0001D;
                default -> false;
            };
        }
    }
}
