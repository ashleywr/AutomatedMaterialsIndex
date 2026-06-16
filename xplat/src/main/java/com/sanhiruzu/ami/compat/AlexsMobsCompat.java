package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class AlexsMobsCompat {
    private static final String MOD_ID = "alexsmobs";

    private static final Set<String> ORGANIC_DROP_PATHS = Set.of(
            "bear_fur", "gazelle_horn", "blood_sac", "mosquito_proboscis", "rattlesnake_rattle",
            "komodo_spit", "centipede_leg", "moose_antler", "raccoon_tail", "cockroach_wing_fragment",
            "cockroach_wing", "soul_heart", "guster_eye", "warped_muscle", "hemolymph_sac",
            "warped_mixture", "straddlite", "dropbear_claw", "ambergris", "cachalot_whale_tooth",
            "falconry_hood", "tarantula_hawk_wing_fragment", "tarantula_hawk_wing",
            "void_worm_mandible", "void_worm_eye", "serrated_shark_tooth", "froststalker_horn",
            "shark_tooth", "shed_snake_skin", "mungal_spores", "bison_fur", "lost_tentacle", "farseer_arm",
            "skreecher_soul", "elastic_tendon"
    );

    private static final Set<String> PROTEIN_FOOD_PATHS = Set.of(
            "maggot", "lobster_tail", "cooked_lobster_tail", "mosquito_larva", "blobfish",
            "leafcutter_ant_pupa", "raw_catfish", "cooked_catfish"
    );

    private AlexsMobsCompat() {
    }

    public static void enrichItem(Identifier id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }
        if (meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()) {
            CompatFamilyDetector.detect(id, meta);
        }
        if (!isAlexsMobsItem(id, meta)) {
            return;
        }

        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addClassFacts(context, facts);
        addTagFacts(context, facts);
        addPathFacts(context, facts);

        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put(SearchNodeKeys.ALEXS_MOBS_ITEM_KIND, kind);
            addSearchToken(meta, "alexsmobs_" + kind);
        }
        applyKindMetadata(kind, meta);
        if (!facts.isEmpty()) {
            meta.put(SearchNodeKeys.ALEXS_MOBS_FACTS, join(facts));
            for (String fact : facts) {
                addSearchToken(meta, fact);
                addSearchToken(meta, "alexsmobs_" + fact);
            }
        }
    }

    private static boolean isAlexsMobsItem(Identifier id, Map<String, String> meta) {
        return MOD_ID.equals(id.getNamespace())
                || CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.ALEXS_MOBS);
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (containsAny(context.itemClass, "ItemInventoryOnly", "ItemTabIcon", "ItemBearDust")) {
            facts.add("internal_or_debug");
        }
        if (containsAny(context.itemClass, "ItemEcholocator")) {
            facts.add("navigation_tool");
        }
        if (containsAny(context.itemClass, "ItemBloodSprayer", "ItemHemolymphBlaster", "ItemStinkRay")) {
            facts.add("ranged_weapon");
        }
        if (containsAny(context.itemClass, "ItemPocketSand")) {
            facts.add("projectile");
        }
        if (containsAny(context.itemClass, "ItemStraddleboard")) {
            facts.add("transport");
        }
        if (containsAny(context.itemClass, "ItemPigshoes")) {
            facts.add("feet_armor");
        }
        if (containsAny(context.itemClass, "ItemAnimalEgg")) {
            facts.add("organic_drop");
        }
        if (containsAny(context.itemClass, "ItemLeafcutterPupa")) {
            facts.add("protein_food");
        }
        if (containsAny(context.itemClass, "ItemShieldOfTheDeep", "ItemMaraca", "ItemFalconryGlove",
                "ItemMysteriousWorm", "ItemDimensionalCarver", "ItemShatteredDimensionalCarver",
                "ItemFlutterPot", "ItemSquidGrapple")) {
            facts.add("utility_item");
        }
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        if (hasToken(context.tags, "alexsmobs:animal_dictionary_ingredient")
                || hasToken(context.tags, "alexsmobs:void_worm_drops")) {
            facts.add("organic_drop");
        }
        if (hasToken(context.tags, "minecraft:fishes")
                || hasAlexsMobsTagEnding(context.tags, "_foodstuffs")
                || hasAlexsMobsTagEnding(context.tags, "_breedables")
                || hasAlexsMobsTagEnding(context.tags, "_tameables")
                || hasAlexsMobsTagEnding(context.tags, "_offerings")
                || hasToken(context.tags, "alexsmobs:insect_items")) {
            facts.add("protein_food");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        if (ORGANIC_DROP_PATHS.contains(context.path)) {
            facts.add("organic_drop");
        }
        if (PROTEIN_FOOD_PATHS.contains(context.path)) {
            facts.add("protein_food");
        }
        if (context.path.endsWith("_locator") || context.path.equals("echolocator") || context.path.equals("endolocator")) {
            facts.add("navigation_tool");
        }
        if (context.path.equals("ancient_dart")) {
            facts.add("projectile");
        }
        if (context.path.equals("chorus_on_a_stick")) {
            facts.add("protein_food");
        }
        if (context.path.equals("mimicream")) {
            facts.add("organic_drop");
        }
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("feet_armor")) return "feet_armor";
        if (facts.contains("ranged_weapon")) return "ranged_weapons";
        if (facts.contains("projectile")) return "projectiles";
        if (facts.contains("transport")) return "transport";
        if (facts.contains("navigation_tool")) return "navigation_tools";
        if (facts.contains("protein_food")) return "protein_foods";
        if (facts.contains("organic_drop")) return "organic_drops";
        if (facts.contains("utility_item")) return "utility_items";
        if (facts.contains("internal_or_debug")) return "internal_items";
        return "";
    }

    private static void applyKindMetadata(String kind, Map<String, String> meta) {
        switch (kind) {
            case "organic_drops" -> addFacet(meta, ItemFacet.INGREDIENT_ORGANIC);
            case "protein_foods" -> addFacet(meta, ItemFacet.FOOD_PROTEIN);
            case "navigation_tools" -> addFacet(meta, ItemFacet.UTILITY_NAVIGATION);
            case "utility_items", "internal_items" -> addFacet(meta, ItemFacet.UTILITY_MISC);
            case "ranged_weapons" -> addFacet(meta, ItemFacet.RANGED_WEAPON);
            case "projectiles" -> addFacet(meta, ItemFacet.PROJECTILE);
            case "transport" -> addFacet(meta, ItemFacet.TRANSPORT);
            case "feet_armor" -> {
                addFacet(meta, ItemFacet.EQUIPPABLE);
                addFacet(meta, ItemFacet.ARMOR_FEET);
            }
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

    private static boolean hasAlexsMobsTagEnding(String csv, String suffix) {
        for (String value : splitCsv(csv)) {
            if (value.startsWith(MOD_ID + ":") && value.endsWith(suffix)) {
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

        Context(Identifier id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
        }
    }
}
