package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CompatFamilyDetector {
    public static final String COBBLEMON = "cobblemon";
    public static final String CREATE = "create";
    public static final String AE2 = "ae2";
    public static final String MEKANISM = "mekanism";
    public static final String GREGTECH = "gregtech";
    public static final String SOPHISTICATED = "sophisticated";
    public static final String MINECOLONIES = "minecolonies";
    public static final String APOTHEOSIS = "apotheosis";
    public static final String BOTANIA = "botania";
    public static final String MAPPING = "mapping";
    private static final int FAMILY_THRESHOLD = 25;
    private static final Map<String, String> MOD_METADATA_CACHE = new ConcurrentHashMap<>();

    private static final Set<String> COBBLEMON_ADDON_NAMESPACES = Set.of(
            "badgebox",
            "cobbled_counter",
            "cobblemore_lib",
            "cobblefurnies",
            "cobblemon_manufactory",
            "create_cobblemon_potion",
            "createmonballsoverhaul",
            "fightorflight",
            "mega_showdown",
            "rctmod"
    );

    private static final Set<String> CREATE_ADDON_NAMESPACES = Set.of(
            "aeronautics",
            "bellsandwhistles",
            "copycats",
            "interiors",
            "offroad",
            "railways",
            "rechiseledcreate",
            "sliceanddice"
    );

    private static final Set<String> MAPPING_NAMESPACES = Set.of(
            "xaerominimap",
            "xaeroworldmap",
            "journeymap",
            "ftbchunks"
    );

    private static final Map<String, String> EXACT_NAMESPACE_FAMILIES = Map.ofEntries(
            Map.entry("ae2", AE2),
            Map.entry("appliedenergistics2", AE2),
            Map.entry("mekanism", MEKANISM),
            Map.entry("mekanismgenerators", MEKANISM),
            Map.entry("mekanismtools", MEKANISM),
            Map.entry("mekanismadditions", MEKANISM),
            Map.entry("gtceu", GREGTECH),
            Map.entry("gregtech", GREGTECH),
            Map.entry("sophisticatedbackpacks", SOPHISTICATED),
            Map.entry("sophisticatedstorage", SOPHISTICATED),
            Map.entry("minecolonies", MINECOLONIES),
            Map.entry("domum_ornamentum", MINECOLONIES),
            Map.entry("structurize", MINECOLONIES),
            Map.entry("apotheosis", APOTHEOSIS),
            Map.entry("apothic_attributes", APOTHEOSIS),
            Map.entry("apothic_enchanting", APOTHEOSIS),
            Map.entry("apothic_spawners", APOTHEOSIS),
            Map.entry("botania", BOTANIA),
            Map.entry("mythicbotany", BOTANIA),
            Map.entry("botanicalmachinery", BOTANIA),
            Map.entry("extrabotany", BOTANIA)
    );

    private static final Set<String> CREATE_OWNERSHIP_TERMS = Set.of(
            "create"
    );

    private static final Set<String> CREATE_METADATA_OWNERSHIP_TERMS = Set.of(
            "create addon",
            "create add on",
            "addon for create",
            "add on for create",
            "create integration",
            "integration for create",
            "create compatibility",
            "compatibility for create",
            "requires create",
            "depends on create"
    );

    private static final Set<String> COBBLEMON_OWNERSHIP_TERMS = Set.of(
            "cobblemon", "pokemon", "pokémon"
    );

    private static final Set<String> MAPPING_OWNERSHIP_TERMS = Set.of(
            "xaero", "journeymap", "ftbchunks",
            "minimap", "worldmap", "mapping"
    );

    private static final Set<String> AMBIGUOUS_FAMILY_TERMS = Set.of(
            "press", "casing", "cell", "drive", "gear", "plate", "core", "module",
            "terminal", "controller", "cable", "pipe", "tank", "berry", "gem",
            "stone", "dust", "ingot", "nugget", "ore", "map", "claim", "claims",
            "waypoint", "waypoints", "badge", "mega", "tera", "species", "poke",
            "pokeball", "poke_ball", "apricorn", "pokedex", "evolution", "kinetic",
            "shaft", "cog", "cogwheel", "gearbox", "belt", "depot", "basin",
            "mixer", "crushing", "millstone", "assembly", "contraption", "train",
            "track", "bogey", "andesite", "brass", "deployer"
    );

    private CompatFamilyDetector() {
    }

    public static void detect(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }

        Context context = new Context(id, meta);
        Map<String, Integer> scores = new LinkedHashMap<>();
        String exactFamily = EXACT_NAMESPACE_FAMILIES.get(context.namespace);
        if (exactFamily != null) {
            score(context, exactFamily, 100, scores);
        }
        score(context, COBBLEMON, scoreCobblemon(context), scores);
        score(context, CREATE, scoreCreate(context), scores);
        score(context, MAPPING, scoreMapping(context), scores);

        if (scores.isEmpty()) {
            return;
        }

        List<String> families = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .toList();
        String primary = families.get(0);
        meta.put(SearchNodeKeys.COMPAT_FAMILIES, String.join(",", families));
        meta.put(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, primary);
        meta.put(SearchNodeKeys.COMPAT_FAMILY, primary);
    }

    public static boolean hasFamily(Map<String, String> meta, String family) {
        if (meta == null || family == null || family.isBlank()) {
            return false;
        }
        String normalized = normalize(family);
        for (String token : splitTokens(meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, ""))) {
            if (normalize(token).equals(normalized)) {
                return true;
            }
        }
        return normalize(meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILY, "")).equals(normalized)
                || normalize(meta.getOrDefault(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "")).equals(normalized);
    }

    public static boolean isPrimaryFamily(Map<String, String> meta, String family) {
        if (meta == null || family == null || family.isBlank()) {
            return false;
        }
        String normalized = normalize(family);
        return normalize(meta.getOrDefault(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "")).equals(normalized)
                || normalize(meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILY, "")).equals(normalized);
    }

    private static void score(Context context, String family, int value, Map<String, Integer> scores) {
        if (value >= FAMILY_THRESHOLD) {
            scores.put(family, value);
        }
    }

    private static int scoreCobblemon(Context context) {
        int score = 0;
        if (COBBLEMON.equals(context.namespace)) score += 100;
        if (context.namespace.contains(COBBLEMON)) score += 90;
        if (COBBLEMON_ADDON_NAMESPACES.contains(context.namespace)) score += 80;
        if (containsOwnershipTerm(context.namespace, COBBLEMON_OWNERSHIP_TERMS)) score += 70;
        if (containsOwnershipTerm(context.modMetadata, COBBLEMON_OWNERSHIP_TERMS)) score += 50;
        if (containsOwnershipTerm(context.creativeTab, COBBLEMON_OWNERSHIP_TERMS)) score += 40;
        if (containsOwnershipTerm(context.itemClass, COBBLEMON_OWNERSHIP_TERMS)) score += 35;
        return score;
    }

    private static int scoreCreate(Context context) {
        int score = 0;
        if (CREATE.equals(context.namespace)) score += 100;
        if (context.namespace.startsWith("create") || CREATE_ADDON_NAMESPACES.contains(context.namespace)) score += 80;
        if (containsOwnershipTerm(context.namespace, CREATE_OWNERSHIP_TERMS)) score += 70;
        if (containsOwnershipTerm(context.modMetadata, CREATE_METADATA_OWNERSHIP_TERMS)) score += 50;
        if (containsOwnershipTerm(context.creativeTab, CREATE_OWNERSHIP_TERMS)) score += 40;
        if (containsOwnershipTerm(context.itemClass, CREATE_OWNERSHIP_TERMS)) score += 35;
        return score;
    }

    private static int scoreMapping(Context context) {
        int score = 0;
        if (MAPPING_NAMESPACES.contains(context.namespace)) score += 100;
        if (containsOwnershipTerm(context.namespace, MAPPING_OWNERSHIP_TERMS)) score += 70;
        if (containsOwnershipTerm(context.modMetadata, MAPPING_OWNERSHIP_TERMS)) score += 50;
        if (containsOwnershipTerm(context.creativeTab, MAPPING_OWNERSHIP_TERMS)) score += 40;
        if (containsOwnershipTerm(context.itemClass, MAPPING_OWNERSHIP_TERMS)) score += 35;
        return score;
    }

    private static boolean containsOwnershipTerm(String haystack, Set<String> terms) {
        for (String term : terms) {
            if (AMBIGUOUS_FAMILY_TERMS.contains(normalizeSearchText(term))) {
                throw new IllegalStateException("Ambiguous family ownership term: " + term);
            }
        }
        return containsAny(haystack, terms);
    }

    private static boolean containsAny(String haystack, Set<String> terms) {
        if (haystack == null || haystack.isBlank()) {
            return false;
        }
        String normalizedHaystack = normalizeSearchText(haystack);
        Set<String> tokens = Set.copyOf(splitTokens(normalizedHaystack));
        for (String term : terms) {
            String normalizedTerm = normalizeSearchText(term);
            if (normalizedTerm.isBlank()) {
                continue;
            }
            if (normalizedTerm.contains(" ")) {
                if ((" " + normalizedHaystack + " ").contains(" " + normalizedTerm + " ")) {
                    return true;
                }
            } else if (tokens.contains(normalizedTerm)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitTokens(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String token : raw.split("[,\\s]+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static String modMetadata(String modId) {
        return MOD_METADATA_CACHE.computeIfAbsent(modId, id -> {
            try {
                return Services.PLATFORM.getModMetadataText(id).orElse("").toLowerCase(Locale.ROOT);
            } catch (RuntimeException | LinkageError ignored) {
                return "";
            }
        });
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private static String normalizeSearchText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String camelSplit = value.replaceAll("([a-z])([A-Z])", "$1 $2");
        return camelSplit.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static final class Context {
        final ResourceLocation id;
        final String namespace;
        final String path;
        final String creativeTab;
        final String itemClass;
        final String tags;
        final String modMetadata;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.id = id;
            this.namespace = normalize(id.getNamespace());
            this.path = id.getPath();
            this.creativeTab = meta.getOrDefault(SearchNodeKeys.CREATIVE_TAB_LABEL, "");
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.tags = normalize(meta.getOrDefault(SearchNodeKeys.TAGS, ""));
            this.modMetadata = modMetadata(this.namespace);
        }
    }
}
