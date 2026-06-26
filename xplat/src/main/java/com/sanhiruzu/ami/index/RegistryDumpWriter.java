package com.sanhiruzu.ami.index;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Builds and writes the registry-dump JSON consumed by the override editor tool. */
public final class RegistryDumpWriter {

    public static final int SCHEMA_VERSION = 1;

    public record Row(String id, String mod, String className, String displayName,
                      List<String> creativeTabs,
                      String currentCategory, String currentSubcategory,
                      List<String> currentFacets) {}

    private RegistryDumpWriter() {}

    public static int writeJson(Path out, List<Row> rows) throws IOException {
        JsonObject doc = new JsonObject();
        doc.addProperty("schemaVersion", SCHEMA_VERSION);
        JsonArray items = new JsonArray();
        for (Row r : rows) {
            JsonObject o = new JsonObject();
            o.addProperty("id", r.id());
            o.addProperty("mod", r.mod());
            o.addProperty("className", r.className());
            o.addProperty("displayName", r.displayName());
            o.add("creativeTabs", strings(r.creativeTabs()));
            if (r.currentCategory() != null) o.addProperty("currentCategory", r.currentCategory());
            if (r.currentSubcategory() != null) o.addProperty("currentSubcategory", r.currentSubcategory());
            o.add("currentFacets", strings(r.currentFacets()));
            items.add(o);
        }
        doc.add("items", items);
        Files.writeString(out, doc.toString(), StandardCharsets.UTF_8);
        return rows.size();
    }

    private static JsonArray strings(List<String> values) {
        JsonArray a = new JsonArray();
        if (values != null) {
            for (String s : values) a.add(new JsonPrimitive(s));
        }
        return a;
    }

    /**
     * Collects registry dump rows from the live runtime index.
     * Uses GlobalIndex ITEM nodes; reads classification metadata via SearchNodeKeys.
     * Creative tabs are read from CREATIVE_TAB_LABEL (comma-separated in some indexers)
     * or fallen back to CREATIVE_TAB_ID. Item class comes from the ITEM_CLASS metadata key.
     *
     * @param level unused — kept for API symmetry with other dump collectors
     */
    public static List<Row> collectFromRuntime(net.minecraft.world.level.Level level) {
        java.util.List<Row> rows = new java.util.ArrayList<>();
        for (SearchNode node : GlobalIndex.getInstance().getNodes(NodeType.ITEM)) {
            net.minecraft.resources.ResourceLocation id =
                    net.minecraft.resources.ResourceLocation.tryParse(node.id().toString());
            if (id == null) continue;

            String displayName = node.displayName();
            String className = node.meta(SearchNodeKeys.ITEM_CLASS, "");
            if (className.isBlank()) {
                // Fall back to looking up the item class from the registry
                net.minecraft.world.item.Item item =
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
                if (item != null) {
                    className = item.getClass().getName();
                }
            }
            String mod = id.getNamespace();

            String category = emptyToNull(node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
            String subcategory = emptyToNull(node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));

            // FACETS is comma-separated
            java.util.List<String> facets = splitComma(node.meta(SearchNodeKeys.FACETS));

            // Creative tabs: prefer CREATIVE_TAB_LABEL, fall back to CREATIVE_TAB_ID
            String tabLabel = node.meta(SearchNodeKeys.CREATIVE_TAB_LABEL);
            String tabId = node.meta(SearchNodeKeys.CREATIVE_TAB_ID);
            java.util.List<String> tabs;
            if (!tabLabel.isBlank()) {
                tabs = java.util.List.of(tabLabel);
            } else if (!tabId.isBlank()) {
                tabs = java.util.List.of(tabId);
            } else {
                tabs = java.util.List.of();
            }

            rows.add(new Row(id.toString(), mod, className, displayName, tabs,
                    category, subcategory, facets));
        }
        return rows;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static java.util.List<String> splitComma(String s) {
        if (s == null || s.isBlank()) return java.util.List.of();
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String part : s.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }
}
