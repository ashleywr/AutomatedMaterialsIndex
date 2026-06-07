package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class NaturesAuraCompat {
    private static final String MOD_ID = "naturesaura";

    private NaturesAuraCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }
        if (meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()) {
            CompatFamilyDetector.detect(id, meta);
        }
        if (!isNaturesAuraItem(id, meta)) {
            return;
        }

        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addClassFacts(context, facts);
        addPathFacts(context, facts);
        addRecipeFacts(context, facts);

        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put(SearchNodeKeys.NATURES_AURA_ITEM_KIND, kind);
            addSearchToken(meta, "naturesaura_" + kind);
        }
        applyKindMetadata(kind, meta);
        if (!facts.isEmpty()) {
            meta.put(SearchNodeKeys.NATURES_AURA_FACTS, join(facts));
            for (String fact : facts) {
                addSearchToken(meta, fact);
                addSearchToken(meta, "naturesaura_" + fact);
            }
        }
    }

    private static boolean isNaturesAuraItem(ResourceLocation id, Map<String, String> meta) {
        return MOD_ID.equals(id.getNamespace())
                || CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.NATURES_AURA);
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (containsAny(context.itemClass, "ItemEffectPowder")) {
            facts.add("effect_powder");
        }
        if (containsAny(context.itemClass, "ItemStructureFinder")) {
            facts.add("structure_finder");
        }
        if (containsAny(context.itemClass, "ItemCaveFinder", "ItemLootFinder", "ItemNetheriteFinder")) {
            facts.add("staff_finder");
        }
        if (containsAny(context.itemClass, "ItemMoverMinecart")) {
            facts.add("transport");
        }
        if (containsAny(context.itemClass, "ItemGoldFiber")) {
            facts.add("material");
        }
        if (containsAny(context.itemClass, "ItemBirthSpirit", "ItemGlowing")) {
            facts.add("spirit");
        }
        if (containsAny(context.itemClass, "ItemEnderAccess", "ItemBreakPrevention", "ItemPetReviver",
                "ItemLightStaff", "ItemShockwaveCreator")) {
            facts.add("artifact");
        }
        if (containsAny(context.itemClass, "ItemColorChanger", "ItemMultiblockMaker", "ItemRangeVisualizer")) {
            facts.add("utility");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        if (context.path.startsWith("effect_powder/variant/")) {
            facts.add("effect_powder");
        }
        if (context.path.startsWith("token_") || context.path.endsWith("_token")) {
            facts.add("token");
        }
        if (context.path.endsWith("_finder")) {
            facts.add(context.path.contains("fortress") || context.path.contains("city") || context.path.contains("outpost")
                    ? "structure_finder"
                    : "staff_finder");
        }
        if (containsAny(context.path, "tainted_gold", "gold_fiber")) {
            facts.add("material");
        }
        if (containsAny(context.path, "calling_spirit", "birth_spirit")) {
            facts.add("spirit");
        }
        if (containsAny(context.path, "farming_stencil")) {
            facts.add("template");
        }
        if (containsAny(context.path, "ender_access", "break_prevention", "pet_reviver")) {
            facts.add("artifact");
        }
        if (containsAny(context.path, "color_changer", "multiblock_maker", "range_visualizer")) {
            facts.add("utility");
        }
        if (containsAny(context.path, "mover_cart")) {
            facts.add("transport");
        }
    }

    private static void addRecipeFacts(Context context, Set<String> facts) {
        if (hasAnyTag(context.recipeCategories, "tree_ritual", "offering")
                || hasAnyTag(context.recipeUseCategories, "tree_ritual", "offering")) {
            facts.add("aura_ritual_item");
        }
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("effect_powder")) return "effect_powders";
        if (facts.contains("structure_finder")) return "structure_finders";
        if (facts.contains("staff_finder")) return "staff_finders";
        if (facts.contains("transport")) return "transport";
        if (facts.contains("material")) return "materials";
        if (facts.contains("template")) return "templates";
        if (facts.contains("artifact")) return "artifacts";
        if (facts.contains("spirit")) return "spirits";
        if (facts.contains("token")) return "tokens";
        if (facts.contains("utility")) return "utility";
        return "";
    }

    private static void applyKindMetadata(String kind, Map<String, String> meta) {
        switch (kind) {
            case "effect_powders" -> {
                addFacet(meta, ItemFacet.MAGIC_REAGENT);
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_FAMILY, MOD_ID + ":effect_powders");
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_LABEL, "Effect Powders");
                meta.putIfAbsent(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
            }
            case "tokens", "spirits" -> addFacet(meta, ItemFacet.MAGIC_REAGENT);
            case "artifacts" -> addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
            case "structure_finders", "staff_finders" -> addFacet(meta, ItemFacet.UTILITY_NAVIGATION);
            case "transport" -> addFacet(meta, ItemFacet.TRANSPORT);
            case "materials" -> addFacet(meta, ItemFacet.INGREDIENT_MINERAL);
            case "templates" -> addFacet(meta, ItemFacet.TEMPLATE);
            case "utility" -> addFacet(meta, ItemFacet.UTILITY_MISC);
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
        final String recipeCategories;
        final String recipeUseCategories;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.recipeCategories = meta.getOrDefault(SearchNodeKeys.RECIPE_CATEGORIES, "").toLowerCase(Locale.ROOT);
            this.recipeUseCategories = meta.getOrDefault(SearchNodeKeys.RECIPE_USE_CATEGORIES, "").toLowerCase(Locale.ROOT);
        }
    }
}
