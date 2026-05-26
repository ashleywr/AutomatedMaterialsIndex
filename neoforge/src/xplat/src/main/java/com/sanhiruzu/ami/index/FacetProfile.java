package com.sanhiruzu.ami.index;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

public record FacetProfile(
        EnumSet<ItemFacet> facets,
        Map<String, String> attributes
) {
    public FacetProfile {
        facets = facets == null || facets.isEmpty()
                ? EnumSet.noneOf(ItemFacet.class)
                : EnumSet.copyOf(facets);
        attributes = attributes == null || attributes.isEmpty()
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(attributes));
    }
}
