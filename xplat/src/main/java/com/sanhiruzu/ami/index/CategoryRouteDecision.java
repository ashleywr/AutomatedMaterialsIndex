package com.sanhiruzu.ami.index;

public record CategoryRouteDecision(
        String phase,
        String ruleId,
        String categoryId,
        String subcategoryId
) {
    public CategoryRouteDecision {
        phase = normalize(phase, "unknown");
        ruleId = normalize(ruleId, "");
        categoryId = normalize(categoryId, "misc");
        subcategoryId = normalize(subcategoryId, "unknown");
    }

    String label() {
        String rule = ruleId.isBlank() ? phase : phase + ":" + ruleId;
        return rule + "[" + categoryId + "/" + subcategoryId + "]";
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
