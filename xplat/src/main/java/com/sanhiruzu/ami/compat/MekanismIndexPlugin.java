package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.providers.RecipeViewerIngredientProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Map;

public class MekanismIndexPlugin implements CompatIndexPlugin {
    @Override
    public String modId() {
        return "mekanism";
    }

    @Override
    public void applyToIndex(GlobalIndex index) {
        for (NodeType type : NodeType.values()) {
            for (SearchNode node : index.getNodes(type)) {
                if (node == null) continue;

                ResourceLocation id = node.id();
                if (id == null || !"mekanism".equals(id.getNamespace())) {
                    continue;
                }

                Map<String, String> meta = node.metadata();
                String path = id.getPath().toLowerCase(Locale.ROOT);

                if (type == NodeType.FLUID && isChemicalFluid(path)) {
                    meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_CATEGORY, "mekanism");
                    meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "chemicals");
                    String chemicalBase = extractChemicalBase(path);
                    applyChemicalGrouping(meta, chemicalBase, node.displayName());
                }

                if (type == NodeType.INGREDIENT) {
                    String typeUid = meta.get(RecipeViewerIngredientProvider.TYPE_UID_KEY);
                    if (isChemicalIngredient(typeUid)) {
                        String chemicalBase = extractChemicalBase(path);
                        applyChemicalGrouping(meta, chemicalBase, node.displayName());
                    }
                }

                if (type == NodeType.ITEM && isChemicalBucketItem(path)) {
                    String chemicalBase = extractChemicalBaseFromBucket(path);
                    applyChemicalGrouping(meta, chemicalBase, extractChemicalDisplayName(path));
                }
            }
        }
    }

    private void applyChemicalGrouping(Map<String, String> meta, String collapseFamily, String displayName) {
        meta.putIfAbsent(SearchNodeKeys.COLLAPSE_FAMILY, collapseFamily);
        meta.putIfAbsent(SearchNodeKeys.COLLAPSE_LABEL, displayName);
        meta.putIfAbsent(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
    }

    private String extractChemicalBase(String path) {
        return "mekanism:" + path;
    }

    private String extractChemicalBaseFromBucket(String path) {
        String base = path.replace("_bucket", "");
        return "mekanism:" + base;
    }

    private String extractChemicalDisplayName(String bucketPath) {
        String base = bucketPath.replace("_bucket", "");
        return base.substring(0, 1).toUpperCase(Locale.ROOT) + base.substring(1).replace("_", " ");
    }

    private boolean isChemicalFluid(String path) {
        return path.contains("oxygen") || path.contains("hydrogen") || path.contains("nitrogen")
                || path.contains("fluorine") || path.contains("chlorine") || path.contains("sulfur")
                || path.contains("ethene") || path.contains("sodium") || path.contains("brine")
                || path.contains("lithium") || path.contains("osmium") || path.contains("steam");
    }

    private boolean isChemicalIngredient(String typeUid) {
        if (typeUid == null) return false;
        String lower = typeUid.toLowerCase(Locale.ROOT);
        return lower.contains("chemical") || lower.contains("gas") || lower.contains("pigment")
                || lower.contains("slurry") || lower.contains("infuse");
    }

    private boolean isChemicalBucketItem(String path) {
        if (!path.endsWith("_bucket")) return false;
        String base = path.replace("_bucket", "");
        return isChemicalFluid(base);
    }
}
