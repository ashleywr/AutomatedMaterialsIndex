package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class MnaCompat {
    private static final String MOD_ID = "mna";

    private MnaCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }
        if (meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()) {
            CompatFamilyDetector.detect(id, meta);
        }
        if (!isMnaItem(id, meta)) {
            return;
        }

        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addClassFacts(context, facts);
        addTagFacts(context, facts);
        addPathFacts(context, facts);

        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put(SearchNodeKeys.MNA_ITEM_KIND, kind);
            addSearchToken(meta, "mna_" + kind);
        }
        applyKindMetadata(kind, meta);
        if (!facts.isEmpty()) {
            meta.put(SearchNodeKeys.MNA_FACTS, join(facts));
            for (String fact : facts) {
                addSearchToken(meta, fact);
                addSearchToken(meta, "mna_" + fact);
            }
        }
    }

    private static boolean isMnaItem(ResourceLocation id, Map<String, String> meta) {
        return MOD_ID.equals(id.getNamespace()) || CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.MNA);
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (containsAny(context.itemClass, ".items.constructs.parts.", "ConstructPart")) {
            facts.add("construct_part");
        }
        if (containsAny(context.itemClass, ".items.ritual.Mote")) {
            facts.add("mote");
        }
        if (containsAny(context.itemClass, "PractitionersPatch")) {
            facts.add("ritual_patch");
        }
        if (containsAny(context.itemClass, "RunePattern", ".items.runes.Rune", "StoneRune")) {
            facts.add("rune");
        }
        if (containsAny(context.itemClass, "AnimusDust", "Vellum", "EnchantedVellum", "ItemManaGem",
                "SightUnguent")) {
            facts.add("magic_reagent");
        }
        if (containsAny(context.itemClass, "PatterningPrism", "EnderDisk", "Manifest", "HealingPoultice",
                "ItemFactionHorn", "ThaumaturgicLink", "DowsingRod", "ItemTransitoryTunnel",
                "RunicMalus", "ScrollOfIcarianFlight", "BoundShield", "BellOfBidding",
                "ConstructRepairKit", "WizardChalk")) {
            facts.add("magic_artifact");
        }
        if (containsAny(context.itemClass, "HellfireTrident", "Shuriken", "AstroBlade")) {
            facts.add("weapon");
        }
        if (containsAny(context.itemClass, "MagicStaff")) {
            facts.add("ranged_weapon");
        }
        if (containsAny(context.itemClass, "ItemMagicBroom")) {
            facts.add("transport");
        }
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        if (hasToken(context.tags, "mna:lesser_motes") || hasToken(context.tags, "mna:greater_motes")) {
            facts.add("mote");
        }
        if (hasToken(context.tags, "mna:runes") || hasToken(context.tags, "mna:stone_runes")) {
            facts.add("rune");
        }
        if (hasToken(context.tags, "mna:staves") || hasToken(context.tags, "mna:wands")) {
            facts.add("ranged_weapon");
        }
        if (hasToken(context.tags, "mna:generated_spell_items")
                || hasToken(context.tags, "mna:alteration_items")
                || hasToken(context.tags, "mna:relics")) {
            facts.add("magic_artifact");
        }
        if (containsTagPrefix(context.tags, "mna:dusts/")
                || hasToken(context.tags, "mna:chimerite_crystals")) {
            facts.add("magic_reagent");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        if (context.path.startsWith("patch_")) {
            facts.add("ritual_patch");
        }
        if (context.path.contains("mote")) {
            facts.add("mote");
        }
        if (startsWithAny(context.path, "sachet_", "mark_of_", "ritual_focus_")
                || containsAny(context.path, "eldrin_rift", "mundane_bracelet", "mundane_amulet",
                "animated_quill", "wizard_chalk")) {
            facts.add("magic_artifact");
        }
        if (containsAny(context.path, "animus_dust", "vellum", "mana_gem", "sight_unguent",
                "living_flame", "resonating_lump", "resonating_dust")) {
            facts.add("magic_reagent");
        }
        if (containsAny(context.path, "raw_vinteum", "transmuted_silver", "quicksilver",
                "vinteum_coated_iron", "superheated_vinteum_ingot", "purified_vinteum_coated_iron",
                "superheated_purified_vinteum_ingot", "runic_silk", "infused_silk", "ironbark",
                "witherbone")) {
            facts.add("material");
        }
        if (containsAny(context.path, "runesmith_chisel", "sorcerous_sewing_set", "vinteum_needle")) {
            facts.add("utility_tool");
        }
        if (context.path.endsWith("_hud_badge_item")) {
            facts.add("utility");
        }
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("construct_part")) return "construct_parts";
        if (facts.contains("ritual_patch")) return "ritual_patches";
        if (facts.contains("mote")) return "motes";
        if (facts.contains("rune")) return "runes";
        if (facts.contains("ranged_weapon")) return "ranged_weapons";
        if (facts.contains("weapon")) return "weapons";
        if (facts.contains("transport")) return "transport";
        if (facts.contains("utility_tool")) return "tools";
        if (facts.contains("utility")) return "utility";
        if (facts.contains("material")) return "materials";
        if (facts.contains("magic_artifact")) return "artifacts";
        if (facts.contains("magic_reagent")) return "reagents";
        return "";
    }

    private static void applyKindMetadata(String kind, Map<String, String> meta) {
        switch (kind) {
            case "construct_parts" -> {
                addFacet(meta, ItemFacet.TECH_COMPONENT);
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_FAMILY, MOD_ID + ":construct_parts");
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_LABEL, "Construct Parts");
                meta.putIfAbsent(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
            }
            case "ritual_patches" -> {
                addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
                addFacet(meta, ItemFacet.UPGRADE);
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_FAMILY, MOD_ID + ":ritual_patches");
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_LABEL, "Practitioner Patches");
                meta.putIfAbsent(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
            }
            case "motes", "runes" -> addFacet(meta, ItemFacet.MAGIC_REAGENT);
            case "reagents" -> addFacet(meta, ItemFacet.MAGIC_REAGENT);
            case "artifacts" -> addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
            case "materials" -> addFacet(meta, ItemFacet.INGREDIENT_MINERAL);
            case "tools" -> addFacet(meta, ItemFacet.UTILITY_TOOL);
            case "utility" -> addFacet(meta, ItemFacet.UTILITY_MISC);
            case "weapons" -> addFacet(meta, ItemFacet.MELEE_WEAPON);
            case "ranged_weapons" -> addFacet(meta, ItemFacet.RANGED_WEAPON);
            case "transport" -> addFacet(meta, ItemFacet.TRANSPORT);
            default -> {
            }
        }
    }

    private static boolean startsWithAny(String value, String... prefixes) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String prefix : prefixes) {
            if (value.startsWith(prefix.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
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

    private static boolean containsTagPrefix(String csv, String prefix) {
        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        if (normalizedPrefix.isBlank()) {
            return false;
        }
        for (String value : splitCsv(csv)) {
            if (value.startsWith(normalizedPrefix)) {
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
