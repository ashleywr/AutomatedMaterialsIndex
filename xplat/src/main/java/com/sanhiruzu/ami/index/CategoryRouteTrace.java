package com.sanhiruzu.ami.index;

import net.minecraft.resources.Identifier;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.Set;
import java.util.stream.Collectors;

final class CategoryRouteTrace {
    private final boolean includeTrace;
    private final StringJoiner steps = new StringJoiner(" -> ");
    private final StringJoiner trace = new StringJoiner(" | ");

    private CategoryRouteTrace(Identifier id, String modFamily, Set<ItemFacet> facets, Map<String, String> attributes) {
        includeTrace = IndexingHotItemPolicy.shouldRecordClassificationTrace();
        StringBuilder input = new StringBuilder("input[")
                .append(id == null ? "null" : id)
                .append(" modFamily=")
                .append(blankTo(attributes.get(SearchNodeKeys.MOD_ID), id == null ? "" : id.getNamespace()))
                .append("/")
                .append(blankTo(modFamily, "generic"));
        appendAttribute(input, "compat", attributes.get(SearchNodeKeys.COMPAT_FAMILIES));
        appendAttribute(input, "primary", attributes.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
        appendAttribute(input, "policy", attributes.get(SearchNodeKeys.COMPAT_CATEGORY_POLICY));
        appendAttribute(input, "kind", attributes.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        input.append("]");
        steps.add(input.toString());
        if (includeTrace) {
            trace.add(input.toString());
            appendFactSummary(facets == null ? Set.of() : facets, attributes);
        }
    }

    static CategoryRouteTrace start(Identifier id, String modFamily, Set<ItemFacet> facets, Map<String, String> attributes) {
        return new CategoryRouteTrace(id, modFamily, facets, attributes == null ? Map.of() : attributes);
    }

    void skipped(String phase, String reason) {
        if (!includeTrace) {
            return;
        }
        trace.add(normalizeStep(phase) + ": skip - " + normalizeStep(reason));
    }

    CategoryAssignment finish(String phase, String ruleId, CategoryAssignment assignment) {
        CategoryRouteDecision decision = new CategoryRouteDecision(
                phase,
                ruleId,
                assignment.categoryId(),
                assignment.subcategoryId()
        );
        steps.add(decision.label());
        if (includeTrace) {
            trace.add(decision.label() + ": matched");
        }

        Map<String, String> attributes = new LinkedHashMap<>(assignment.attributes());
        attributes.put(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE, decision.phase());
        attributes.put(SearchNodeKeys.CLASSIFICATION_ROUTE_RULE, decision.ruleId());
        attributes.put(SearchNodeKeys.CLASSIFICATION_ROUTE, steps.toString());
        if (includeTrace) {
            attributes.put(SearchNodeKeys.CLASSIFICATION_TRACE, trace.toString());
        }
        return new CategoryAssignment(assignment.categoryId(), assignment.subcategoryId(), attributes);
    }

    private void appendFactSummary(Set<ItemFacet> facets, Map<String, String> attributes) {
        StringBuilder facts = new StringBuilder("facts[");
        String facetSummary = facets.stream()
                .map(ItemFacet::id)
                .sorted(Comparator.naturalOrder())
                .limit(12)
                .collect(Collectors.joining(","));
        facts.append("facets=").append(facetSummary.isBlank() ? "-" : facetSummary);
        if (facets.size() > 12) {
            facts.append(",+").append(facets.size() - 12);
        }
        appendAttribute(facts, "shape", attributes.get("blockShape"));
        appendAttribute(facts, "blockClass", simpleClassName(attributes.get(SearchNodeKeys.BLOCK_CLASS)));
        appendAttribute(facts, "itemClass", simpleClassName(attributes.get(SearchNodeKeys.ITEM_CLASS)));
        appendAttribute(facts, "props", attributes.get(SearchNodeKeys.BLOCK_STATE_PROPERTIES));
        appendTagSignals(facts, attributes);
        facts.append("]");
        trace.add(facts.toString());
    }

    private static void appendTagSignals(StringBuilder builder, Map<String, String> attributes) {
        String tags = (attributes.getOrDefault(SearchNodeKeys.TAGS, "") + ","
                + attributes.getOrDefault(SearchNodeKeys.BLOCK_TAGS, "")).toLowerCase(Locale.ROOT);
        StringJoiner signals = new StringJoiner(",");
        if (tags.contains("minecraft:leaves")) signals.add("leaves");
        if (tags.contains("minecraft:saplings")) signals.add("saplings");
        if (tags.contains("minecraft:logs")) signals.add("logs");
        if (tags.contains("minecraft:planks")) signals.add("planks");
        if (tags.contains("minecraft:flowers")) signals.add("flowers");
        if (tags.contains("minecraft:mineable/hoe")) signals.add("hoe");
        if (tags.contains("minecraft:mineable/axe")) signals.add("axe");
        String value = signals.toString();
        if (!value.isBlank()) {
            builder.append(" tagSignals=").append(value);
        }
    }

    private static void appendAttribute(StringBuilder builder, String label, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(" ").append(label).append("=").append(value);
        }
    }

    private static String simpleClassName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int lastDot = value.lastIndexOf('.');
        return lastDot >= 0 && lastDot + 1 < value.length() ? value.substring(lastDot + 1) : value;
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String normalizeStep(String value) {
        return value == null || value.isBlank() ? "unknown" : value.replace('|', '/');
    }
}
