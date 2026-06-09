package com.sanhiruzu.ami.index;

/**
 * Relationship types between nodes in the AMI discovery graph.
 */
public enum EdgeType {
    DROPS,
    SPAWNS_IN,
    GENERATES_IN,
    CRAFTED_WITH,
    // Recipe graph edges (wired by RecipeGraphProvider)
    PRODUCES,   // RECIPE → ITEM: the recipe outputs this item
    REQUIRES,   // RECIPE → ITEM: the recipe needs this item as an ingredient
    OUTPUT_OF,  // ITEM → RECIPE: this item is produced by the recipe
    USED_IN     // ITEM → RECIPE: this item is consumed as an ingredient in the recipe
}
