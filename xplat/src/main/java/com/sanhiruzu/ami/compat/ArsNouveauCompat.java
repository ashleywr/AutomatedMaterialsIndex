package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class ArsNouveauCompat {
    private static final String MOD_ID = "ars_nouveau";

    private ArsNouveauCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }
        if (meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()) {
            CompatFamilyDetector.detect(id, meta);
        }
        if (!isArsNouveauItem(id, meta)) {
            return;
        }

        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addClassFacts(context, facts);
        addPathFacts(context, facts);
        addRecipeFacts(context, facts);

        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put(SearchNodeKeys.ARS_NOUVEAU_ITEM_KIND, kind);
            addSearchToken(meta, "ars_nouveau_" + kind);
        }
        applyKindMetadata(kind, meta);
        if (!facts.isEmpty()) {
            meta.put(SearchNodeKeys.ARS_NOUVEAU_FACTS, join(facts));
            for (String fact : facts) {
                addSearchToken(meta, fact);
                addSearchToken(meta, "ars_nouveau_" + fact);
            }
        }
    }

    private static boolean isArsNouveauItem(ResourceLocation id, Map<String, String> meta) {
        return MOD_ID.equals(id.getNamespace()) || CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.ARS_NOUVEAU);
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (containsAny(context.itemClass, ".items.Glyph")) {
            facts.add("glyph");
            facts.add("spell_component");
        }
        if (containsAny(context.itemClass, "RitualTablet")) {
            facts.add("ritual_tablet");
            facts.add("ritual");
        }
        if (containsAny(context.itemClass, "SpellBook", "Wand", "SpellBow", "SpellParchment", "FormSpellArrow", "RunicChalk")) {
            facts.add("spellcasting");
        }
        if (containsAny(context.itemClass, "FamiliarScript", "summon_charm")) {
            facts.add("familiar");
        }
        if (containsAny(context.itemClass, "AnimatedMagicArmor", "PerkItem", "curios.")) {
            facts.add("equipment");
        }
        if (containsAny(context.itemClass, "AbstractEssence")) {
            facts.add("source");
            facts.add("material");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        if (context.path.startsWith("glyph_") || context.path.startsWith("glyph/") || context.path.equals("wololo")) {
            facts.add("glyph");
            facts.add("spell_component");
        }
        if (context.path.startsWith("ritual_")) {
            facts.add("ritual_tablet");
            facts.add("ritual");
        }
        if (context.path.contains("source") || context.path.contains("relay") || context.path.contains("jar")) {
            facts.add("source");
        }
        if (context.path.contains("apparatus")
                || context.path.contains("imbuement")
                || context.path.contains("scribes_table")
                || context.path.contains("enchanting_apparatus")
                || context.path.contains("repository")
                || context.path.contains("lectern")
                || context.path.contains("pedestal")
                || context.path.contains("turret")
                || context.path.contains("sensor")
                || context.path.contains("relay")) {
            facts.add("automation");
        }
        if (context.path.contains("spell_book")
                || context.path.contains("wand")
                || context.path.contains("spell_bow")
                || context.path.contains("spell_arrow")
                || context.path.contains("parchment")
                || context.path.contains("runic_chalk")) {
            facts.add("spellcasting");
        }
        if (context.path.startsWith("familiar_") || context.path.contains("charm") || context.path.contains("starbuncle")
                || context.path.contains("drygmy") || context.path.contains("wixie") || context.path.contains("whirlisprig")
                || context.path.contains("bookwyrm")) {
            facts.add("familiar");
        }
        if (context.path.contains("robe")
                || context.path.contains("hood")
                || context.path.contains("leggings")
                || context.path.contains("boots")
                || context.path.contains("ring")
                || context.path.contains("belt")
                || context.path.contains("amulet")
                || context.path.contains("trinket")) {
            facts.add("equipment");
        }
        if (context.path.contains("magebloom")
                || context.path.contains("archwood")
                || context.path.contains("wilden")
                || context.path.contains("essence")
                || context.path.contains("experience_gem")
                || context.path.contains("source_gem")) {
            facts.add("material");
        }
        if (context.path.contains("sourcestone")
                || context.path.contains("mage_block")
                || context.path.contains("weave")
                || context.path.contains("decor_")
                || context.path.endsWith("_slab")
                || context.path.endsWith("_stairs")
                || context.path.endsWith("_fence")
                || context.path.endsWith("_wall")) {
            facts.add("building");
        }
    }

    private static void addRecipeFacts(Context context, Set<String> facts) {
        if (hasToken(context.recipeCategories, "glyph") || hasToken(context.recipeUseCategories, "glyph")) {
            facts.add("glyph");
            facts.add("spell_component");
        }
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("glyph")) return "glyphs";
        if (facts.contains("ritual_tablet")) return "ritual_tablets";
        if (facts.contains("spellcasting")) return "spellcasting";
        if (facts.contains("source")) return "source";
        if (facts.contains("automation")) return "automation";
        if (facts.contains("familiar")) return "familiars";
        if (facts.contains("equipment")) return "equipment";
        if (facts.contains("material")) return "materials";
        if (facts.contains("building")) return "building";
        return "";
    }

    private static void applyKindMetadata(String kind, Map<String, String> meta) {
        switch (kind) {
            case "glyphs" -> {
                addFacet(meta, ItemFacet.MAGIC_REAGENT);
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_FAMILY, MOD_ID + ":glyphs");
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_LABEL, "Glyphs");
                meta.putIfAbsent(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
            }
            case "ritual_tablets" -> {
                addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_FAMILY, MOD_ID + ":ritual_tablets");
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_LABEL, "Ritual Tablets");
                meta.putIfAbsent(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
            }
            case "spellcasting", "source", "automation", "familiars", "equipment" -> addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
            case "materials" -> addFacet(meta, ItemFacet.MAGIC_REAGENT);
            case "building" -> addFacet(meta, ItemFacet.DECORATIVE_BLOCK);
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

    private static boolean hasToken(String csv, String token) {
        for (String value : splitCsv(csv)) {
            if (value.equals(token)) {
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
            this.recipeCategories = meta.getOrDefault(SearchNodeKeys.RECIPE_CATEGORIES, "");
            this.recipeUseCategories = meta.getOrDefault(SearchNodeKeys.RECIPE_USE_CATEGORIES, "");
        }
    }
}
