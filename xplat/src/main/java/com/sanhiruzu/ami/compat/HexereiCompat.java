package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HexereiCompat {
    private static final String MOD_ID = "hexerei";

    private HexereiCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
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
            meta.put("hexereiItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "hexerei_" + kind);
        }
        applyKindMetadata(kind, meta);
        if (!facts.isEmpty()) {
            meta.put("hexereiFacts", CompatMetaUtil.join(facts));
        }
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (CompatMetaUtil.containsAny(context.itemClass, "WhistleItem", "DowsingRodItem")) facts.add("navigation_tool");
        if (CompatMetaUtil.containsAny(context.itemClass, "CrowFluteItem", "WaxingKitItem", "KeychainItem")) facts.add("utility_tool");
        if (CompatMetaUtil.containsAny(context.itemClass, "FlowerOutputItem", "BlendItem")) facts.add("organic_reagent");
        if (CompatMetaUtil.containsAny(context.itemClass, "HexereiBookItem")) facts.add("guide_book");
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        for (String tag : CompatMetaUtil.splitCsv(context.tags)) {
            if (tag.startsWith("hexerei:herbs")) facts.add("organic_reagent");
            if (tag.startsWith("hexerei:sigils")) facts.add("magic_artifact");
            if (tag.startsWith("hexerei:broom_misc") || tag.startsWith("hexerei:broom_brush")) facts.add("broom_tool");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = CompatMetaUtil.basePath(context.path);
        if (path.contains("broom")) facts.add("broom_tool");
        if (CompatMetaUtil.containsAny(path, "sage", "mandrake", "moon_dust", "wax_blend")) facts.add("organic_reagent");
        if (path.contains("sigil")) facts.add("magic_artifact");
        if (CompatMetaUtil.containsAny(path, "dowsing", "whistle")) facts.add("navigation_tool");
        if (path.contains("flute")) facts.add("utility_tool");
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("guide_book")) return "guide_book";
        if (facts.contains("magic_artifact")) return "magic_artifact";
        if (facts.contains("navigation_tool")) return "navigation_tool";
        if (facts.contains("broom_tool")) return "broom_tool";
        if (facts.contains("utility_tool")) return "utility_tool";
        if (facts.contains("organic_reagent")) return "organic_reagent";
        return "";
    }

    private static void applyKindMetadata(String kind, Map<String, String> meta) {
        switch (kind) {
            case "guide_book" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.BOOK);
                CompatMetaUtil.addFacet(meta, ItemFacet.GUIDE_BOOK);
            }
            case "magic_artifact" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
            case "navigation_tool" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_NAVIGATION);
            case "broom_tool" -> CompatMetaUtil.addFacet(meta, ItemFacet.TRANSPORT);
            case "utility_tool" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_TOOL);
            case "organic_reagent" -> CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_ORGANIC);
            default -> {
            }
        }
    }

    private static final class Context {
        final String path;
        final String itemClass;
        final String tags;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT);
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
        }
    }
}
