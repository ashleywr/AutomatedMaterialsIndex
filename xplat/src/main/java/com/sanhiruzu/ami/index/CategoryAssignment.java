package com.sanhiruzu.ami.index;

import java.util.LinkedHashMap;
import java.util.Map;

public record CategoryAssignment(
        String categoryId,
        String subcategoryId,
        Map<String, String> attributes
) {
    public CategoryAssignment {
        attributes = attributes == null || attributes.isEmpty()
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(attributes));
    }
}
