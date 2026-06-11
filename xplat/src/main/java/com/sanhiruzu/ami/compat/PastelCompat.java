package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PastelCompat {
    private static final String MOD_ID = "pastel";

    private PastelCompat() {
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
            meta.put("pastelItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "pastel_" + kind);
        }
        applyKindMetadata(kind, context, meta);
        if (!facts.isEmpty()) {
            meta.put("pastelFacts", CompatMetaUtil.join(facts));
        }
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (CompatMetaUtil.containsAny(context.itemClass, "StructurePlacerItem")) facts.add("structure_placer");
        if (CompatMetaUtil.containsAny(context.itemClass, "BeverageItem", "DrinkItem", "JadeWineItem")) facts.add("drink");
        if (CompatMetaUtil.containsAny(context.itemClass, "InkFlaskItem")) facts.add("ink_container");
        if (CompatMetaUtil.containsAny(context.itemClass, "PigmentItem")) facts.add("pigment");
        if (CompatMetaUtil.containsAny(context.itemClass, "UpgradeBlockItem")) facts.add("node_upgrade");
        if (CompatMetaUtil.containsAny(context.itemClass, "GemstonePowderItem", "FloatItem")) facts.add("mineral_resource");
        if (CompatMetaUtil.containsAny(context.itemClass, "trinkets", "GemstoneArmorItem")) facts.add("curio_or_armor");
        if (CompatMetaUtil.containsAny(context.itemClass, "CookbookItem")) facts.add("guide_book");
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        for (String tag : CompatMetaUtil.splitCsv(context.tags)) {
            if (tag.startsWith("pastel:pigments")) facts.add("pigment");
            if (tag.startsWith("pastel:ink_containers")) facts.add("ink_container");
            if (tag.startsWith("pastel:pastel_node_upgrades")) facts.add("node_upgrade");
            if (tag.startsWith("pastel:pure_resources") || tag.startsWith("pastel:ores")) facts.add("mineral_resource");
            if (tag.startsWith("pastel:trinkets")) facts.add("curio_or_armor");
            if (tag.startsWith("pastel:drinkable_spirits") || tag.startsWith("pastel:alcohols")) facts.add("drink");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = CompatMetaUtil.basePath(context.path);
        if (path.endsWith("structure_placer")) facts.add("structure_placer");
        if (path.startsWith("pure_") || path.startsWith("raw_")
                || CompatMetaUtil.containsAny(path, "gem", "fragments", "flake", "chunk", "shard")) facts.add("mineral_resource");
        if (CompatMetaUtil.containsAny(path, "petal", "orchid")) facts.add("organic_reagent");
        if (CompatMetaUtil.containsAny(path, "pigment", "dye")) facts.add("pigment");
        if (path.contains("ink_flask")) facts.add("ink_container");
        if (path.contains("midnight_aberration")) facts.add("magic_artifact");
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("guide_book")) return "guide_books";
        if (facts.contains("structure_placer")) return "structure_placers";
        if (facts.contains("node_upgrade")) return "node_upgrades";
        if (facts.contains("ink_container")) return "ink_containers";
        if (facts.contains("pigment")) return "pigments";
        if (facts.contains("drink")) return "drinks";
        if (facts.contains("curio_or_armor")) return "equipment";
        if (facts.contains("magic_artifact")) return "artifacts";
        if (facts.contains("organic_reagent")) return "organic_reagents";
        if (facts.contains("mineral_resource")) return "resources";
        return "";
    }

    private static void applyKindMetadata(String kind, Context context, Map<String, String> meta) {
        switch (kind) {
            case "guide_books" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.BOOK);
                CompatMetaUtil.addFacet(meta, ItemFacet.GUIDE_BOOK);
                route(meta, "progression");
            }
            case "structure_placers" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_MISC);
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_FAMILY, MOD_ID + ":structure_placers");
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_LABEL, "Structure Placers");
                meta.putIfAbsent(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
                route(meta, "structures");
            }
            case "node_upgrades" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.UPGRADE);
                CompatMetaUtil.addFacet(meta, ItemFacet.TECH_COMPONENT);
                route(meta, "progression");
            }
            case "ink_containers" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.FLUID_CONTAINER);
                route(meta, "pigments");
            }
            case "pigments" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_DYE);
                route(meta, "pigments");
            }
            case "drinks" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.EDIBLE);
                CompatMetaUtil.addFacet(meta, ItemFacet.FOOD_DRINK);
                route(meta, "drinks");
            }
            case "equipment" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.CURIO);
                route(meta, "equipment");
            }
            case "artifacts" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
                route(meta, "magic");
            }
            case "organic_reagents" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_ORGANIC);
                route(meta, "magic");
            }
            case "resources" -> {
                if (context.path.contains("raw_")) CompatMetaUtil.addFacet(meta, ItemFacet.RAW_MATERIAL);
                else CompatMetaUtil.addFacet(meta, ItemFacet.GEM);
                CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_MINERAL);
                route(meta, "resources");
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

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
        }
    }
}
