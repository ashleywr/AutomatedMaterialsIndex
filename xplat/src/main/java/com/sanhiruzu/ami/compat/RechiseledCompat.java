package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Set;

public final class RechiseledCompat {
    private static final String RECHISELED = "rechiseled";
    private static final String RECHISELED_CREATE = "rechiseledcreate";
    private static final Set<String> SHAPE_TAG_SUFFIXES = Set.of("_stairs", "_slabs");

    private RechiseledCompat() {
    }

    public static void enrichItem(Identifier id, Map<String, String> meta) {
        if (id == null || meta == null || !isSupportedNamespace(id)) {
            return;
        }

        if (isChisel(id, meta)) {
            meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_CATEGORY, "tools");
            meta.putIfAbsent(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "utility");
            addSearchToken(meta, "chisel");
            return;
        }

        if (!isRechiseledBlock(meta)) {
            return;
        }

        if (RECHISELED.equals(id.getNamespace())
                && GeneratedPaletteCompat.collapseByBaseTag(meta, RECHISELED, SHAPE_TAG_SUFFIXES)) {
            return;
        }

        if (RECHISELED_CREATE.equals(id.getNamespace())) {
            String root = rechiseledCreateFamilyRoot(id.getPath());
            GeneratedPaletteCompat.collapseByPathRoot(id, meta, root, root.endsWith("_window"));
        }
    }

    private static boolean isSupportedNamespace(Identifier id) {
        return RECHISELED.equals(id.getNamespace()) || RECHISELED_CREATE.equals(id.getNamespace());
    }

    private static boolean isChisel(Identifier id, Map<String, String> meta) {
        return RECHISELED.equals(id.getNamespace()) && "chisel".equals(id.getPath())
                || meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").endsWith(".ChiselItem");
    }

    private static boolean isRechiseledBlock(Map<String, String> meta) {
        String itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
        String blockClass = meta.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
        String facets = meta.getOrDefault(SearchNodeKeys.FACETS, "");
        return itemClass.contains(".rechiseled.")
                || blockClass.contains(".rechiseled.")
                || hasToken(facets, "placeable");
    }

    private static String rechiseledCreateFamilyRoot(String path) {
        String normalized = GeneratedPaletteCompat.stripConnectingAndShapeSuffixes(path);
        int windowIndex = normalized.indexOf("_window_");
        if (windowIndex >= 0) {
            return normalized.substring(0, windowIndex + "_window".length());
        }
        return normalized;
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
