package com.sanhiruzu.ami.index;

public record ClassificationEvidence(
        String id,
        String source,
        String categoryId,
        String subcategoryId,
        int weight,
        String reason
) {
    public ClassificationEvidence {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Evidence id must not be blank");
        }
        if (source == null || source.isBlank()) {
            source = "unknown";
        }
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("Evidence category must not be blank");
        }
        if (subcategoryId == null) {
            subcategoryId = "";
        }
        if (reason == null) {
            reason = "";
        }
    }

    public String categoryKey() {
        return categoryId + "/" + subcategoryId;
    }
}
