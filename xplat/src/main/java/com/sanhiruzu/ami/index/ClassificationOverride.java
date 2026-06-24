package com.sanhiruzu.ami.index;

import java.util.EnumSet;

/**
 * Per-item classification override loaded from data. {@code forceCategory}/{@code forceSubcategory}
 * may be null when the override only adjusts facets.
 */
public record ClassificationOverride(EnumSet<ItemFacet> addFacets,
                                     EnumSet<ItemFacet> removeFacets,
                                     EnumSet<SemanticVerb> addVerbs,
                                     EnumSet<SemanticVerb> removeVerbs,
                                     String forceCategory,
                                     String forceSubcategory) {
    public ClassificationOverride(EnumSet<ItemFacet> addFacets,
                                  EnumSet<ItemFacet> removeFacets,
                                  String forceCategory,
                                  String forceSubcategory) {
        this(addFacets, removeFacets, EnumSet.noneOf(SemanticVerb.class), EnumSet.noneOf(SemanticVerb.class),
                forceCategory, forceSubcategory);
    }

    public boolean hasForcedCategory() {
        return forceCategory != null && !forceCategory.isBlank();
    }

    public String subcategoryOrEmpty() {
        return forceSubcategory == null ? "" : forceSubcategory;
    }
}
