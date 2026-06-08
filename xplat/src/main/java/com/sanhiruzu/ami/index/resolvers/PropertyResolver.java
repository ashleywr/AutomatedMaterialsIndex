package com.sanhiruzu.ami.index.resolvers;

import com.sanhiruzu.ami.index.IQueryResolver;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.query.SearchSyntax;

import java.util.*;

/**
 * Resolves simple metadata property filters such as ?tamable, ?mountable,
 * ?category:creature, and ?health:20.
 */
public final class PropertyResolver implements IQueryResolver {
    private final List<SearchNode> nodes = new ArrayList<>();

    private static boolean matches(SearchNode node, String key, String value) {
        return switch (key) {
            case "tamable", "tameable", "mountable", "trustsplayer", "pet" ->
                    containsToken(node, SearchNodeKeys.ENTITY_TRAITS, key)
                            || containsToken(node, SearchNodeKeys.SEARCH_TOKENS, key)
                            || containsToken(node, SearchNodeKeys.TAGS, "ami:" + key);
            case "category", "entitycategory" -> containsValue(node, SearchNodeKeys.ENTITY_CATEGORY, value);
            case "mod", "modid" -> containsValue(node, SearchNodeKeys.MOD_ID, value);
            case "compat", "family", "ecosystem", "compatfamily", "compatfamilies" ->
                    containsToken(node, SearchNodeKeys.COMPAT_FAMILIES, value)
                            || containsValue(node, SearchNodeKeys.PRIMARY_COMPAT_FAMILY, value)
                            || containsValue(node, SearchNodeKeys.COMPAT_FAMILY, value)
                            || containsValue(node, SearchNodeKeys.MOD_ID, value);
            case "fact", "facts", "behavior", "behaviour" -> containsConventionToken(node, value,
                    FieldConvention.FACTS,
                    FieldConvention.FACETS,
                    FieldConvention.SEARCH_TOKENS);
            case "ami", "amifilter", "amibucket" -> matchesAmiSemanticValue(node, value);
            case "guidebook", "guidebooks", "guide", "guides", "book", "books" -> matchesGuideBook(node, value);
            case "kind", "itemkind" -> containsConventionToken(node, value, FieldConvention.KIND);
            case "tier" -> containsConventionToken(node, value, FieldConvention.TIER);
            case "gregtech", "gtceu" -> value.isEmpty()
                    ? containsToken(node, SearchNodeKeys.COMPAT_FAMILIES, "gregtech")
                      || containsValue(node, SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "gregtech")
                      || containsValue(node, SearchNodeKeys.MOD_ID, "gtceu")
                      || containsValue(node, SearchNodeKeys.MOD_ID, "gregtech")
                    : containsValue(node, SearchNodeKeys.GREGTECH_ITEM_KIND, value)
                      || containsToken(node, SearchNodeKeys.GREGTECH_FACTS, value)
                      || containsToken(node, SearchNodeKeys.GREGTECH_TIER, value)
                      || containsToken(node, SearchNodeKeys.GREGTECH_CIRCUIT_GRADE, value)
                      || containsGregTechEnergyValue(node, value);
            case "gregtechtier", "gtceutier", "voltage", "voltagetier" ->
                    containsToken(node, SearchNodeKeys.GREGTECH_TIER, value);
            case "gregtechkind", "gtceukind" -> containsToken(node, SearchNodeKeys.GREGTECH_ITEM_KIND, value);
            case "gregtechfact", "gregtechfacts", "gtceufact", "gtceufacts" ->
                    containsToken(node, SearchNodeKeys.GREGTECH_FACTS, value);
            case "gregtechcircuit", "gtceucircuit", "gregtechgrade", "gtceugrade", "circuitgrade" ->
                    containsToken(node, SearchNodeKeys.GREGTECH_CIRCUIT_GRADE, value);
            case "gregtechenergy", "gtceuenergy", "gregtecheu", "gtceueu", "eu", "eut", "eupertick" ->
                    value.isEmpty() ? hasGregTechEnergyMetadata(node) : containsGregTechEnergyValue(node, value);
            case "gregtechenergyrole", "gtceuenergyrole", "eurole" ->
                    containsToken(node, SearchNodeKeys.GREGTECH_ENERGY_ROLE, value);
            case "gear", "modulargear" -> value.isEmpty()
                    ? containsToken(node, SearchNodeKeys.COMPAT_FAMILIES, "modular_gear")
                      || containsValue(node, SearchNodeKeys.MODULAR_GEAR_FAMILY, "")
                    : containsModularGearValue(node, value);
            case "material", "materials", "gearmaterial" ->
                    containsValue(node, SearchNodeKeys.MODULAR_GEAR_MATERIAL, value)
                            || containsToken(node, SearchNodeKeys.MODULAR_GEAR_RUNTIME_MATERIALS, value)
                            || containsValue(node, SearchNodeKeys.MATERIAL_GROUP, value)
                            || containsValue(node, SearchNodeKeys.BLOCKS_MATERIAL, value);
            case "part", "parts", "gearpart" -> containsModularGearPart(node, value);
            case "trait", "traits", "modifier", "modifiers" -> value.isEmpty()
                    ? containsToken(node, SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, "modifiers")
                      || containsValue(node, SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAITS, "")
                    : containsToken(node, SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAITS, value)
                      || containsToken(node, SearchNodeKeys.MODULAR_GEAR_FACTS, value)
                      || containsToken(node, SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, value)
                      || containsConventionToken(node, value, FieldConvention.FACTS, FieldConvention.KIND);
            case "runtimetrait", "runtimetraits", "runtime_trait", "runtime_traits", "geartrait", "geartraits",
                 "gear_trait", "gear_traits" -> value.isEmpty()
                    ? containsValue(node, SearchNodeKeys.MODULAR_GEAR_RUNTIME_TRAITS, "")
                    : containsToken(node, SearchNodeKeys.MODULAR_GEAR_RUNTIME_TRAITS, value);
            case "role", "recipe", "processing", "process" -> containsConventionToken(node, value,
                    FieldConvention.ROLE,
                    FieldConvention.RECIPE);
            case "capability", "cap", "resource" -> containsCapability(node, value);
            case "ponder", "hasponder" -> value.isEmpty()
                    ? hasBooleanConvention(node, "hasponder")
                    : containsConventionValue(node, "hasponder", value);
            case "energy", "power", "fe", "rf" -> value.isEmpty()
                    ? containsCapability(node, "energy")
                    : containsResourceMetadata(node, "energy", value);
            case "fluid", "fluids", "liquid", "tank" -> value.isEmpty()
                    ? containsCapability(node, "fluid")
                    : containsResourceMetadata(node, "fluid", value);
            case "storage", "inventory", "slots" -> value.isEmpty()
                    ? containsCapability(node, "storage")
                    : containsResourceMetadata(node, "storage", value);
            case "color", "colour", "colorbucket", "colourbucket" ->
                    containsValue(node, SearchNodeKeys.COLOR_BUCKET, value);
            case "machine", "machines" -> value.isEmpty()
                    ? containsSemanticToken(node, "machine", "machines", "interactive_block")
                    : containsConventionToken(node, value,
                    FieldConvention.FACTS,
                    FieldConvention.FACETS,
                    FieldConvention.KIND);
            case "upgrade", "upgrades" -> value.isEmpty()
                    ? containsSemanticToken(node, "upgrade", "upgrades")
                    : containsConventionToken(node, value,
                    FieldConvention.FACTS,
                    FieldConvention.FACETS,
                    FieldConvention.KIND);
            case "token", "tokens", "searchtoken", "searchtokens", "rawtoken", "debugtoken" ->
                    containsToken(node, SearchNodeKeys.SEARCH_TOKENS, value);
            case "meta", "metadata", "rawmeta", "debugmeta" -> containsAnyMetadata(node, key, value);
            case "health", "hp" -> containsValue(node, SearchNodeKeys.ENTITY_HEALTH, value);
            case "attack", "attackdamage", "damage" -> containsValue(node, SearchNodeKeys.ATTACK_DAMAGE, value)
                    || containsValue(node, SearchNodeKeys.ENTITY_ATTACK_DAMAGE, value);
            case "medicine", "pokemonmedicine" -> containsKind(node, "medicine", value)
                    || containsToken(node, SearchNodeKeys.POKEMON_MEDICINE_KIND, value);
            case "pokeball", "pokemonball", "captureball" -> containsKind(node, "poke_ball", value)
                    || containsToken(node, SearchNodeKeys.POKEMON_BALL_TIER, value)
                    || containsToken(node, SearchNodeKeys.POKEMON_BALL_FAMILY, value);
            case "helditem", "pokemonhelditem", "held" -> containsKind(node, "held_item", value)
                    || containsToken(node, SearchNodeKeys.POKEMON_HELD_ITEM_ROLE, value);
            case "evolution", "evolutionitem", "pokemonevolution" -> containsKind(node, "evolution_item", value)
                    || containsToken(node, SearchNodeKeys.POKEMON_EVOLUTION_TRIGGER, value);
            case "fossil", "pokemonfossil" -> containsKind(node, "fossil", value);
            case "berry", "pokemonberry" -> containsKind(node, "berry", value);
            case "apricorn", "pokemonapricorn" -> containsKind(node, "apricorn", value)
                    || containsKind(node, "apricorn_seed", value);
            case "type" -> isGuideBookValue(value)
                    ? matchesGuideBook(node, "")
                    : containsToken(node, SearchNodeKeys.POKEMON_TYPE, value);
            case "pokemontype" -> containsToken(node, SearchNodeKeys.POKEMON_TYPE, value);
            case "species", "pokemon", "pokemonspecies" -> containsValue(node, SearchNodeKeys.POKEMON_SPECIES, value);
            case "generation", "gen", "pokemongeneration" ->
                    containsValue(node, SearchNodeKeys.POKEMON_GENERATION, value);
            case "ability", "pokemonability" -> containsToken(node, SearchNodeKeys.POKEMON_ABILITIES, value);
            case "status", "statuscure", "pokemonstatus", "pokemonstatuscure" ->
                    containsToken(node, SearchNodeKeys.POKEMON_STATUS_CURE, value);
            case "move", "pokemonmove" -> containsToken(node, SearchNodeKeys.POKEMON_MOVE, value);
            case "tm", "tmmove", "pokemontm" -> containsToken(node, SearchNodeKeys.POKEMON_TM_MOVE, value);
            case "tutor", "tutormove", "pokemontutor" -> containsToken(node, SearchNodeKeys.POKEMON_TUTOR_MOVE, value);
            case "egg", "egggroup", "pokemonegg", "pokemonegggroup" ->
                    containsToken(node, SearchNodeKeys.POKEMON_EGG_GROUPS, value);
            case "eggmove", "pokemoneggmove" -> containsToken(node, SearchNodeKeys.POKEMON_EGG_MOVE, value);
            case "drop", "pokemondrop" -> containsValue(node, SearchNodeKeys.POKEMON_DROP_ITEM, value);
            case "fireimmune" -> value.isEmpty()
                    ? "true".equalsIgnoreCase(node.meta(SearchNodeKeys.FIRE_IMMUNE, ""))
                    : containsValue(node, SearchNodeKeys.FIRE_IMMUNE, value);
            default -> value.isEmpty() && containsSemanticPropertyValue(node, key);
        };
    }

    private static boolean containsKind(SearchNode node, String kind, String value) {
        if (!containsToken(node, SearchNodeKeys.COBBLEMON_ITEM_KIND, kind)) {
            return false;
        }
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalizedValue = normalize(value);
        for (String metadataValue : node.metadata().values()) {
            if (normalize(metadataValue).contains(normalizedValue)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAmiSemanticValue(SearchNode node, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return containsValue(node, SearchNodeKeys.ONTOLOGY_CATEGORY, value)
                || containsToken(node, SearchNodeKeys.ONTOLOGY_SUBCATEGORY, value)
                || containsConventionToken(node, value,
                FieldConvention.FACTS,
                FieldConvention.FACETS,
                FieldConvention.KIND,
                FieldConvention.ROLE,
                FieldConvention.RECIPE)
                || containsNamespacedToken(node, SearchNodeKeys.TAGS, "ami", value)
                || containsNamespacedToken(node, SearchNodeKeys.BLOCK_TAGS, "ami", value)
                || containsNamespacedToken(node, SearchNodeKeys.SEARCH_TOKENS, "ami", value);
    }

    private static boolean containsNamespacedToken(SearchNode node, String metadataKey, String namespace, String value) {
        String normalizedNamespace = normalize(namespace);
        String normalizedValue = normalize(value);
        if (normalizedNamespace.isEmpty() || normalizedValue.isEmpty()) {
            return false;
        }
        for (String token : splitTokens(node.meta(metadataKey, ""))) {
            int separator = token.indexOf(':');
            if (separator <= 0 || separator >= token.length() - 1) {
                continue;
            }
            String tokenNamespace = token.substring(0, separator);
            String tokenPath = token.substring(separator + 1);
            if (normalize(tokenNamespace).equals(normalizedNamespace)
                    && normalize(tokenPath).equals(normalizedValue)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsModularGearPart(SearchNode node, String value) {
        if (value == null || value.isBlank()) {
            return containsValue(node, SearchNodeKeys.MODULAR_GEAR_PART, "")
                    || containsToken(node, SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, "parts");
        }
        return containsToken(node, SearchNodeKeys.MODULAR_GEAR_PART, value);
    }

    private static boolean containsModularGearValue(SearchNode node, String value) {
        return containsValue(node, SearchNodeKeys.MODULAR_GEAR_FAMILY, value)
                || containsValue(node, SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, value)
                || containsValue(node, SearchNodeKeys.MODULAR_GEAR_FACTS, value)
                || containsValue(node, SearchNodeKeys.MODULAR_GEAR_MATERIAL, value)
                || containsValue(node, SearchNodeKeys.MODULAR_GEAR_PART, value)
                || containsValue(node, SearchNodeKeys.MODULAR_GEAR_TIER, value)
                || containsValue(node, SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAITS, value)
                || containsValue(node, SearchNodeKeys.MODULAR_GEAR_RUNTIME_MATERIALS, value)
                || containsValue(node, SearchNodeKeys.MODULAR_GEAR_RUNTIME_TRAITS, value)
                || containsValue(node, SearchNodeKeys.MODULAR_GEAR_RUNTIME_STATS, value);
    }

    private static boolean containsToken(SearchNode node, String metadataKey, String token) {
        String normalizedToken = normalize(token);
        if (normalizedToken.isEmpty()) {
            return false;
        }
        for (String part : normalize(node.meta(metadataKey, "")).split("[,\\s]+")) {
            if (part.equals(normalizedToken)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsConventionToken(SearchNode node, String value, FieldConvention... conventions) {
        for (var entry : node.metadata().entrySet()) {
            if (!matchesAnyConvention(entry.getKey(), conventions)) {
                continue;
            }
            if (containsTokenValue(entry.getValue(), value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsConventionValue(SearchNode node, String keySuffix, String value) {
        String normalizedSuffix = normalize(keySuffix);
        for (var entry : node.metadata().entrySet()) {
            if (normalize(entry.getKey()).endsWith(normalizedSuffix) && containsValue(entry.getValue(), value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBooleanConvention(SearchNode node, String keySuffix) {
        String normalizedSuffix = normalize(keySuffix);
        for (var entry : node.metadata().entrySet()) {
            if (normalize(entry.getKey()).endsWith(normalizedSuffix)
                    && "true".equalsIgnoreCase(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsCapability(SearchNode node, String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return !indexedCapabilities(node).isEmpty();
        }
        if (Set.of("power", "fe", "rf").contains(normalized)) {
            normalized = "energy";
        } else if (Set.of("fluids", "liquid", "tank").contains(normalized)) {
            normalized = "fluid";
        }
        return indexedCapabilities(node).contains(normalized);
    }

    public static Set<String> indexedCapabilities(SearchNode node) {
        LinkedHashSet<String> capabilities = new LinkedHashSet<>();
        if (hasMetadata(node, SearchNodeKeys.ENERGY_CAPACITY)
                || hasMetadata(node, SearchNodeKeys.ENERGY_GENERATION)
                || hasMetadata(node, SearchNodeKeys.ENERGY_CONSUMPTION)
                || hasGregTechEnergyMetadata(node)
                || containsToken(node, SearchNodeKeys.FACETS, "has_energy")
                || containsFactComponent(node, "energy", "power", "fe", "rf")) {
            capabilities.add("energy");
        }
        if (hasMetadata(node, SearchNodeKeys.FLUID_CAPACITY)
                || containsToken(node, SearchNodeKeys.FACETS, "fluid_container")
                || containsFactComponent(node, "fluid", "fluids", "liquid")) {
            capabilities.add("fluid");
        }
        if (hasMetadata(node, SearchNodeKeys.STORAGE_ITEM_KIND)
                || containsToken(node, SearchNodeKeys.STORAGE_FACTS, "storage")
                || containsFactComponent(node, "storage")) {
            capabilities.add("storage");
        }
        return capabilities;
    }

    private static boolean containsSemanticToken(SearchNode node, String... tokens) {
        for (String token : tokens) {
            if (containsConventionToken(node, token,
                    FieldConvention.FACTS,
                    FieldConvention.FACETS,
                    FieldConvention.KIND)
                    || containsToken(node, SearchNodeKeys.ONTOLOGY_SUBCATEGORY, token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSemanticPropertyValue(SearchNode node, String value) {
        return containsConventionToken(node, value,
                FieldConvention.FACTS,
                FieldConvention.FACETS,
                FieldConvention.KIND,
                FieldConvention.TIER,
                FieldConvention.ROLE,
                FieldConvention.RECIPE);
    }

    private static boolean containsResourceMetadata(SearchNode node, String resource, String value) {
        String normalizedResource = normalize(resource);
        for (var entry : node.metadata().entrySet()) {
            String normalizedKey = normalize(entry.getKey());
            if (normalizedKey.contains(normalizedResource) && containsValue(entry.getValue(), value)) {
                return true;
            }
        }
        return containsConventionToken(node, value, FieldConvention.FACTS, FieldConvention.FACETS);
    }

    private static boolean containsFactComponent(SearchNode node, String... concepts) {
        for (var entry : node.metadata().entrySet()) {
            if (!FieldConvention.FACTS.matches(entry.getKey()) && !FieldConvention.SEARCH_TOKENS.matches(entry.getKey())) {
                continue;
            }
            for (String token : splitTokens(entry.getValue())) {
                Set<String> parts = tokenParts(token);
                for (String concept : concepts) {
                    if (parts.contains(normalize(concept))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasMetadata(SearchNode node, String key) {
        return !node.meta(key, "").isBlank();
    }

    private static boolean matchesGuideBook(SearchNode node, String value) {
        boolean guideBookCandidate = isGuideBookCandidate(node);
        if (value != null && !value.isBlank() && !isGuideBookValue(value)) {
            return guideBookCandidate && containsValue(node, SearchNodeKeys.GUIDE_BOOK_SYSTEM, value);
        }
        return guideBookCandidate;
    }

    private static boolean isGuideBookCandidate(SearchNode node) {
        return "true".equalsIgnoreCase(node.meta(SearchNodeKeys.GUIDE_BOOK_CANDIDATE, ""))
                || containsToken(node, SearchNodeKeys.FACETS, "guide_book")
                || containsToken(node, SearchNodeKeys.SEARCH_TOKENS, "guidebook")
                || containsToken(node, SearchNodeKeys.SEARCH_TOKENS, "guidebooks");
    }

    private static boolean isGuideBookValue(String value) {
        return Set.of("guidebook", "guidebooks").contains(normalize(value));
    }

    private static boolean hasGregTechEnergyMetadata(SearchNode node) {
        return hasMetadata(node, SearchNodeKeys.GREGTECH_ENERGY_ROLE)
                || hasMetadata(node, SearchNodeKeys.GREGTECH_EU_GENERATION)
                || hasMetadata(node, SearchNodeKeys.GREGTECH_EU_CONSUMPTION)
                || hasMetadata(node, SearchNodeKeys.GREGTECH_EU_INPUT)
                || hasMetadata(node, SearchNodeKeys.GREGTECH_EU_OUTPUT);
    }

    private static boolean containsGregTechEnergyValue(SearchNode node, String value) {
        String normalizedValue = normalize(value);
        String amperageValue = normalizedValue.endsWith("a")
                ? normalizedValue.substring(0, normalizedValue.length() - 1)
                : normalizedValue;
        return containsToken(node, SearchNodeKeys.GREGTECH_ENERGY_ROLE, value)
                || containsToken(node, SearchNodeKeys.GREGTECH_TIER, value)
                || containsValue(node, SearchNodeKeys.GREGTECH_EU_GENERATION, value)
                || containsValue(node, SearchNodeKeys.GREGTECH_EU_CONSUMPTION, value)
                || containsValue(node, SearchNodeKeys.GREGTECH_EU_INPUT, value)
                || containsValue(node, SearchNodeKeys.GREGTECH_EU_OUTPUT, value)
                || containsValue(node, SearchNodeKeys.GREGTECH_AMPERAGE, value)
                || (!amperageValue.equals(normalizedValue)
                && containsValue(node, SearchNodeKeys.GREGTECH_AMPERAGE, amperageValue));
    }

    private static boolean containsValue(SearchNode node, String metadataKey, String value) {
        return containsValue(node.meta(metadataKey, ""), value);
    }

    private static boolean containsValue(String metadata, String value) {
        String normalizedMetadata = normalize(metadata);
        String normalizedValue = normalize(value);
        if (normalizedValue.isEmpty()) {
            return !normalizedMetadata.isEmpty();
        }
        return normalizedMetadata.contains(normalizedValue);
    }

    private static boolean containsAnyMetadata(SearchNode node, String key, String value) {
        String normalizedValue = normalize(value.isEmpty() ? key : value);
        if (normalizedValue.isEmpty()) {
            return false;
        }
        for (var entry : node.metadata().entrySet()) {
            String metadataKey = normalize(entry.getKey());
            String metadataValue = normalize(entry.getValue());
            if ((metadataKey.equals(key) || value.isEmpty()) && metadataValue.contains(normalizedValue)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .trim();
    }

    private static boolean containsTokenValue(String metadata, String token) {
        String normalizedToken = normalize(token);
        if (normalizedToken.isEmpty()) {
            return metadata != null && !metadata.isBlank();
        }
        for (String part : splitTokens(metadata)) {
            if (normalize(part).equals(normalizedToken)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitTokens(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String part : metadata.split("[,\\s]+")) {
            if (!part.isBlank()) {
                tokens.add(part.trim());
            }
        }
        return tokens;
    }

    private static Set<String> tokenParts(String token) {
        Set<String> parts = new HashSet<>();
        String normalized = normalize(token);
        if (!normalized.isBlank()) {
            parts.add(normalized);
        }
        for (String part : token.split("[_\\-:/]+")) {
            String normalizedPart = normalize(part);
            if (!normalizedPart.isBlank()) {
                parts.add(normalizedPart);
            }
        }
        return parts;
    }

    private static boolean matchesAnyConvention(String metadataKey, FieldConvention... conventions) {
        for (FieldConvention convention : conventions) {
            if (convention.matches(metadataKey)) {
                return true;
            }
        }
        return false;
    }

    public void addNode(SearchNode node) {
        nodes.add(node);
    }

    @Override
    public Map<NodeType, List<SearchNode>> resolve(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return new LinkedHashMap<>();
        }

        int separator = normalized.indexOf(':');
        String key = separator >= 0 ? normalized.substring(0, separator) : normalized;
        String value = separator >= 0 ? normalized.substring(separator + 1) : "";
        if (separator < 0 && SearchSyntax.isIncompletePropertyFieldPrefix(key)) {
            return new LinkedHashMap<>();
        }

        Map<NodeType, List<SearchNode>> result = new LinkedHashMap<>();
        for (SearchNode node : nodes) {
            if (matches(node, key, value)) {
                result.computeIfAbsent(node.type(), ignored -> new ArrayList<>()).add(node);
            }
        }
        return result;
    }

    private enum FieldConvention {
        FACTS,
        FACETS,
        SEARCH_TOKENS,
        KIND,
        TIER,
        ROLE,
        RECIPE;

        boolean matches(String key) {
            return switch (this) {
                case FACTS -> normalize(key).endsWith("facts");
                case FACETS -> SearchNodeKeys.FACETS.equals(key) || SearchNodeKeys.COMPONENT_FACTS.equals(key);
                case SEARCH_TOKENS -> SearchNodeKeys.SEARCH_TOKENS.equals(key);
                case KIND -> {
                    String normalized = normalize(key);
                    yield normalized.endsWith("itemkind") || SearchNodeKeys.ONTOLOGY_SUBCATEGORY.equals(key);
                }
                case TIER -> normalize(key).endsWith("tier");
                case ROLE -> {
                    String normalized = normalize(key);
                    yield normalized.endsWith("role") || normalized.endsWith("roles");
                }
                case RECIPE ->
                        key.equals(SearchNodeKeys.RECIPE_CATEGORIES) || key.equals(SearchNodeKeys.RECIPE_USE_CATEGORIES);
            };
        }
    }
}
