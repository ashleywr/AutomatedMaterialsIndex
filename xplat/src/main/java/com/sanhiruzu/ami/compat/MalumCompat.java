package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MalumCompat {
    private static final String MOD_ID = "malum";

    private MalumCompat() {
    }

    public static void enrichItem(Identifier id, Map<String, String> meta) {
        if (id == null || meta == null || !MOD_ID.equals(id.getNamespace())) {
            return;
        }
        meta.putIfAbsent(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, MOD_ID);
        meta.putIfAbsent(SearchNodeKeys.COMPAT_FAMILIES, MOD_ID);
        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addClassFacts(context, facts);
        addTagFacts(context, facts);
        addPathFacts(context, facts);

        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put("malumItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "malum_" + kind);
        }
        applyKindMetadata(context, kind, meta);
        if (!facts.isEmpty()) {
            meta.put("malumFacts", CompatMetaUtil.join(facts));
        }
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (CompatMetaUtil.containsAny(context.itemClass, "GeasItem")) facts.add("geas");
        if (CompatMetaUtil.containsAny(context.itemClass, "SpiritShardItem", "UmbralSpiritShardItem")) facts.add("spirit_reagent");
        if (CompatMetaUtil.containsAny(context.itemClass, "ImpetusItem", "FracturedImpetusItem", "NodeItem")) facts.add("impetus");
        if (CompatMetaUtil.containsAny(context.itemClass, "augment")) facts.add("augment");
        if (CompatMetaUtil.containsAny(context.itemClass, "Encyclopedia")) facts.add("guide_book");
        if (CompatMetaUtil.containsAny(context.itemClass, "TinkeringToolItem")) facts.add("utility_tool");
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        for (String tag : CompatMetaUtil.splitCsv(context.tags)) {
            if (tag.startsWith("malum:spirits") || tag.startsWith("malum:aspected_spirits")) facts.add("spirit_reagent");
            if (tag.startsWith("malum:augments")) facts.add("augment");
            if (tag.startsWith("malum:impetus") || tag.startsWith("malum:fractured_impetus")) facts.add("impetus");
            if (tag.startsWith("malum:materials") || tag.startsWith("malum:minerals")) facts.add("material");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = CompatMetaUtil.basePath(context.path);
        if (path.contains("geas")) facts.add("geas");
        if (CompatMetaUtil.containsAny(path, "spirit", "soul_stained", "runewood", "soulwood")) facts.add("spirit_reagent");
        if (CompatMetaUtil.containsAny(path, "ingot", "nugget", "chunk", "quartz", "crystal")) facts.add("material");
        if (path.contains("rune")) facts.add("rune");
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("guide_book")) return "guide_books";
        if (facts.contains("geas")) return "geasa";
        if (facts.contains("impetus")) return "impetus";
        if (facts.contains("augment")) return "augments";
        if (facts.contains("rune") || facts.contains("spirit_reagent")) return "reagents";
        if (facts.contains("material")) return "materials";
        if (facts.contains("utility_tool")) return "tools";
        return "";
    }

    private static void applyKindMetadata(Context context, String kind, Map<String, String> meta) {
        switch (kind) {
            case "guide_books" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.BOOK);
                CompatMetaUtil.addFacet(meta, ItemFacet.GUIDE_BOOK);
                route(meta, "misc");
            }
            case "geasa" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_FAMILY, MOD_ID + ":geas");
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_LABEL, "Geas");
                meta.putIfAbsent(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
                route(meta, "geasa");
            }
            case "impetus", "augments" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
                CompatMetaUtil.addFacet(meta, ItemFacet.UPGRADE);
                route(meta, kind.equals("impetus") ? "impetus" : "augments");
            }
            case "reagents" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_REAGENT);
                route(meta, "spirits");
            }
            case "materials" -> {
                if (context.path.contains("nugget")) CompatMetaUtil.addFacet(meta, ItemFacet.NUGGET);
                else if (context.path.contains("ingot")) CompatMetaUtil.addFacet(meta, ItemFacet.INGOT);
                else CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_MINERAL);
                CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_MINERAL);
                route(meta, "materials");
            }
            case "tools" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_TOOL);
                route(meta, "equipment");
            }
            default -> {
            }
        }
    }

    private static void route(Map<String, String> meta, String subcategory) {
        meta.put(SearchNodeKeys.COMPAT_ROUTE_CATEGORY, MOD_ID);
        meta.put(SearchNodeKeys.COMPAT_ROUTE_SUBCATEGORY, subcategory);
    }

    private static final class Context {
        final String path;
        final String itemClass;
        final String tags;

        Context(Identifier id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
        }
    }
}
