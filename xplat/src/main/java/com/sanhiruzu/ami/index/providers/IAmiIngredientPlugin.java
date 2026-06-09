package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.resources.ResourceLocation;

/**
 * Plugin interface for mods to register custom ingredient types with AMI.
 * Replaces dependency on recipe viewers (JEI/EMI) for ingredient discovery.
 */
public interface IAmiIngredientPlugin {
    /**
     * Unique identifier for this plugin (typically the mod ID).
     */
    String modId();

    /**
     * Register custom ingredients with the AMI index.
     * Called during the ingredient discovery phase.
     */
    void registerIngredients(IngredientRegistry registry);

    /**
     * Registry for custom ingredients.
     */
    interface IngredientRegistry {
        /**
         * Add a custom ingredient to the index.
         *
         * @param id unique resource location for this ingredient
         * @param displayName human-readable name for display
         * @param typeUid a unique identifier for the ingredient type (e.g., "mekanism:gas", "ae2:fluid")
         * @param metadata additional metadata (mod ID, type label, search tokens, etc.)
         */
        void addIngredient(ResourceLocation id, String displayName, String typeUid, java.util.Map<String, String> metadata);

        /**
         * Convenience: add ingredient with minimal metadata.
         */
        default void addIngredient(ResourceLocation id, String displayName, String typeUid) {
            addIngredient(id, displayName, typeUid, java.util.Map.of());
        }
    }
}
