package com.sanhiruzu.ami.index;

import java.util.EnumSet;
import java.util.Set;

public record ModPatternRule(String modId, Set<String> pathTokens, Set<String> classTokens,
                             EnumSet<ItemFacet> addFacets, EnumSet<ItemFacet> removeFacets,
                             EnumSet<SemanticVerb> addVerbs, EnumSet<SemanticVerb> removeVerbs,
                             String category, String subcategory,
                             String collapseFamily, String collapseLabel, String collapseMode) {

    public ModPatternRule(String modId, Set<String> pathTokens, Set<String> classTokens,
                          EnumSet<ItemFacet> addFacets, EnumSet<ItemFacet> removeFacets,
                          String category, String subcategory,
                          String collapseFamily, String collapseLabel, String collapseMode) {
        this(modId, pathTokens, classTokens, addFacets, removeFacets,
                EnumSet.noneOf(SemanticVerb.class), EnumSet.noneOf(SemanticVerb.class),
                category, subcategory, collapseFamily, collapseLabel, collapseMode);
    }

    public ModPatternRule(String modId, Set<String> pathTokens, String category, String subcategory) {
        this(modId, pathTokens, Set.of(), EnumSet.noneOf(ItemFacet.class), EnumSet.noneOf(ItemFacet.class),
                category, subcategory, null, null, null);
    }

    public boolean hasCategory() {
        return category != null && !category.isBlank();
    }

    public boolean hasCollapse() {
        return collapseFamily != null && !collapseFamily.isBlank();
    }
}
