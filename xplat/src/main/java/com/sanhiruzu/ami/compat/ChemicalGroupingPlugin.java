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
        Map<String, String> chemicalBaseNames = new HashMap<>();
        Map<String, String> fluidDisplayNames = new HashMap<>();
        Map<String, String> bucketDisplayNames = new HashMap<>();

        // Collect ingredients with recognized chemical types
        for (SearchNode node : index.getNodes(NodeType.INGREDIENT)) {
            if (node == null || node.id() == null) continue;
            String typeUid = node.metadata().get(RecipeViewerIngredientProvider.TYPE_UID_KEY);
            if (isChemicalIngredient(typeUid)) {
                String baseName = extractBaseName(node.id().getPath());
                chemicalBaseNames.put(baseName, node.displayName());
            }
        }

        // Collect fluids
        for (SearchNode node : index.getNodes(NodeType.FLUID)) {
            if (node == null || node.id() == null) continue;
            String baseName = extractBaseName(node.id().getPath());
            fluidDisplayNames.put(baseName, node.displayName());
        }

        // Collect bucket items by class inspection
        for (SearchNode node : index.getNodes(NodeType.ITEM)) {
            if (node == null || node.id() == null) continue;
            String itemClass = node.metadata().getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            if (isBucketItem(itemClass)) {
                String baseName = extractBaseName(node.id().getPath());
                bucketDisplayNames.put(baseName, node.displayName());
            }
        }

        // Apply grouping to collected families
        processIngredients(index, chemicalBaseNames);
        processFluids(index, fluidDisplayNames, bucketDisplayNames);
        processBuckets(index, fluidDisplayNames);
    }

    private void processIngredients(GlobalIndex index, Map<String, String> chemicalBaseNames) {
        for (SearchNode node : index.getNodes(NodeType.INGREDIENT)) {
            if (node == null || node.id() == null) continue;
            Map<String, String> meta = node.metadata();
            String typeUid = meta.get(RecipeViewerIngredientProvider.TYPE_UID_KEY);
            if (isChemicalIngredient(typeUid)) {
                String baseName = extractBaseName(node.id().getPath());
                String modId = node.id().getNamespace();
                meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_CATEGORY, modId);
                meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "chemicals");
                applyChemicalGrouping(meta, modId, baseName, chemicalBaseNames.get(baseName));
            }
        }
    }

    private void processFluids(GlobalIndex index, Map<String, String> fluidDisplayNames, Map<String, String> bucketDisplayNames) {
        for (SearchNode node : index.getNodes(NodeType.FLUID)) {
            if (node == null || node.id() == null) continue;
            String baseName = extractBaseName(node.id().getPath());
            if (bucketDisplayNames.containsKey(baseName)) {
                Map<String, String> meta = node.metadata();
                String modId = node.id().getNamespace();
                meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_CATEGORY, modId);
                meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "chemicals");
                applyChemicalGrouping(meta, modId, baseName, fluidDisplayNames.get(baseName));
            }
        }
    }

    private void processBuckets(GlobalIndex index, Map<String, String> fluidDisplayNames) {
        for (SearchNode node : index.getNodes(NodeType.ITEM)) {
            if (node == null || node.id() == null) continue;
            Map<String, String> meta = node.metadata();
            String itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            if (isBucketItem(itemClass)) {
                String baseName = extractBaseName(node.id().getPath());
                if (fluidDisplayNames.containsKey(baseName)) {
                    String modId = node.id().getNamespace();
                    applyChemicalGrouping(meta, modId, baseName, fluidDisplayNames.get(baseName));
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
