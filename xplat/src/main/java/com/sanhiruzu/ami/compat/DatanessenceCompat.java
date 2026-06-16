package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.ItemFacet;
import net.minecraft.resources.Identifier;

import java.util.Locale;
import java.util.Map;

public final class DatanessenceCompat {
    private static final String MOD_ID = "datanessence";
    private static final String FAMILY = "halcyon";

    private DatanessenceCompat() {
    }

    public static void enrichItem(Identifier id, Map<String, String> meta) {
        if (id == null || meta == null || !MOD_ID.equals(id.getNamespace())) {
            return;
        }

        meta.putIfAbsent(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, FAMILY);
        meta.putIfAbsent(SearchNodeKeys.COMPAT_FAMILIES, FAMILY);
        meta.put(SearchNodeKeys.COMPAT_CATEGORY_POLICY, "focused");
        markGuideBookCandidate(id, meta);
        applyClassFacts(id, meta);
        meta.put("halcyonItemKind", classifyKind(id, meta));
        meta.put(SearchNodeKeys.COMPAT_ROUTE_CATEGORY, FAMILY);
        meta.put(SearchNodeKeys.COMPAT_ROUTE_SUBCATEGORY, routeSubcategory(id, meta));
        CompatMetaUtil.addSearchToken(meta, "halcyon");
    }

    private static void markGuideBookCandidate(Identifier id, Map<String, String> meta) {
        String path = id.getPath().toLowerCase(Locale.ROOT);
        if (!path.contains("book") && !path.contains("guide") && !path.contains("manual")
                && !path.contains("codex") && !path.contains("lexicon")) {
            return;
        }

        CompatMetaUtil.addFacet(meta, ItemFacet.BOOK);
        CompatMetaUtil.addFacet(meta, ItemFacet.GUIDE_BOOK);
        meta.put(SearchNodeKeys.GUIDE_BOOK_CANDIDATE, "true");
        CompatMetaUtil.addSearchToken(meta, "guidebook");
        CompatMetaUtil.addSearchToken(meta, "guide");
    }

    private static void applyClassFacts(Identifier id, Map<String, String> meta) {
        String path = id.getPath().toLowerCase(Locale.ROOT);
        String itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT);
        String blockClass = meta.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "").toLowerCase(Locale.ROOT);
        if (containsAny(itemClass, "essencesword")) {
            CompatMetaUtil.addFacet(meta, ItemFacet.MELEE_WEAPON);
        }
        if (containsAny(itemClass, "shrinkray")) {
            CompatMetaUtil.addFacet(meta, ItemFacet.RANGED_WEAPON);
        }
        if (containsAny(itemClass, "essencebombitem")) {
            CompatMetaUtil.addFacet(meta, ItemFacet.PROJECTILE);
            CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
        }
        if (containsAny(itemClass, "grapplinghook", "warpcapsule")) {
            CompatMetaUtil.addFacet(meta, ItemFacet.TRANSPORT);
        }
        if (containsAny(itemClass, "hammerandchisel", "orescanner", "thermometer", "locator",
                "essencemeter", "illuminationrod", "repulsionrod", "fallingmoon", "datatablet")
                || path.endsWith("_rod")) {
            CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_TOOL);
        }
        if (containsAny(itemClass, "antigravitypack", "traversitetrudgers")) {
            CompatMetaUtil.addFacet(meta, ItemFacet.EQUIPPABLE);
            CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_MISC);
        }
        if (containsAny(itemClass, "datadrive", "musicdiscplayer")) {
            CompatMetaUtil.addFacet(meta, ItemFacet.STORAGE);
        }
        if (containsAny(itemClass, "speednodeupgrade", "locatorupgrade", "filterupgrade")) {
            CompatMetaUtil.addFacet(meta, ItemFacet.UPGRADE);
        }
        if (containsAny(itemClass, "essenceshard") || containsAny(blockClass, "essence")) {
            CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_REAGENT);
        }
        if (path.contains("lens")) {
            CompatMetaUtil.addFacet(meta, ItemFacet.UPGRADE);
        }
    }

    private static String classifyKind(Identifier id, Map<String, String> meta) {
        String path = id.getPath().toLowerCase(Locale.ROOT);
        String facets = meta.getOrDefault(SearchNodeKeys.FACETS, "").toLowerCase(Locale.ROOT);
        String category = meta.getOrDefault(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
        String subcategory = meta.getOrDefault(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");

        if (hasToken(facets, "guide_book") || hasToken(facets, "book") || "books".equals(subcategory)) return "books";
        if (hasToken(facets, "transport") || "transport".equals(subcategory)) return "transport";
        if (hasToken(facets, "cable") || "cables".equals(subcategory) || path.endsWith("_wire")) return "cables";
        if (hasToken(facets, "machine") || hasToken(facets, "has_block_entity")
                || hasToken(facets, "storage") || "machines".equals(subcategory)) return "machines";
        if (hasToken(facets, "upgrade") || path.contains("upgrade")) return "upgrades";
        if (hasToken(facets, "template") || "templates".equals(subcategory) || path.contains("mold")) return "templates";
        if (hasToken(facets, "utility_tool") || "tools".equals(category)) return "tools";
        if ("armor".equals(category)
                || hasToken(facets, "melee_weapon")
                || hasToken(facets, "ranged_weapon")
                || hasToken(facets, "projectile")
                || hasToken(facets, "equippable")) return "equipment";
        if (hasToken(facets, "tech_component") || hasToken(facets, "mechanical_component") || "parts".equals(subcategory)) {
            return "parts";
        }
        if ("magic".equals(category) || hasToken(facets, "magic_reagent")) return "essence";
        if ("ingredients".equals(category)) return "materials";
        return "items";
    }

    private static String routeSubcategory(Identifier id, Map<String, String> meta) {
        return switch (classifyKind(id, meta)) {
            case "transport" -> "transport";
            case "cables" -> "cables";
            case "machines" -> "machines";
            case "upgrades" -> "upgrades";
            case "templates" -> "templates";
            case "tools" -> "tools";
            case "equipment" -> "equipment";
            case "parts" -> "parts";
            case "books" -> "books";
            case "essence" -> "essence";
            case "materials" -> "materials";
            default -> "items";
        };
    }

    private static boolean hasToken(String csv, String token) {
        for (String value : CompatMetaUtil.splitCsv(csv)) {
            if (token.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
