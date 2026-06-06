package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

public final class ChippedCompat {
    private static final String CHIPPED = "chipped";
    private static final Set<String> SHAPE_TAG_SUFFIXES = Set.of(
            "_stairs",
            "_slab",
            "_slabs",
            "_wall",
            "_walls",
            "_pane",
            "_panes",
            "_button",
            "_buttons",
            "_pressure_plate",
            "_pressure_plates"
    );

    private ChippedCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null || !CHIPPED.equals(id.getNamespace())) {
            return;
        }

        if (isWorkbenchItem(meta)) {
            meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_CATEGORY, "tools");
            meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "utility");
            addSearchToken(meta, "chipped_workbench");
            return;
        }

        if (!hasToken(meta.getOrDefault(SearchNodeKeys.FACETS, ""), "placeable")) {
            return;
        }

        GeneratedPaletteCompat.collapseByBaseTag(meta, CHIPPED, SHAPE_TAG_SUFFIXES);
    }

    private static boolean isWorkbenchItem(Map<String, String> meta) {
        return meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").endsWith(".WorkbenchItem");
    }

    private static boolean hasToken(String csv, String token) {
        if (csv == null || csv.isBlank()) {
            return false;
        }
        for (String value : csv.split(",")) {
            if (token.equals(value.trim())) {
                return true;
            }
        }
        return false;
    }

    private static void addSearchToken(Map<String, String> meta, String token) {
        String existing = meta.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, "");
        if (hasToken(existing.replace(' ', ','), token)) {
            return;
        }
        meta.put(SearchNodeKeys.SEARCH_TOKENS, existing.isBlank() ? token : existing + " " + token);
    }
}
