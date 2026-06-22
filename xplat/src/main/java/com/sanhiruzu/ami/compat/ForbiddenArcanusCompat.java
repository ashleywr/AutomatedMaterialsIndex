package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ForbiddenArcanusCompat {
    private static final String MOD_ID = "forbidden_arcanus";

    private ForbiddenArcanusCompat() {
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
            meta.put("forbiddenArcanusItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "forbidden_arcanus_" + kind);
        }
        applyKindMetadata(kind, meta);
        if (!facts.isEmpty()) {
            meta.put("forbiddenArcanusFacts", CompatMetaUtil.join(facts));
        }
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (CompatMetaUtil.containsAny(context.itemClass, "QuantumCatcherItem")) facts.add("capture_tool");
        if (CompatMetaUtil.containsAny(context.itemClass, "XpetrifiedOrbItem", "WhirlwindPrismItem",
                "SmelterPrismItem", "DracoArcanusScepterItem")) {
            facts.add("magic_artifact");
        }
        if (CompatMetaUtil.containsAny(context.itemClass, "SoulExtractorItem")) facts.add("utility_tool");
        if (CompatMetaUtil.containsAny(context.itemClass, "AurealTankItem")) facts.add("storage_vessel");
        if (CompatMetaUtil.containsAny(context.itemClass, "FerrogneticMixtureItem", "EdelwoodOilItem", "DarkMatterItem",
                "DarkSoulItem")) {
            facts.add("magic_reagent");
        }
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        for (String tag : CompatMetaUtil.splitCsv(context.tags)) {
            if (tag.startsWith("forbidden_arcanus:clibano/creates_")) facts.add("soul_reagent");
            if (tag.startsWith("c:explosion_resistant")) facts.add("magic_artifact");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = CompatMetaUtil.basePath(context.path);
        if (path.contains("quantum_catcher") || path.contains("boss_catcher")) facts.add("capture_tool");
        if (CompatMetaUtil.containsAny(path, "stella", "orb", "wardstone", "prism", "scepter", "pact", "moon",
                "elementarium", "stellarite", "crimson_stone")) {
            facts.add("magic_artifact");
        }
        if (CompatMetaUtil.containsAny(path, "soul", "matter", "oil", "mixture")) facts.add("magic_reagent");
        if (path.contains("tank")) facts.add("storage_vessel");
        if (path.contains("extractor")) facts.add("utility_tool");
        if (CompatMetaUtil.containsAny(path, "stick", "wax")) facts.add("organic_reagent");
        if (path.contains("scrap")) facts.add("mineral_reagent");
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("capture_tool")) return "capture_tool";
        if (facts.contains("storage_vessel")) return "storage_vessel";
        if (facts.contains("utility_tool")) return "utility_tool";
        if (facts.contains("magic_artifact")) return "magic_artifact";
        if (facts.contains("soul_reagent")) return "soul_reagent";
        if (facts.contains("magic_reagent")) return "magic_reagent";
        if (facts.contains("organic_reagent")) return "organic_reagent";
        if (facts.contains("mineral_reagent")) return "mineral_reagent";
        return "";
    }

    private static void applyKindMetadata(String kind, Map<String, String> meta) {
        switch (kind) {
            case "capture_tool", "utility_tool" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_TOOL);
            case "storage_vessel" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_MISC);
            case "magic_artifact" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
            case "soul_reagent", "magic_reagent" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_REAGENT);
            case "organic_reagent" -> CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_ORGANIC);
            case "mineral_reagent" -> CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_MINERAL);
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
