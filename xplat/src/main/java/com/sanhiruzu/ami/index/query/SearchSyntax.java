package com.sanhiruzu.ami.index.query;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Player-facing query syntax catalog. Keep visible examples, autocomplete fields,
 * and token kinds here so the search UI does not drift from parser behavior.
 */
public final class SearchSyntax {
    private static final List<PrefixRule> PREFIX_RULES = List.of(
            new PrefixRule('$', QueryParser.TokenType.CATEGORY, false),
            new PrefixRule('~', QueryParser.TokenType.META, false),
            new PrefixRule('#', QueryParser.TokenType.TAG, false),
            new PrefixRule('@', QueryParser.TokenType.MOD, false),
            new PrefixRule('&', QueryParser.TokenType.ENV, false),
            new PrefixRule('?', QueryParser.TokenType.PROP, false),
            new PrefixRule('!', QueryParser.TokenType.ESSENTIAL, false),
            new PrefixRule('>', QueryParser.TokenType.ESM, true),
            new PrefixRule('<', QueryParser.TokenType.ESM, true),
            new PrefixRule('=', QueryParser.TokenType.ESM, true)
    );

    public static final List<PropertyField> PROPERTY_FIELDS = List.of(
            new PropertyField("energy", List.of("power", "fe", "rf"), "ami.gui.search.help.property_energy", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("fluid", List.of("fluids", "liquid", "tank"), "ami.gui.search.help.property_fluid", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("storage", List.of("inventory", "slots"), "ami.gui.search.help.property_storage", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("kind", List.of("itemkind"), "ami.gui.search.help.property_kind", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("material", List.of("materials", "gearmaterial"), "ami.gui.search.help.property_material", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("tier", List.of(), "ami.gui.search.help.property_tier", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("role", List.of("recipe", "processing", "process"), "ami.gui.search.help.property_role", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("color", List.of("colour", "colorbucket", "colourbucket"), "ami.gui.search.help.property_color", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("capability", List.of("cap", "resource"), "ami.gui.search.help.property_capability", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("fact", List.of("facts", "behavior", "behaviour"), "ami.gui.search.help.property_fact", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("trait", List.of("traits", "modifier", "modifiers"), "ami.gui.search.help.property_trait", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("gregtech", List.of("gtceu"), "ami.gui.search.help.property_field", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("gregtechTier", List.of("gtceuTier", "voltage", "voltageTier"), "ami.gui.search.help.property_tier", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("gregtechKind", List.of("gtceuKind"), "ami.gui.search.help.property_kind", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("gregtechFact", List.of("gregtechFacts", "gtceuFact", "gtceuFacts"), "ami.gui.search.help.property_fact", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("gregtechCircuit", List.of("gtceuCircuit", "gregtechGrade", "gtceuGrade", "circuitGrade"), "ami.gui.search.help.property_field", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("gregtechEnergy", List.of("gtceuEnergy", "gregtechEu", "gtceuEu", "eu", "eut", "euPerTick"), "ami.gui.search.help.property_energy", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("gregtechEnergyRole", List.of("gtceuEnergyRole", "euRole"), "ami.gui.search.help.property_role", SearchSuggestions.Kind.PROPERTY),
            new PropertyField("mod", List.of("modid", "compat", "family", "ecosystem", "compatfamily", "compatfamilies"),
                    "ami.gui.search.help.property_mod", SearchSuggestions.Kind.MOD)
    );

    public static final List<PrefixShortcut> PREFIX_SHORTCUTS = List.of(
            new PrefixShortcut("egg", "%egg:", "%egg:", "pokemonEggGroup", "ami.gui.search.help.pokemon_egg",
                    SearchSuggestions.Kind.PROPERTY, Feature.POKEMON_EGG_GROUPS)
    );

    public static final List<HelpSection> HELP_LEFT_SECTIONS = List.of(
            new HelpSection("ami.gui.search.help.common", List.of(
                    new Example("diamond", "ami.gui.search.help.plain", SearchSuggestions.Kind.PLAIN),
                    new Example("\"iron chest\"", "ami.gui.search.help.quote", SearchSuggestions.Kind.PLAIN),
                    new Example("iron chest", "ami.gui.search.help.and", SearchSuggestions.Kind.PLAIN),
                    new Example("wood | stone", "ami.gui.search.help.or", SearchSuggestions.Kind.PLAIN),
                    new Example("-zombie", "ami.gui.search.help.exclude", SearchSuggestions.Kind.PLAIN),
                    new Example("~energy", "ami.gui.search.help.meta", SearchSuggestions.Kind.META)
            ))
    );

    public static final String COMMON_SECTION_TITLE_KEY = "ami.gui.search.help.common";
    public static final List<HelpSection> HELP_RIGHT_SECTIONS = List.of(
            new HelpSection("ami.gui.search.help.advanced", List.of(
                    new Example("?energy", "ami.gui.search.help.property_simple", SearchSuggestions.Kind.PROPERTY),
                    new Example("?kind:machines", "ami.gui.search.help.property_field", SearchSuggestions.Kind.PROPERTY),
                    new Example("?color:red", "ami.gui.search.help.property_color", SearchSuggestions.Kind.PROPERTY),
                    new Example("$tech", "ami.gui.search.help.category", SearchSuggestions.Kind.CATEGORY),
                    new Example(">energy:10000", "ami.gui.search.help.greater", SearchSuggestions.Kind.NUMERIC),
                    new Example("<durability:500", "ami.gui.search.help.less", SearchSuggestions.Kind.NUMERIC),
                    new Example("=damage:8", "ami.gui.search.help.equal", SearchSuggestions.Kind.NUMERIC),
                    new Example("&nether", "ami.gui.search.help.environment", SearchSuggestions.Kind.ENVIRONMENT)
            ))
    );

    public static final String COMPAT_SECTION_TITLE_KEY = "ami.gui.search.help.compat";

    private SearchSyntax() {
    }

    public static Optional<PropertyField> propertyField(String raw) {
        String normalized = normalizeField(raw);
        return PROPERTY_FIELDS.stream()
                .filter(field -> field.matches(normalized))
                .findFirst();
    }

    public static Optional<ParsedPrefix> parsePrefixedToken(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        for (PrefixShortcut shortcut : PREFIX_SHORTCUTS) {
            if (token.startsWith(shortcut.replacement())) {
                String value = token.substring(shortcut.replacement().length());
                return Optional.of(new ParsedPrefix(QueryParser.TokenType.PROP,
                        value.isEmpty() ? "" : shortcut.propertyKey() + ":" + value));
            }
        }

        char prefix = token.charAt(0);
        return prefixRule(prefix)
                .map(rule -> {
                    String value = rule.keepPrefix() ? token : token.substring(1);
                    return new ParsedPrefix(rule.type(), value);
                });
    }

    public static String exclusionValue(QueryParser.TokenType type, String value) {
        return prefixForExclusion(type)
                .map(prefix -> prefix + value)
                .orElse(value);
    }

    public static Optional<ParsedPrefix> parseExcludedValue(String value) {
        return parsePrefixedToken(value);
    }

    public static Optional<String> routedPropertyToken(QueryParser.TokenType type, String value) {
        if (type == QueryParser.TokenType.TAG && value.startsWith("move:")) {
            return Optional.of("pokemonMove:" + value.substring("move:".length()));
        }
        if (type == QueryParser.TokenType.MOD && value.startsWith("type:")) {
            return Optional.of("pokemonType:" + value.substring("type:".length()));
        }
        return Optional.empty();
    }

    public static Optional<String> categoryNumericFilter(String categoryValue) {
        if (categoryValue == null || !categoryValue.startsWith("stat:")) {
            return Optional.empty();
        }
        String body = categoryValue.substring("stat:".length()).trim();
        int operatorAt = -1;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '>' || c == '<' || c == '=') {
                operatorAt = i;
                break;
            }
        }
        if (operatorAt <= 0 || operatorAt >= body.length() - 1) {
            return Optional.empty();
        }

        String field = body.substring(0, operatorAt).trim();
        String value = body.substring(operatorAt + 1).trim();
        if (field.isEmpty() || value.isEmpty()) {
            return Optional.empty();
        }

        String numericField = switch (normalizeField(field)) {
            case "hp", "health" -> "pokemonHp";
            case "attack", "atk" -> "pokemonAttack";
            case "defense", "def" -> "pokemonDefense";
            case "specialattack", "spatk", "spa" -> "pokemonSpecialAttack";
            case "specialdefense", "spdef", "spd" -> "pokemonSpecialDefense";
            case "speed", "spe" -> "pokemonSpeed";
            default -> null;
        };
        return numericField == null
                ? Optional.empty()
                : Optional.of(body.charAt(operatorAt) + numericField + ":" + value);
    }

    public static SearchSuggestions.Kind kindForPrefix(char prefix) {
        return switch (prefix) {
            case '?' -> SearchSuggestions.Kind.PROPERTY;
            case '@' -> SearchSuggestions.Kind.MOD;
            case '#' -> SearchSuggestions.Kind.TAG;
            case '~' -> SearchSuggestions.Kind.META;
            case '>', '<', '=' -> SearchSuggestions.Kind.NUMERIC;
            case '$' -> SearchSuggestions.Kind.CATEGORY;
            case '&' -> SearchSuggestions.Kind.ENVIRONMENT;
            case '%' -> SearchSuggestions.Kind.PROPERTY;
            default -> SearchSuggestions.Kind.PLAIN;
        };
    }

    private static Optional<PrefixRule> prefixRule(char prefix) {
        return PREFIX_RULES.stream()
                .filter(rule -> rule.prefix() == prefix)
                .findFirst();
    }

    private static Optional<Character> prefixForExclusion(QueryParser.TokenType type) {
        return PREFIX_RULES.stream()
                .filter(rule -> !rule.keepPrefix() && rule.type() == type)
                .map(PrefixRule::prefix)
                .findFirst();
    }

    public static String normalizeField(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").trim();
    }

    public record PropertyField(String id, List<String> aliases, String descriptionKey, SearchSuggestions.Kind kind) {
        boolean matches(String normalized) {
            if (SearchSyntax.normalizeField(id).equals(normalized)) {
                return true;
            }
            return aliases.stream().anyMatch(alias -> SearchSyntax.normalizeField(alias).equals(normalized));
        }
    }

    public enum Feature {
        POKEMON_TYPES,
        POKEMON_MOVES,
        POKEMON_EGG_GROUPS
    }

    private record PrefixRule(char prefix, QueryParser.TokenType type, boolean keepPrefix) {
    }

    public record ParsedPrefix(QueryParser.TokenType type, String value) {
    }

    public record PrefixShortcut(String id, String replacement, String display, String propertyKey, String descriptionKey,
                                 SearchSuggestions.Kind kind, Feature feature) {
    }

    public record HelpSection(String titleKey, List<Example> examples) {
    }

    public record Example(String text, String descriptionKey, SearchSuggestions.Kind kind) {
    }

    public record HelpLayout(List<HelpSection> leftSections, List<HelpSection> rightSections) {
    }
}
