package com.sanhiruzu.ami.index;

import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;

import java.util.*;

/**
 * Tokenized Trie implementation using fastutil's primitive-char map to avoid boxing.
 * Fast for prefix searches and has a simple substring fallback.
 */
public final class SearchIndex {
    private static final Set<String> SEARCHABLE_METADATA_KEYS = Set.of(
            SearchNodeKeys.MOD_ID,
            SearchNodeKeys.COMPAT_FAMILY,
            SearchNodeKeys.COMPAT_FAMILIES,
            SearchNodeKeys.PRIMARY_COMPAT_FAMILY,
            SearchNodeKeys.ONTOLOGY_CATEGORY,
            SearchNodeKeys.ONTOLOGY_SUBCATEGORY,
            SearchNodeKeys.VARIANT_GROUP,
            SearchNodeKeys.COLOR_BUCKET,
            SearchNodeKeys.MATERIAL_GROUP,
            SearchNodeKeys.FACETS,
            SearchNodeKeys.ESM_CAPACITY,
            SearchNodeKeys.ENERGY_CAPACITY,
            SearchNodeKeys.ENERGY_GENERATION,
            SearchNodeKeys.ENERGY_CONSUMPTION,
            SearchNodeKeys.FLUID_CAPACITY,
            SearchNodeKeys.TOOL_SPEED,
            SearchNodeKeys.TOOL_USES,
            SearchNodeKeys.TOOL_ATTACK_BONUS,
            SearchNodeKeys.ARMOR_DEFENSE,
            SearchNodeKeys.ARMOR_TOUGHNESS,
            SearchNodeKeys.AMMO_TYPE,
            SearchNodeKeys.MAX_DURABILITY,
            SearchNodeKeys.FOOD_NUTRITION,
            SearchNodeKeys.FOOD_SATURATION,
            SearchNodeKeys.ATTACK_DAMAGE,
            SearchNodeKeys.DPS,
            SearchNodeKeys.DIMENSION,
            SearchNodeKeys.ENTITY_CATEGORY,
            SearchNodeKeys.ENTITY_TRAITS,
            SearchNodeKeys.ENTITY_HEALTH,
            SearchNodeKeys.ENTITY_ATTACK_DAMAGE,
            SearchNodeKeys.POKEMON_TYPE,
            SearchNodeKeys.POKEMON_SPECIES,
            SearchNodeKeys.POKEMON_BALL_FAMILY,
            SearchNodeKeys.POKEMON_BALL_TIER,
            SearchNodeKeys.POKEMON_MEDICINE_KIND,
            SearchNodeKeys.POKEMON_HEALING,
            SearchNodeKeys.POKEMON_STATUS_CURE,
            SearchNodeKeys.POKEMON_HELD_ITEM_ROLE,
            SearchNodeKeys.POKEMON_EVOLUTION_TRIGGER,
            SearchNodeKeys.POKEMON_DEX_NUMBER,
            SearchNodeKeys.POKEMON_PRIMARY_TYPE,
            SearchNodeKeys.POKEMON_SECONDARY_TYPE,
            SearchNodeKeys.POKEMON_GENERATION,
            SearchNodeKeys.POKEMON_ABILITIES,
            SearchNodeKeys.POKEMON_EGG_GROUPS,
            SearchNodeKeys.POKEMON_MOVE,
            SearchNodeKeys.POKEMON_DROP_ITEM,
            SearchNodeKeys.POKEMON_DROP_CHANCE,
            SearchNodeKeys.POKEMON_DROP_MIN,
            SearchNodeKeys.POKEMON_DROP_MAX,
            SearchNodeKeys.POKEMON_BASE_HP,
            SearchNodeKeys.POKEMON_BASE_ATTACK,
            SearchNodeKeys.POKEMON_BASE_DEFENSE,
            SearchNodeKeys.POKEMON_BASE_SPECIAL_ATTACK,
            SearchNodeKeys.POKEMON_BASE_SPECIAL_DEFENSE,
            SearchNodeKeys.POKEMON_BASE_SPEED,
            SearchNodeKeys.POKEMON_HEIGHT,
            SearchNodeKeys.POKEMON_WEIGHT,
            SearchNodeKeys.POKEMON_IMPLEMENTED,
            SearchNodeKeys.POKEMON_TM_MOVE,
            SearchNodeKeys.POKEMON_EGG_MOVE,
            SearchNodeKeys.POKEMON_TUTOR_MOVE,
            SearchNodeKeys.POKEMON_LEVEL_UP_MOVE,
            SearchNodeKeys.CREATE_ITEM_KIND,
            SearchNodeKeys.CREATE_FACTS,
            SearchNodeKeys.CREATE_RECIPE_ROLES,
            SearchNodeKeys.CREATE_STRESS_ROLE,
            SearchNodeKeys.CREATE_KINETIC_ROLE,
            SearchNodeKeys.AE2_ITEM_KIND,
            SearchNodeKeys.AE2_FACTS,
            SearchNodeKeys.AE2_STORAGE_TIER,
            SearchNodeKeys.AE2_STORAGE_MEDIUM,
            SearchNodeKeys.MEKANISM_ITEM_KIND,
            SearchNodeKeys.MEKANISM_FACTS,
            SearchNodeKeys.MEKANISM_TIER,
            SearchNodeKeys.STORAGE_ITEM_KIND,
            SearchNodeKeys.STORAGE_FACTS,
            SearchNodeKeys.STORAGE_TIER,
            SearchNodeKeys.SOPHISTICATED_ITEM_KIND,
            SearchNodeKeys.SOPHISTICATED_FACTS,
            SearchNodeKeys.SOPHISTICATED_TIER,
            SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAITS,
            SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAIT_DETAILS,
            SearchNodeKeys.MODULAR_GEAR_RUNTIME_MATERIALS,
            SearchNodeKeys.MODULAR_GEAR_RUNTIME_TRAITS,
            SearchNodeKeys.MODULAR_GEAR_RUNTIME_STATS,
            SearchNodeKeys.REQUIRED_TOOL,
            SearchNodeKeys.ACCESS_LEVEL,
            SearchNodeKeys.OBTAINABILITY,
            SearchNodeKeys.VARIANT_SOURCE,
            SearchNodeKeys.VARIANT_AXES,
            SearchNodeKeys.VARIANT_COLLAPSE_MODE,
            SearchNodeKeys.SEARCH_TOKENS
    );
    private final TrieNode root = new TrieNode();
    private final Set<SearchNode> allNodes = new LinkedHashSet<>();
    private final Map<SearchNode, String> searchableText = new LinkedHashMap<>();
    private final boolean includeMetadata;
    public SearchIndex() {
        this(true);
    }

    public SearchIndex(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }

    private static List<String> searchableKeys(SearchNode node, boolean includeMetadata) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.add(node.displayName());
        keys.add(node.id().toString());
        keys.add(node.id().getNamespace());
        keys.add(node.id().getPath());

        if (includeMetadata) {
            for (var entry : node.metadata().entrySet()) {
                if (!isSearchableMetadataKey(entry.getKey())) continue;
                addMetadataAliases(keys, entry.getValue());
            }
        }
        return new ArrayList<>(keys);
    }

    private static boolean isSearchableMetadataKey(String key) {
        if (SEARCHABLE_METADATA_KEYS.contains(key)) {
            return true;
        }
        String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return normalized.endsWith("facts")
                || normalized.endsWith("itemkind")
                || normalized.endsWith("tier")
                || normalized.endsWith("role")
                || normalized.endsWith("roles")
                || normalized.endsWith("family")
                || normalized.endsWith("families");
    }

    private static void addMetadataAliases(Set<String> keys, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return;
        keys.add(rawValue);

        // Split by all common delimiters
        for (String part : rawValue.split("[,:/\\s\\-]+")) {
            if (!part.isBlank()) keys.add(part);
        }

        // Add variants with spaces
        keys.add(rawValue.replace('_', ' ').replace('-', ' '));
        keys.add(rawValue.replace(':', ' '));
    }

    private static String normalizeSearchText(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replace(':', ' ')
                .trim();
    }

    /**
     * Add a node into the trie. Mutations are synchronized to keep the trie safe
     * during concurrent indexing; reads remain lock-free.
     */
    public void addNode(SearchNode node) {
        synchronized (this) {
            for (String key : searchableKeys(node)) {
                addKey(key, node);
            }
            allNodes.add(node);
            searchableText.put(node, searchableHaystack(node));
        }
    }

    /**
     * Prefix search: walk the trie to the prefix node then BFS collect results.
     */
    public List<SearchNode> prefixSearch(String prefix) {
        if (prefix == null || prefix.isEmpty()) return Collections.emptyList();
        String low = normalizeSearchText(prefix);
        if (low.isEmpty()) return Collections.emptyList();
        TrieNode cur = root;
        for (int i = 0; i < low.length(); i++) {
            TrieNode next = cur.children.get(low.charAt(i));
            if (next == null) return Collections.emptyList();
            cur = next;
        }

        LinkedHashSet<SearchNode> out = new LinkedHashSet<>();
        Deque<TrieNode> dq = new ArrayDeque<>();
        dq.add(cur);
        while (!dq.isEmpty()) {
            TrieNode t = dq.poll();
            out.addAll(t.hits);
            for (TrieNode child : t.children.values()) {
                dq.add(child);
            }
        }
        return new ArrayList<>(out);
    }

    /**
     * Substring fallback scan.
     */
    public List<SearchNode> substringSearch(String substring) {
        if (substring == null || substring.isEmpty()) return Collections.emptyList();
        String low = normalizeSearchText(substring);
        if (low.isEmpty()) return Collections.emptyList();
        List<SearchNode> out = new ArrayList<>();
        for (SearchNode n : allNodes) {
            if (searchableText.getOrDefault(n, n.displayName().toLowerCase(Locale.ROOT)).contains(low)) {
                out.add(n);
            }
        }
        return out;
    }

    private void addKey(String key, SearchNode node) {
        if (key == null || key.isBlank()) return;
        String normalized = normalizeSearchText(key);
        if (normalized.isBlank()) return;

        // Index the full string
        addPrefix(normalized, node);

        // Also index individual words if the string has spaces
        if (normalized.contains(" ")) {
            String[] words = normalized.split("\\s+");
            for (String word : words) {
                if (word.length() > 1) {
                    addPrefix(word, node);
                }
            }
        }
    }

    private void addPrefix(String text, SearchNode node) {
        TrieNode cur = root;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            TrieNode next = cur.children.get(c);
            if (next == null) {
                next = new TrieNode();
                cur.children.put(c, next);
            }
            cur = next;
        }
        cur.hits.add(node);
    }

    private List<String> searchableKeys(SearchNode node) {
        return searchableKeys(node, includeMetadata);
    }

    private String searchableHaystack(SearchNode node) {
        StringBuilder sb = new StringBuilder();
        for (String key : searchableKeys(node)) {
            sb.append(key).append(" ");
        }
        return normalizeSearchText(sb.toString());
    }

    private static final class TrieNode {
        final Char2ObjectOpenHashMap<TrieNode> children = new Char2ObjectOpenHashMap<>();
        final LinkedHashSet<SearchNode> hits = new LinkedHashSet<>();
    }
}
