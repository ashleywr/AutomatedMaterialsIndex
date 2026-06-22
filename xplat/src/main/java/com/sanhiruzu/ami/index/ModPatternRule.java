package com.sanhiruzu.ami.index;

import java.util.EnumSet;
import java.util.Set;

/**
 * Per-mod path-token rule. If an item from {@code modId} has any of {@code pathTokens}:
 * apply {@code addFacets}/{@code removeFacets}, and -- when {@link #hasCategory()} -- route to
 * {@code category}/{@code subcategory}. A rule with no category applies facets and falls through.
 */
public record ModPatternRule(String modId, Set<String> pathTokens,
                             EnumSet<ItemFacet> addFacets, EnumSet<ItemFacet> removeFacets,
                             String category, String subcategory) {

    public ModPatternRule(String modId, Set<String> pathTokens, String category, String subcategory) {
        this(modId, pathTokens, EnumSet.noneOf(ItemFacet.class), EnumSet.noneOf(ItemFacet.class),
                category, subcategory);
    }

    public boolean hasCategory() {
        return category != null && !category.isBlank();
    }
}
