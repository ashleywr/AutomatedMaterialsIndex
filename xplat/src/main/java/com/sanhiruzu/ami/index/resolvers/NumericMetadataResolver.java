package com.sanhiruzu.ami.index.resolvers;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;

import java.util.*;

/**
 * Resolves numeric UQL filters such as >dps:10 and <storage:4096.
 */
public final class NumericMetadataResolver {
    private final List<SearchNode> nodes = new ArrayList<>();

    private static double numericValue(SearchNode node, String metadataKey) {
        if (SearchNodeKeys.ATTACK_DAMAGE.equals(metadataKey)) {
            double itemDamage = parseDouble(node.meta(SearchNodeKeys.ATTACK_DAMAGE, ""));
            return Double.isNaN(itemDamage)
                    ? parseDouble(node.meta(SearchNodeKeys.ENTITY_ATTACK_DAMAGE, ""))
                    : itemDamage;
        }
        return parseDouble(node.meta(metadataKey, ""));
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) return Double.NaN;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    public void addNode(SearchNode node) {
        nodes.add(node);
    }

    public static List<String> aliasesForMetadataKey(String metadataKey) {
        return switch (metadataKey) {
            case SearchNodeKeys.DPS -> List.of("dps");
            case SearchNodeKeys.ATTACK_DAMAGE, SearchNodeKeys.ENTITY_ATTACK_DAMAGE -> List.of("damage");
            case SearchNodeKeys.POKEMON_HEALING -> List.of("heal");
            case SearchNodeKeys.POKEMON_DEX_NUMBER -> List.of("dex");
            case SearchNodeKeys.POKEMON_BASE_HP -> List.of("pokemonHp");
            case SearchNodeKeys.POKEMON_BASE_ATTACK -> List.of("pokemonAttack");
            case SearchNodeKeys.POKEMON_BASE_DEFENSE -> List.of("pokemonDefense");
            case SearchNodeKeys.POKEMON_BASE_SPECIAL_ATTACK -> List.of("pokemonSpecialAttack");
            case SearchNodeKeys.POKEMON_BASE_SPECIAL_DEFENSE -> List.of("pokemonSpecialDefense");
            case SearchNodeKeys.POKEMON_BASE_SPEED -> List.of("pokemonSpeed");
            case SearchNodeKeys.ESM_CAPACITY -> List.of("storage");
            case SearchNodeKeys.ENERGY_CAPACITY -> List.of("energy");
            case SearchNodeKeys.ENERGY_GENERATION -> List.of("gen");
            case SearchNodeKeys.ENERGY_CONSUMPTION -> List.of("consume");
            case SearchNodeKeys.FLUID_CAPACITY -> List.of("fluid");
            case SearchNodeKeys.TOOL_SPEED -> List.of("toolspeed");
            case SearchNodeKeys.MAX_DURABILITY, SearchNodeKeys.TOOL_USES -> List.of("durability");
            case SearchNodeKeys.ARMOR_DEFENSE -> List.of("armor");
            case SearchNodeKeys.ARMOR_TOUGHNESS -> List.of("toughness");
            case SearchNodeKeys.FOOD_NUTRITION -> List.of("food");
            case SearchNodeKeys.FOOD_SATURATION -> List.of("saturation");
            case SearchNodeKeys.ENTITY_HEALTH -> List.of("health");
            default -> List.of();
        };
    }

    public Map<NodeType, List<SearchNode>> resolve(String token) {
        Optional<NumericFilter> parsed = NumericFilter.parse(token);
        if (parsed.isEmpty()) return new LinkedHashMap<>();

        NumericFilter filter = parsed.get();
        Map<NodeType, List<SearchNode>> result = new LinkedHashMap<>();
        for (SearchNode node : nodes) {
            double value = numericValue(node, filter.metadataKey());
            if (Double.isNaN(value) || !filter.matches(value)) continue;
            result.computeIfAbsent(node.type(), ignored -> new ArrayList<>()).add(node);
        }
        return result;
    }

    private record NumericFilter(char operator, String metadataKey, double threshold) {
        private static Optional<NumericFilter> parse(String rawToken) {
            if (rawToken == null || rawToken.isBlank()) return Optional.empty();

            String token = rawToken.trim();
            char operator = token.charAt(0);
            if (operator != '>' && operator != '<' && operator != '=') {
                return Optional.empty();
            }

            String body = token.substring(1).trim();
            if (body.isEmpty()) return Optional.empty();

            String field = "storage";
            String number = body;
            int sep = body.indexOf(':');
            if (sep >= 0) {
                field = body.substring(0, sep).trim();
                number = body.substring(sep + 1).trim();
            }

            String metadataKey = metadataKeyFor(field);
            if (metadataKey == null || number.isEmpty()) return Optional.empty();

            try {
                return Optional.of(new NumericFilter(operator, metadataKey, Double.parseDouble(number)));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }

        private static String metadataKeyFor(String field) {
            String normalized = field.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
            return switch (normalized) {
                case "dps", "damagepersecond" -> SearchNodeKeys.DPS;
                case "damage", "attack", "attackdamage", "entityattack", "entitydamage" -> SearchNodeKeys.ATTACK_DAMAGE;
                case "heal", "healing", "hprestore", "pokemonhealing" -> SearchNodeKeys.POKEMON_HEALING;
                case "dex", "pokedex", "dexnumber", "pokemondex", "pokemondexnumber" -> SearchNodeKeys.POKEMON_DEX_NUMBER;
                case "pokemonhp", "pokemonbasehp", "basehp" -> SearchNodeKeys.POKEMON_BASE_HP;
                case "pokemonattack", "pokemonatk", "pokemonbaseattack", "pokemonbaseatk", "baseattack", "baseatk" ->
                        SearchNodeKeys.POKEMON_BASE_ATTACK;
                case "pokemondefense", "pokemondef", "pokemonbasedefense", "pokemonbasedef", "basedefense", "basedef" ->
                        SearchNodeKeys.POKEMON_BASE_DEFENSE;
                case "pokemonspecialattack", "pokemonspatk", "pokemonspa", "pokemonbasespecialattack",
                     "pokemonbasespatk", "pokemonbasespa", "basespecialattack", "basespatk", "basespa" ->
                        SearchNodeKeys.POKEMON_BASE_SPECIAL_ATTACK;
                case "pokemonspecialdefense", "pokemonspdef", "pokemonspd", "pokemonbasespecialdefense",
                     "pokemonbasespdef", "pokemonbasespd", "basespecialdefense", "basespdef", "basespd" ->
                        SearchNodeKeys.POKEMON_BASE_SPECIAL_DEFENSE;
                case "pokemonspeed", "pokemonbasespeed", "basespeed", "basespe" -> SearchNodeKeys.POKEMON_BASE_SPEED;
                case "storage", "capacity", "esm", "items" -> SearchNodeKeys.ESM_CAPACITY;
                case "energy", "energycapacity", "fecapacity", "rfcapacity", "capacityfe", "capacityrf", "fe", "rf" ->
                        SearchNodeKeys.ENERGY_CAPACITY;
                case "energygeneration", "generation", "gen", "generate", "generator", "fegeneration", "rfgeneration",
                     "fet", "rft", "fepertick", "rfpertick", "power" -> SearchNodeKeys.ENERGY_GENERATION;
                case "energyconsumption", "consumption", "consume", "feusage", "rfusage", "feuse", "rfuse" ->
                        SearchNodeKeys.ENERGY_CONSUMPTION;
                case "fluid", "fluids", "fluidcapacity", "tank", "buckets", "bucket" -> SearchNodeKeys.FLUID_CAPACITY;
                case "toolspeed", "miningspeed", "speed", "mine" -> SearchNodeKeys.TOOL_SPEED;
                case "tooluses" -> SearchNodeKeys.TOOL_USES;
                case "uses", "durability", "maxdamage", "maxdurability" -> SearchNodeKeys.MAX_DURABILITY;
                case "armor", "armordefense", "defense", "protection" -> SearchNodeKeys.ARMOR_DEFENSE;
                case "toughness", "armortoughness" -> SearchNodeKeys.ARMOR_TOUGHNESS;
                case "food", "hunger", "nutrition", "foodnutrition" -> SearchNodeKeys.FOOD_NUTRITION;
                case "saturation", "sat", "foodsaturation" -> SearchNodeKeys.FOOD_SATURATION;
                case "health", "hp", "entityhealth" -> SearchNodeKeys.ENTITY_HEALTH;
                default -> null;
            };
        }

        private boolean matches(double value) {
            return switch (operator) {
                case '>' -> value >= threshold;
                case '<' -> value <= threshold;
                case '=' -> Math.abs(value - threshold) < 0.0001D;
                default -> false;
            };
        }
    }
}
