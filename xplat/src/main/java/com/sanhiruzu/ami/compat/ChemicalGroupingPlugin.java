package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.providers.RecipeViewerIngredientProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChemicalGroupingPlugin implements CompatIndexPlugin {
    @Override
    public String modId() {
        return "chemical-grouping";
    }

    @Override
    public void applyToIndex(GlobalIndex index) {
        // First pass: collect chemicals and fluids with their display names
        Map<String, String> chemicalBaseNames = new HashMap<>();
        Map<String, String> fluidDisplayNames = new HashMap<>();

        // Collect ingredients with recognized chemical types
        for (SearchNode ingredientNode : index.getNodes(NodeType.INGREDIENT)) {
            if (ingredientNode == null) continue;
            ResourceLocation id = ingredientNode.id();
            if (id == null) continue;

            Map<String, String> meta = ingredientNode.metadata();
            String typeUid = meta.get(RecipeViewerIngredientProvider.TYPE_UID_KEY);
            if (isChemicalIngredient(typeUid)) {
                String baseName = extractBaseName(id.getPath());
                chemicalBaseNames.put(baseName, ingredientNode.displayName());
            }
        }

        // Collect fluids
        for (SearchNode fluidNode : index.getNodes(NodeType.FLUID)) {
            if (fluidNode == null) continue;
            ResourceLocation id = fluidNode.id();
            if (id == null) continue;

            String baseName = extractBaseName(id.getPath().toLowerCase(Locale.ROOT));
            fluidDisplayNames.put(baseName, fluidNode.displayName());
        }

        // Collect bucket items by class inspection
        Map<String, String> bucketDisplayNames = new HashMap<>();
        for (SearchNode itemNode : index.getNodes(NodeType.ITEM)) {
            if (itemNode == null) continue;
            ResourceLocation id = itemNode.id();
            if (id == null) continue;

            Map<String, String> meta = itemNode.metadata();
            String itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            if (isBucketItem(itemClass)) {
                String baseName = extractBaseName(id.getPath().toLowerCase(Locale.ROOT));
                bucketDisplayNames.put(baseName, itemNode.displayName());
            }
        }

        // Second pass: group ingredients, fluids, and buckets
        for (NodeType type : NodeType.values()) {
            for (SearchNode node : index.getNodes(type)) {
                if (node == null) continue;

                ResourceLocation id = node.id();
                if (id == null) continue;

                Map<String, String> meta = node.metadata();
                String path = id.getPath().toLowerCase(Locale.ROOT);
                String baseName = null;
                String displayName = node.displayName();

                // Process ingredients with chemical types
                if (type == NodeType.INGREDIENT) {
                    String typeUid = meta.get(RecipeViewerIngredientProvider.TYPE_UID_KEY);
                    if (isChemicalIngredient(typeUid)) {
                        baseName = extractBaseName(path);
                        String modId = id.getNamespace();
                        meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_CATEGORY, modId);
                        meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "chemicals");
                    }
                }

                // Process fluids that have a matching bucket
                if (type == NodeType.FLUID) {
                    baseName = extractBaseName(path);
                    if (bucketDisplayNames.containsKey(baseName)) {
                        String modId = id.getNamespace();
                        meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_CATEGORY, modId);
                        meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "chemicals");
                    }
                }

                // Process bucket items with matching fluids
                if (type == NodeType.ITEM) {
                    String itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
                    if (isBucketItem(itemClass)) {
                        baseName = extractBaseName(path);
                        if (fluidDisplayNames.containsKey(baseName)) {
                            displayName = fluidDisplayNames.get(baseName);
                        }
                    }
                }

                // Apply grouping if this is part of a chemical family
                if (baseName != null && (chemicalBaseNames.containsKey(baseName) || (fluidDisplayNames.containsKey(baseName) && bucketDisplayNames.containsKey(baseName)))) {
                    String groupDisplayName = chemicalBaseNames.getOrDefault(baseName, displayName);
                    applyChemicalGrouping(meta, id.getNamespace(), baseName, groupDisplayName);
                }
            }
        }
    }

    private boolean isBucketItem(String itemClass) {
        if (itemClass == null || itemClass.isBlank()) return false;
        return itemClass.contains("BucketItem") || itemClass.contains(".Bucket");
    }

    private void applyChemicalGrouping(Map<String, String> meta, String modNamespace, String baseName, String displayName) {
        meta.putIfAbsent(SearchNodeKeys.COLLAPSE_FAMILY, modNamespace + ":" + baseName);
        meta.putIfAbsent(SearchNodeKeys.COLLAPSE_LABEL, displayName);
        meta.putIfAbsent(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
    }

    private String extractBaseName(String path) {
        String normalized = path.toLowerCase(Locale.ROOT).replace("_bucket", "");
        // Handle variant IDs like "oxygen/rv/38f493f35930" -> "oxygen"
        if (normalized.contains("/")) {
            normalized = normalized.substring(0, normalized.indexOf('/'));
        }
        return normalized;
    }

    private boolean isChemicalIngredient(String typeUid) {
        if (typeUid == null) return false;
        String lower = typeUid.toLowerCase(Locale.ROOT);
        return lower.contains("chemical") || lower.contains("gas") || lower.contains("pigment")
                || lower.contains("slurry") || lower.contains("infuse");
    }
}
