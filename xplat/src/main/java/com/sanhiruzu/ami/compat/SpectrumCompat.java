package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class SpectrumCompat {
    private static final String MOD_ID = "spectrum";

    private SpectrumCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }
        if (meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()) {
            CompatFamilyDetector.detect(id, meta);
        }
        if (!isSpectrumItem(id, meta)) {
            return;
        }

        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addClassFacts(context, facts);
        addTagFacts(context, facts);
        addPathFacts(context, facts);
        addRecipeFacts(context, facts);

        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put(SearchNodeKeys.SPECTRUM_ITEM_KIND, kind);
            addSearchToken(meta, "spectrum_" + kind);
        }
        applyKindMetadata(kind, meta);
        if (!facts.isEmpty()) {
            meta.put(SearchNodeKeys.SPECTRUM_FACTS, join(facts));
            for (String fact : facts) {
                addSearchToken(meta, fact);
                addSearchToken(meta, "spectrum_" + fact);
            }
        }
    }

    private static boolean isSpectrumItem(ResourceLocation id, Map<String, String> meta) {
        return MOD_ID.equals(id.getNamespace()) || CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.SPECTRUM);
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (containsAny(context.itemClass, "StructurePlacerItem")) {
            facts.add("structure_placer");
        }
        if (containsAny(context.itemClass, "CloakedItem", "CloakedItemWithLoomPattern",
                "GlassAmpouleItem", "MidnightAberrationItem", "AetherVestigesItem", "StormStoneItem")) {
            facts.add("reagent");
        }
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        if (hasAnyTag(context.tags, "spectrum:reagent/reagents", "spectrum:reagent/complex",
                "spectrum:memory_bonding_agents")) {
            facts.add("reagent");
        }
        if (hasAnyTag(context.tags, "spectrum:pure_resources")) {
            facts.add("material");
        }
        if (hasAnyTag(context.tags, "spectrum:pastel_node_upgrades")) {
            facts.add("upgrade");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        if (context.path.endsWith("_structure_placer")) {
            facts.add("structure_placer");
        }
        if (context.path.startsWith("raw_") || context.path.endsWith("_fragments") || context.path.endsWith("_gem")
                || containsAny(context.path, "bismuth_flake", "shimmerstone", "storm_stone", "aether_vestiges")) {
            facts.add("reagent");
        }
        if (context.path.startsWith("pure_")) {
            facts.add("material");
        }
        if (context.path.contains("glass_ampoule")) {
            facts.add("reagent");
        }
    }

    private static void addRecipeFacts(Context context, Set<String> facts) {
        if (hasAnyTag(context.recipeCategories, "potion_workshop_reacting", "pedestal")
                || hasAnyTag(context.recipeUseCategories, "potion_workshop_reacting", "fusion_shrine", "spirit_instiller")) {
            facts.add("reagent");
        }
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("structure_placer")) return "structure_placers";
        if (facts.contains("reagent")) return "reagents";
        if (facts.contains("upgrade")) return "upgrades";
        if (facts.contains("material")) return "materials";
        return "";
    }

    private static void applyKindMetadata(String kind, Map<String, String> meta) {
        switch (kind) {
            case "structure_placers" -> {
                addFacet(meta, ItemFacet.UTILITY_MISC);
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_FAMILY, MOD_ID + ":structure_placers");
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_LABEL, "Structure Placers");
                meta.putIfAbsent(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
            }
            case "reagents" -> addFacet(meta, ItemFacet.MAGIC_REAGENT);
            case "upgrades" -> addFacet(meta, ItemFacet.UPGRADE);
            case "materials" -> addFacet(meta, ItemFacet.INGREDIENT_MINERAL);
            default -> {
            }
        }
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (normalized.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyTag(String csv, String... tags) {
        Set<String> expected = Set.of(tags);
        for (String value : splitCsv(csv)) {
            if (expected.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static Iterable<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String value : csv.split(",")) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }
        return values;
    }

    private static void addFacet(Map<String, String> meta, ItemFacet facet) {
        String encoded = meta.getOrDefault(SearchNodeKeys.FACETS, "");
        if (encoded.isBlank()) {
            meta.put(SearchNodeKeys.FACETS, facet.id());
            return;
        }
        for (String value : encoded.split(",")) {
            if (facet.id().equals(value.trim())) {
                return;
            }
        }
        meta.put(SearchNodeKeys.FACETS, encoded + "," + facet.id());
    }

    private static String join(Set<String> values) {
        StringJoiner joiner = new StringJoiner(",");
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                joiner.add(value);
            }
        }
        return joiner.toString();
    }

    private static void addSearchToken(Map<String, String> meta, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        String existing = meta.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, "");
        for (String value : existing.split("\\s+")) {
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        if (values.add(token)) {
            meta.put(SearchNodeKeys.SEARCH_TOKENS, String.join(" ", values));
        }
    }

    private static final class Context {
        final String path;
        final String itemClass;
        final String tags;
        final String recipeCategories;
        final String recipeUseCategories;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
            this.recipeCategories = meta.getOrDefault(SearchNodeKeys.RECIPE_CATEGORIES, "").toLowerCase(Locale.ROOT);
            this.recipeUseCategories = meta.getOrDefault(SearchNodeKeys.RECIPE_USE_CATEGORIES, "").toLowerCase(Locale.ROOT);
        }
    }
}
