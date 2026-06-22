package com.sanhiruzu.ami.index;

import java.util.EnumSet;
import java.util.Set;

/**
 * Per-mod classification rule. Fires when an item from {@code modId} matches any path token
 * OR any class token (substring of the item class name). Applies facets and — when
 * {@link #hasCategory()} — routes to {@code category}/{@code subcategory}. A rule with no
 * category applies facets and falls through.
 */
public record ModPatternRule(String modId, Set<String> pathTokens, Set<String> classTokens,
                             EnumSet<ItemFacet> addFacets, EnumSet<ItemFacet> removeFacets,
                             String category, String subcategory) {

    /** Convenience constructor for rules that only match path tokens (no class tokens). */
    public ModPatternRule(String modId, Set<String> pathTokens, String category, String subcategory) {
        this(modId, pathTokens, Set.of(), EnumSet.noneOf(ItemFacet.class), EnumSet.noneOf(ItemFacet.class),
                category, subcategory);
    }

    public boolean hasCategory() {
        return category != null && !category.isBlank();
    }
}
