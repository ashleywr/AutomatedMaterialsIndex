package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SwemCompat {
    private static final String MOD_ID = "swem";

    private SwemCompat() {
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
            meta.put("swemItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "swem_" + kind);
        }
        applyKindMetadata(kind, meta);
        if (!facts.isEmpty()) {
            meta.put("swemFacts", CompatMetaUtil.join(facts));
            for (String fact : facts) {
                CompatMetaUtil.addSearchToken(meta, fact);
                CompatMetaUtil.addSearchToken(meta, "swem_" + fact);
            }
        }
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (CompatMetaUtil.containsAny(context.itemClass, "TackItem", "SaddlebagItem")) facts.add("horse_tack");
        if (CompatMetaUtil.containsAny(context.itemClass, "CoinItem")) facts.add("currency");
        if (CompatMetaUtil.containsAny(context.itemClass, "HorseXPPotion", "PotionItem")) facts.add("horse_potion");
        if (CompatMetaUtil.containsAny(context.itemClass, "FlySprayItem", "DesensitizingItem", "Booster",
                "WhistleItem", "BreedingToken", "VetCheckBag")) facts.add("horse_care");
        if (CompatMetaUtil.containsAny(context.itemClass, "FeedItem", "GrainFeedItem", "ScoopFeedItem", "ShavingsItem")) facts.add("horse_feed");
        if (CompatMetaUtil.containsAny(context.itemClass, "EggJumpItem", "TackBoxBlockItem", "ConeBlockItem")) facts.add("stable_equipment");
        if (CompatMetaUtil.containsAny(context.itemClass, "RidingHelmet", "CowboyHat")) facts.add("head_armor");
        if (CompatMetaUtil.containsAny(context.itemClass, "RidingBoots")) facts.add("feet_armor");
        if (CompatMetaUtil.containsAny(context.blockClass, "GrainBinBlock")) facts.add("horse_feed");
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        for (String tag : CompatMetaUtil.splitCsv(context.tags)) {
            if (tag.startsWith("swem:") && CompatMetaUtil.containsAny(tag,
                    "saddles", "blankets", "bridles", "girth_straps", "leg_wraps",
                    "breast_collars", "halters", "saddle_bags")) facts.add("horse_tack");
            if (tag.startsWith("swem:") && tag.contains("horse_armors")) facts.add("horse_armor");
            if (tag.startsWith("swem:") && tag.contains("riding_boots")) facts.add("feet_armor");
            if (tag.startsWith("swem:") && tag.contains("toys")) facts.add("horse_toy");
            if (tag.startsWith("swem:") && CompatMetaUtil.containsAny(tag,
                    "grain_bins", "slow_feeders", "grain_feeders", "half_barrels")) facts.add("horse_feed");
            if (tag.startsWith("swem:") && CompatMetaUtil.containsAny(tag,
                    "wheelbarrows", "tack_boxes")) facts.add("stable_equipment");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = context.path;
        if (path.contains("horse_armor")) facts.add("horse_armor");
        if (CompatMetaUtil.containsAny(path, "plate_", "rivet_", "_plate", "_rivet")) facts.add("metal_component");
        if (path.contains("shield_")) facts.add("shield_part");
        if (CompatMetaUtil.containsAny(path, "bushel", "feed", "sugar_cube", "sweet_feed")) facts.add("horse_feed");
        if (CompatMetaUtil.containsAny(path, "bin_grain", "slow_feeder", "grain_feeder", "half_barrel")) facts.add("horse_feed");
        if (CompatMetaUtil.containsAny(path, "poop", "manure")) facts.add("organic_drop");
        if (CompatMetaUtil.containsAny(path, "stall_horse", "pasture_", "tack_box", "wheelbarrow", "jump_xc")
                || path.startsWith("spawn_structure")
                || path.startsWith("spawn_structures")) facts.add("stable_equipment");
        if (CompatMetaUtil.containsAny(path, "offering", "cantazarite", "star_worm")) facts.add("magic_reagent");
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("horse_armor")) return "horse_armor";
        if (facts.contains("horse_tack")) return "horse_tack";
        if (facts.contains("head_armor")) return "head_armor";
        if (facts.contains("feet_armor")) return "feet_armor";
        if (facts.contains("currency")) return "currency";
        if (facts.contains("horse_feed")) return "horse_feed";
        if (facts.contains("stable_equipment")) return "stable_equipment";
        if (facts.contains("metal_component") || facts.contains("shield_part")) return "components";
        if (facts.contains("magic_reagent")) return "magic_reagents";
        if (facts.contains("horse_potion") || facts.contains("horse_care") || facts.contains("horse_toy")) return "horse_care";
        if (facts.contains("organic_drop")) return "organic_drops";
        return "";
    }

    private static void applyKindMetadata(String kind, Map<String, String> meta) {
        switch (kind) {
            case "horse_armor" -> CompatMetaUtil.addFacet(meta, ItemFacet.ARMOR_ANIMAL);
            case "horse_tack", "horse_care" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_MISC);
                route(meta, "horse_tack".equals(kind) ? "tack" : "care");
            }
            case "stable_equipment" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_MISC);
                route(meta, "stable");
            }
            case "head_armor" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.EQUIPPABLE);
                CompatMetaUtil.addFacet(meta, ItemFacet.ARMOR_HEAD);
                route(meta, "riding_gear");
            }
            case "feet_armor" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.EQUIPPABLE);
                CompatMetaUtil.addFacet(meta, ItemFacet.ARMOR_FEET);
                route(meta, "riding_gear");
            }
            case "currency" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_CURRENCY);
                route(meta, "care");
            }
            case "horse_feed" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.EDIBLE);
                CompatMetaUtil.addFacet(meta, ItemFacet.FOOD_PROTEIN);
                route(meta, "feed");
            }
            case "components" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.TECH_COMPONENT);
                CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_MINERAL);
            }
            case "magic_reagents" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_REAGENT);
            case "organic_drops" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_ORGANIC);
                route(meta, "care");
            }
            default -> {
            }
        }
        if ("horse_armor".equals(kind)) {
            route(meta, "horse_armor");
        }
    }

    private static void route(Map<String, String> meta, String subcategory) {
        meta.put(SearchNodeKeys.COMPAT_ROUTE_CATEGORY, MOD_ID);
        meta.put(SearchNodeKeys.COMPAT_ROUTE_SUBCATEGORY, subcategory);
    }

    private static final class Context {
        final String path;
        final String itemClass;
        final String blockClass;
        final String tags;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.blockClass = meta.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
        }
    }
}
