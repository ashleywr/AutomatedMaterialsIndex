package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

final class CategoryRouteTrace {
    private final StringJoiner steps = new StringJoiner(" -> ");

    private CategoryRouteTrace(ResourceLocation id, String modFamily, Map<String, String> attributes) {
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
    }

    static CategoryRouteTrace start(ResourceLocation id, String modFamily, Map<String, String> attributes) {
        return new CategoryRouteTrace(id, modFamily, attributes == null ? Map.of() : attributes);
    }

    CategoryAssignment finish(String phase, String ruleId, CategoryAssignment assignment) {
        CategoryRouteDecision decision = new CategoryRouteDecision(
                phase,
                ruleId,
                assignment.categoryId(),
                assignment.subcategoryId()
        );
        steps.add(decision.label());

        Map<String, String> attributes = new LinkedHashMap<>(assignment.attributes());
        attributes.put(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE, decision.phase());
        attributes.put(SearchNodeKeys.CLASSIFICATION_ROUTE_RULE, decision.ruleId());
        attributes.put(SearchNodeKeys.CLASSIFICATION_ROUTE, steps.toString());
        return new CategoryAssignment(assignment.categoryId(), assignment.subcategoryId(), attributes);
    }

    private static void appendAttribute(StringBuilder builder, String label, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(" ").append(label).append("=").append(value);
        }
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
