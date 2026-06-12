package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class AlexsCavesCompat {
    private static final String MOD_ID = "alexscaves";

    private static final Set<String> MATERIAL_PATHS = Set.of(
            "raw_scarlet_neodymium", "raw_azure_neodymium",
            "scarlet_neodymium_ingot", "azure_neodymium_ingot"
    );
    private static final Set<String> TECH_PART_PATHS = Set.of(
            "telecore", "notor_gizmo", "heavyweight", "toxic_paste", "polymer_plate",
            "fissile_core", "charred_remnant"
    );
    private static final Set<String> PROTEIN_FOOD_PATHS = Set.of(
            "cooked_trilocaris_tail", "cooked_radgill", "cooked_lanternfish",
            "cooked_tripodfish", "cooked_mussel"
    );
    private static final Set<String> SNACK_PATHS = Set.of(
            "spelunkie", "slam", "gelatin_red", "gelatin_green", "gelatin_blue",
            "gelatin_yellow", "gelatin_pink", "vanilla_ice_cream_scoop",
            "sweetberry_ice_cream_scoop", "gumball_pile", "caramel", "sweet_tooth",
            "sack_of_sating"
    );
    private static final Set<String> ORGANIC_PATHS = Set.of(
            "amber_curiosity", "dinosaur_nugget", "marine_snow", "guano", "dark_tatters",
            "corrodent_teeth", "vesper_wing", "shadow_silk", "immortal_embryo"
    );
    private static final Set<String> MAGIC_REAGENT_PATHS = Set.of(
            "ominous_catalyst", "bioluminesscence", "moth_dust", "occult_gem", "pure_darkness"
    );

    private AlexsCavesCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }
        if (meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()) {
            CompatFamilyDetector.detect(id, meta);
        }
        if (!isAlexsCavesItem(id, meta)) {
            return;
        }

        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addClassFacts(context, facts);
        addTagFacts(context, facts);
        addPathFacts(context, facts);

        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put(SearchNodeKeys.ALEXS_CAVES_ITEM_KIND, kind);
            addSearchToken(meta, "alexscaves_" + kind);
        }
        applyKindMetadata(kind, meta);
        if (!facts.isEmpty()) {
            meta.put(SearchNodeKeys.ALEXS_CAVES_FACTS, join(facts));
            for (String fact : facts) {
                addSearchToken(meta, fact);
                addSearchToken(meta, "alexscaves_" + fact);
            }
        }
    }

    private static boolean isAlexsCavesItem(ResourceLocation id, Map<String, String> meta) {
        return MOD_ID.equals(id.getNamespace())
                || CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.ALEXS_CAVES);
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (containsAny(context.itemClass, "CaveInfoItem")) {
            facts.add("cave_info");
        }
        if (containsAny(context.itemClass, "QuarrySmasherItem")) {
            facts.add("harvest_tool");
        }
        if (containsAny(context.itemClass, "HolocoderItem", "RemoteDetonatorItem", "FloaterItem",
                "FertilizerItem")) {
            facts.add("utility_item");
        }
        if (containsAny(context.itemClass, "SubmarineItem")) {
            facts.add("transport");
        }
        if (containsAny(context.itemClass, "ThrownProjectileItem", "ShotGumItem")) {
            facts.add("projectile");
        }
        if (containsAny(context.itemClass, "RadiationRemovingFoodItem", "SackOfSatingItem")) {
            facts.add("snack_food");
        }
        if (containsAny(context.itemClass, "JellyBeanItem")) {
            facts.add("snack_food");
        }
        if (containsAny(context.itemClass, "RadioactiveItem")) {
            facts.add("tech_part");
        }
        if (containsAny(context.itemClass, "MothDustItem", "OccultGemItem")) {
            facts.add("magic_reagent");
        }
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        if (hasToken(context.tags, "minecraft:fishes")) {
            facts.add("protein_food");
        }
        if (hasToken(context.tags, "alexscaves:gelatins") || hasToken(context.tags, "alexscaves:gummy_items")) {
            facts.add("snack_food");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = basePath(context.path);
        if (path.equals("advancement_tab_icon") || path.equals("game_controller")) {
            facts.add("internal_or_debug");
        }
        if (path.contains("dreadbow")) {
            facts.add("ranged_weapon");
        }
        if (path.contains("ortholance")) {
            facts.add("melee_weapon");
        }
        if (MATERIAL_PATHS.contains(path)) {
            facts.add("material");
        }
        if (TECH_PART_PATHS.contains(path)) {
            facts.add("tech_part");
        }
        if (PROTEIN_FOOD_PATHS.contains(path)) {
            facts.add("protein_food");
        }
        if (SNACK_PATHS.contains(path)) {
            facts.add("snack_food");
        }
        if (ORGANIC_PATHS.contains(path)) {
            facts.add("organic_drop");
        }
        if (MAGIC_REAGENT_PATHS.contains(path)) {
            facts.add("magic_reagent");
        }
        if (path.equals("peppermint_powder")) {
            facts.add("ingredient");
        }
        if (path.equals("sulfur_dust") || path.equals("sulphur_dust")) {
            facts.add("mineral_dust");
        }
        if (path.equals("depth_charge") || path.equals("cinder_brick")) {
            facts.add("projectile");
        }
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("cave_info")) return "guide_items";
        if (facts.contains("ranged_weapon")) return "ranged_weapons";
        if (facts.contains("melee_weapon")) return "weapons";
        if (facts.contains("harvest_tool")) return "harvest_tools";
        if (facts.contains("projectile")) return "projectiles";
        if (facts.contains("transport")) return "transport";
        if (facts.contains("material")) return "materials";
        if (facts.contains("tech_part")) return "tech_parts";
        if (facts.contains("magic_reagent")) return "magic_reagents";
        if (facts.contains("mineral_dust")) return "mineral_dusts";
        if (facts.contains("protein_food")) return "protein_foods";
        if (facts.contains("snack_food")) return "snacks";
        if (facts.contains("organic_drop")) return "organic_drops";
        if (facts.contains("ingredient")) return "ingredients";
        if (facts.contains("utility_item")) return "utility_items";
        if (facts.contains("internal_or_debug")) return "internal_items";
        return "";
    }

    private static void applyKindMetadata(String kind, Map<String, String> meta) {
        switch (kind) {
            case "guide_items" -> {
                addFacet(meta, ItemFacet.BOOK);
                addFacet(meta, ItemFacet.GUIDE_BOOK);
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_FAMILY, MOD_ID + ":cave_info");
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_LABEL, "Cave Info");
                meta.putIfAbsent(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
            }
            case "materials" -> addFacet(meta, ItemFacet.INGOT);
            case "tech_parts" -> addFacet(meta, ItemFacet.TECH_COMPONENT);
            case "magic_reagents" -> addFacet(meta, ItemFacet.MAGIC_REAGENT);
            case "mineral_dusts" -> {
                addFacet(meta, ItemFacet.DUST);
                addFacet(meta, ItemFacet.INGREDIENT_MINERAL);
            }
            case "protein_foods" -> addFacet(meta, ItemFacet.FOOD_PROTEIN);
            case "snacks" -> addFacet(meta, ItemFacet.EDIBLE);
            case "organic_drops", "ingredients" -> addFacet(meta, ItemFacet.INGREDIENT_ORGANIC);
            case "utility_items", "internal_items" -> addFacet(meta, ItemFacet.UTILITY_MISC);
            case "ranged_weapons" -> addFacet(meta, ItemFacet.RANGED_WEAPON);
            case "weapons" -> addFacet(meta, ItemFacet.MELEE_WEAPON);
            case "harvest_tools" -> addFacet(meta, ItemFacet.HARVEST_TOOL);
            case "projectiles" -> addFacet(meta, ItemFacet.PROJECTILE);
            case "transport" -> addFacet(meta, ItemFacet.TRANSPORT);
            default -> {
            }
        }
    }

    private static String basePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int variant = path.indexOf("/variant/");
        String normalized = variant >= 0 ? path.substring(0, variant) : path;
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
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
        final String tags;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
        }
    }
}
