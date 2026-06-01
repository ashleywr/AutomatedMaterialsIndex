package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.IAmiDataProvider;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CobblemonSpeciesProvider implements IAmiDataProvider {
    private static final String SPECIES_REGISTRY_CLASS = "com.cobblemon.mod.common.api.pokemon.PokemonSpecies";

    @Override
    public void populate(GlobalIndex index, @Nullable Level level) {
        SpeciesApi api = SpeciesApi.tryLoad();
        if (api == null) {
            return;
        }

        int added = 0;
        for (Object species : api.species()) {
            try {
                SearchNode node = buildSpeciesNode(api, species);
                if (node != null) {
                    index.addNode(node);
                    added++;
                }
            } catch (Exception e) {
                AmiCore.LOGGER.warn("Failed to index Cobblemon species {}", safeString(species), e);
            }
        }
        AmiCore.LOGGER.debug("Indexed {} Cobblemon species nodes", added);
    }

    private static SearchNode buildSpeciesNode(SpeciesApi api, Object species) throws ReflectiveOperationException {
        ResourceLocation speciesId = (ResourceLocation) api.getResourceIdentifier.invoke(species);
        if (speciesId == null) {
            return null;
        }

        String speciesPath = speciesId.getPath();
        ResourceLocation nodeId = ResourceLocation.fromNamespaceAndPath("cobblemon", "species/" + speciesPath);
        String displayName = componentString(api.getTranslatedName.invoke(species), fallbackName(speciesPath));

        Map<String, String> meta = new LinkedHashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "cobblemon");
        meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, "cobblemon");
        meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "species");
        meta.put(SearchNodeKeys.ENTITY_CATEGORY, "pokemon_species");
        meta.put(SearchNodeKeys.ENTITY_TRAITS, "pokemon");
        meta.put(SearchNodeKeys.POKEMON_SPECIES, speciesId.toString());
        meta.put(SearchNodeKeys.POKEMON_DEX_NUMBER, Integer.toString((Integer) api.getNationalPokedexNumber.invoke(species)));
        meta.put(SearchNodeKeys.POKEMON_GENERATION, Integer.toString(generation((Integer) api.getNationalPokedexNumber.invoke(species))));
        meta.put(SearchNodeKeys.POKEMON_IMPLEMENTED, Boolean.toString((Boolean) api.getImplemented.invoke(species)));

        putTypes(api, species, meta);
        putBaseStats(api, species, meta);
        putEggGroups(api, species, meta);
        putAbilities(api, species, meta);
        putMoves(api, species, meta);
        putFloat(api.getHeight.invoke(species), SearchNodeKeys.POKEMON_HEIGHT, meta);
        putFloat(api.getWeight.invoke(species), SearchNodeKeys.POKEMON_WEIGHT, meta);

        meta.put(SearchNodeKeys.SEARCH_TOKENS, "pokemon species pokedex " + speciesPath.replace('_', ' '));
        return new SearchNode(nodeId, NodeType.ENTITY, displayName, 0xFFFFFF, 0, meta);
    }

    private static void putTypes(SpeciesApi api, Object species, Map<String, String> meta) throws ReflectiveOperationException {
        String primary = typeName(api, api.getPrimaryType.invoke(species));
        String secondary = typeName(api, api.getSecondaryType.invoke(species));
        if (!primary.isBlank()) {
            meta.put(SearchNodeKeys.POKEMON_PRIMARY_TYPE, primary);
        }
        if (!secondary.isBlank()) {
            meta.put(SearchNodeKeys.POKEMON_SECONDARY_TYPE, secondary);
        }
        meta.put(SearchNodeKeys.POKEMON_TYPE, joinNonBlank(primary, secondary));
    }

    private static void putBaseStats(SpeciesApi api, Object species, Map<String, String> meta) throws ReflectiveOperationException {
        Object rawStats = api.getBaseStats.invoke(species);
        if (!(rawStats instanceof Map<?, ?> stats)) {
            return;
        }

        for (var entry : stats.entrySet()) {
            String stat = statName(api, entry.getKey());
            String value = String.valueOf(entry.getValue());
            switch (stat) {
                case "hp" -> meta.put(SearchNodeKeys.POKEMON_BASE_HP, value);
                case "atk", "attack" -> meta.put(SearchNodeKeys.POKEMON_BASE_ATTACK, value);
                case "def", "defence", "defense" -> meta.put(SearchNodeKeys.POKEMON_BASE_DEFENSE, value);
                case "spa", "special_attack" -> meta.put(SearchNodeKeys.POKEMON_BASE_SPECIAL_ATTACK, value);
                case "spd", "special_defence", "special_defense" -> meta.put(SearchNodeKeys.POKEMON_BASE_SPECIAL_DEFENSE, value);
                case "spe", "speed" -> meta.put(SearchNodeKeys.POKEMON_BASE_SPEED, value);
                default -> {
                }
            }
        }
    }

    private static void putEggGroups(SpeciesApi api, Object species, Map<String, String> meta) throws ReflectiveOperationException {
        Object rawGroups = api.getEggGroups.invoke(species);
        if (!(rawGroups instanceof Iterable<?> groups)) {
            return;
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (Object group : groups) {
            String value = invokeString(findNoArgMethod(group.getClass(), "getShowdownID", "getShowdownID$common"), group);
            if (value.isBlank()) {
                value = safeString(group);
            }
            addNormalized(values, value);
        }
        putJoined(meta, SearchNodeKeys.POKEMON_EGG_GROUPS, values);
    }

    private static void putAbilities(SpeciesApi api, Object species, Map<String, String> meta) throws ReflectiveOperationException {
        Object rawAbilities = api.getAbilities.invoke(species);
        if (!(rawAbilities instanceof Iterable<?> abilities)) {
            return;
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (Object potential : abilities) {
            Object template = invokeNoArg(potential, "getTemplate");
            addNormalized(values, invokeString(findNoArgMethod(template == null ? null : template.getClass(), "getName"), template));
        }
        putJoined(meta, SearchNodeKeys.POKEMON_ABILITIES, values);
    }

    private static void putMoves(SpeciesApi api, Object species, Map<String, String> meta) throws ReflectiveOperationException {
        Object learnset = api.getMoves.invoke(species);
        if (learnset == null) {
            return;
        }

        LinkedHashSet<String> allMoves = moves(api, invokeNoArg(learnset, "getAllLegalMoves"));
        if (allMoves.isEmpty()) {
            allMoves.addAll(moves(api, invokeNoArg(learnset, "getTmMoves")));
            allMoves.addAll(moves(api, invokeNoArg(learnset, "getEggMoves")));
            allMoves.addAll(moves(api, invokeNoArg(learnset, "getTutorMoves")));
            allMoves.addAll(moves(api, invokeNoArg(learnset, "getEvolutionMoves")));
            allMoves.addAll(moves(api, invokeNoArg(learnset, "getFormChangeMoves")));
            Object rawLevelMoves = invokeNoArg(learnset, "getLevelUpMoves");
            if (rawLevelMoves instanceof Map<?, ?> levelMoves) {
                for (Object moveList : levelMoves.values()) {
                    allMoves.addAll(moves(api, moveList));
                }
            }
        }
        putJoined(meta, SearchNodeKeys.POKEMON_MOVE, allMoves);
        putJoined(meta, SearchNodeKeys.POKEMON_TM_MOVE, moves(api, invokeNoArg(learnset, "getTmMoves")));
        putJoined(meta, SearchNodeKeys.POKEMON_EGG_MOVE, moves(api, invokeNoArg(learnset, "getEggMoves")));
        putJoined(meta, SearchNodeKeys.POKEMON_TUTOR_MOVE, moves(api, invokeNoArg(learnset, "getTutorMoves")));

        Object rawLevelMoves = invokeNoArg(learnset, "getLevelUpMoves");
        if (rawLevelMoves instanceof Map<?, ?> levelMoves) {
            LinkedHashSet<String> values = new LinkedHashSet<>();
            for (Object moveList : levelMoves.values()) {
                values.addAll(moves(api, moveList));
            }
            putJoined(meta, SearchNodeKeys.POKEMON_LEVEL_UP_MOVE, values);
        }
    }

    private static LinkedHashSet<String> moves(SpeciesApi api, Object rawMoves) throws ReflectiveOperationException {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (!(rawMoves instanceof Iterable<?> moves)) {
            return values;
        }
        for (Object move : moves) {
            addNormalized(values, invokeString(findNoArgMethod(move == null ? null : move.getClass(), "getName"), move));
        }
        return values;
    }

    private static String typeName(SpeciesApi api, Object type) throws ReflectiveOperationException {
        if (type == null) {
            return "";
        }
        String name = invokeString(findNoArgMethod(type.getClass(), "getName"), type);
        return normalizeToken(name);
    }

    private static String statName(SpeciesApi api, Object stat) throws ReflectiveOperationException {
        if (stat == null) {
            return "";
        }
        String showdown = invokeString(findNoArgMethod(stat.getClass(), "getShowdownId"), stat);
        if (!showdown.isBlank()) {
            return normalizeToken(showdown);
        }
        return normalizeToken(safeString(stat));
    }

    private static void putFloat(Object value, String key, Map<String, String> meta) {
        if (value instanceof Number number) {
            meta.put(key, Float.toString(number.floatValue()));
        }
    }

    private static void putJoined(Map<String, String> meta, String key, Collection<String> values) {
        if (!values.isEmpty()) {
            meta.put(key, String.join(",", values));
        }
    }

    private static void addNormalized(Set<String> values, String value) {
        String normalized = normalizeToken(value);
        if (!normalized.isBlank()) {
            values.add(normalized);
        }
    }

    private static String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_')
                .trim();
    }

    private static String joinNonBlank(String first, String second) {
        if (first.isBlank()) return second;
        if (second.isBlank()) return first;
        return first + "," + second;
    }

    private static String invokeString(Method method, Object target) throws ReflectiveOperationException {
        if (method == null || target == null) {
            return "";
        }
        Object value = method.invoke(target);
        return value == null ? "" : value.toString();
    }

    private static Object invokeNoArg(Object target, String methodName) throws ReflectiveOperationException {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        Method method = findNoArgMethod(target.getClass(), methodName);
        return method == null ? null : method.invoke(target);
    }

    private static Method findNoArgMethod(Class<?> owner, String... names) {
        if (owner == null || names == null) {
            return null;
        }
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            try {
                Method method = owner.getMethod(name);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
            try {
                Method method = owner.getDeclaredMethod(name);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static String componentString(Object component, String fallback) {
        if (component instanceof Component text) {
            String value = text.getString();
            return value.isBlank() ? fallback : value;
        }
        String value = component == null ? "" : component.toString();
        return value.isBlank() ? fallback : value;
    }

    private static String fallbackName(String path) {
        String[] parts = path.replace('-', '_').split("_");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) continue;
            words.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
        }
        return words.isEmpty() ? path : String.join(" ", words);
    }

    private static int generation(int dexNumber) {
        if (dexNumber <= 151) return 1;
        if (dexNumber <= 251) return 2;
        if (dexNumber <= 386) return 3;
        if (dexNumber <= 493) return 4;
        if (dexNumber <= 649) return 5;
        if (dexNumber <= 721) return 6;
        if (dexNumber <= 809) return 7;
        if (dexNumber <= 905) return 8;
        return 9;
    }

    private static String safeString(Object value) {
        return value == null ? "<null>" : value.toString();
    }

    private record SpeciesApi(
            Object speciesRegistry,
            Method getSpecies,
            Method getResourceIdentifier,
            Method getTranslatedName,
            Method getNationalPokedexNumber,
            Method getImplemented,
            Method getPrimaryType,
            Method getSecondaryType,
            Method getBaseStats,
            Method getEggGroups,
            Method getAbilities,
            Method getMoves,
            Method getHeight,
            Method getWeight
    ) {
        static SpeciesApi tryLoad() {
            try {
                Class<?> pokemonSpecies = Class.forName(SPECIES_REGISTRY_CLASS);
                Class<?> species = Class.forName("com.cobblemon.mod.common.pokemon.Species");
                Method getSpecies = pokemonSpecies.getMethod("getSpecies");
                Object speciesRegistry = Modifier.isStatic(getSpecies.getModifiers())
                        ? null
                        : pokemonSpecies.getField("INSTANCE").get(null);

                return new SpeciesApi(
                        speciesRegistry,
                        getSpecies,
                        species.getMethod("getResourceIdentifier"),
                        species.getMethod("getTranslatedName"),
                        species.getMethod("getNationalPokedexNumber"),
                        species.getMethod("getImplemented"),
                        species.getMethod("getPrimaryType"),
                        species.getMethod("getSecondaryType"),
                        species.getMethod("getBaseStats"),
                        species.getMethod("getEggGroups"),
                        species.getMethod("getAbilities"),
                        species.getMethod("getMoves"),
                        species.getMethod("getHeight"),
                        species.getMethod("getWeight")
                );
            } catch (ClassNotFoundException ignored) {
                return null;
            } catch (ReflectiveOperationException e) {
                AmiCore.LOGGER.warn("Cobblemon species API shape was not recognized; species indexing disabled", e);
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        Collection<Object> species() {
            try {
                Object value = getSpecies.invoke(speciesRegistry);
                if (value instanceof Collection<?> collection) {
                    List<Object> copy = new ArrayList<>((Collection<Object>) collection);
                    copy.sort(Comparator.comparing(CobblemonSpeciesProvider::safeString));
                    return copy;
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                AmiCore.LOGGER.warn("Failed to read Cobblemon species registry", e);
            }
            return List.of();
        }
    }
}
