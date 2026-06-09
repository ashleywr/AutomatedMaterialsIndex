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
        // First pass: collect all chemicals by ingredient type UID (any mod)
        Map<String, String> chemicalBaseNames = new HashMap<>();
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

        // Second pass: group fluids, ingredients, and buckets by their chemical base name (any mod)
        for (NodeType type : NodeType.values()) {
            for (SearchNode node : index.getNodes(type)) {
                if (node == null) continue;

                ResourceLocation id = node.id();
                if (id == null) {
                    continue;
                }

                Map<String, String> meta = node.metadata();
                String path = id.getPath().toLowerCase(Locale.ROOT);

                // Check if this node belongs to a chemical family
                String baseName = null;
                String displayName = node.displayName();

                if (type == NodeType.FLUID) {
                    baseName = extractBaseName(path);
                    if (chemicalBaseNames.containsKey(baseName)) {
                        String modId = id.getNamespace();
                        meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_CATEGORY, modId);
                        meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "chemicals");
                        displayName = chemicalBaseNames.get(baseName);
                    }
                }

                if (type == NodeType.INGREDIENT) {
                    String typeUid = meta.get(RecipeViewerIngredientProvider.TYPE_UID_KEY);
                    if (isChemicalIngredient(typeUid)) {
                        baseName = extractBaseName(path);
                    }
                }

                if (type == NodeType.ITEM && path.endsWith("_bucket")) {
                    baseName = extractBaseName(path.replace("_bucket", ""));
                    if (chemicalBaseNames.containsKey(baseName)) {
                        displayName = chemicalBaseNames.get(baseName);
                    }
                }

                if (baseName != null && chemicalBaseNames.containsKey(baseName)) {
                    applyChemicalGrouping(meta, id.getNamespace(), baseName, chemicalBaseNames.get(baseName));
                }
            }
        }
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
