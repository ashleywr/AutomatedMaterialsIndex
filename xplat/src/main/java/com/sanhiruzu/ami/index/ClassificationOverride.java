package com.sanhiruzu.ami.index;

import java.util.EnumSet;
import java.util.List;

/**
 * Per-item classification override loaded from data. {@code forceCategory}/{@code forceSubcategory}
 * may be null when the override only adjusts facets. {@code tooltipLines} is never null; absent ==
 * empty list.
 */
public record ClassificationOverride(EnumSet<ItemFacet> addFacets,
                                     EnumSet<ItemFacet> removeFacets,
                                     EnumSet<SemanticVerb> addVerbs,
                                     EnumSet<SemanticVerb> removeVerbs,
                                     String forceCategory,
                                     String forceSubcategory,
                                     List<String> tooltipLines) {
    public ClassificationOverride {
        tooltipLines = tooltipLines == null ? List.of() : List.copyOf(tooltipLines);
    }

    public ClassificationOverride(EnumSet<ItemFacet> addFacets,
                                  EnumSet<ItemFacet> removeFacets,
                                  EnumSet<SemanticVerb> addVerbs,
                                  EnumSet<SemanticVerb> removeVerbs,
                                  String forceCategory,
                                  String forceSubcategory) {
        this(addFacets, removeFacets, addVerbs, removeVerbs, forceCategory, forceSubcategory, List.of());
    }

    public ClassificationOverride(EnumSet<ItemFacet> addFacets,
                                  EnumSet<ItemFacet> removeFacets,
                                  String forceCategory,
                                  String forceSubcategory) {
        this(addFacets, removeFacets, EnumSet.noneOf(SemanticVerb.class), EnumSet.noneOf(SemanticVerb.class),
                forceCategory, forceSubcategory, List.of());
    }

    public boolean hasForcedCategory() {
        return forceCategory != null && !forceCategory.isBlank();
    }

    public String subcategoryOrEmpty() {
        return forceSubcategory == null ? "" : forceSubcategory;
    }
}
