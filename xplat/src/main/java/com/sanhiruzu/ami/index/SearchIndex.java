package com.sanhiruzu.ami.index;

import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.text.Normalizer;
import java.util.*;

/**
 * Tokenized Trie implementation using fastutil's primitive-char map to avoid boxing.
 * Fast for prefix searches and has a simple substring fallback.
 */
public final class SearchIndex {
    private static final Set<String> PLAIN_SEARCHABLE_METADATA_KEYS = Set.of(
            SearchNodeKeys.PLAIN_SEARCH_TOKENS,
            SearchNodeKeys.DESCRIPTION_SEARCH_TOKENS,
            SearchNodeKeys.TOOLTIP_SEARCH_TOKENS
    );
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
            SearchNodeKeys.GREGTECH_ITEM_KIND,
            SearchNodeKeys.GREGTECH_FACTS,
            SearchNodeKeys.GREGTECH_TIER,
            SearchNodeKeys.GREGTECH_CIRCUIT_GRADE,
            SearchNodeKeys.GREGTECH_ENERGY_ROLE,
            SearchNodeKeys.GREGTECH_EU_CONSUMPTION,
            SearchNodeKeys.GREGTECH_EU_GENERATION,
            SearchNodeKeys.GREGTECH_EU_INPUT,
            SearchNodeKeys.GREGTECH_EU_OUTPUT,
            SearchNodeKeys.GREGTECH_AMPERAGE,
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
            SearchNodeKeys.MODULAR_GOLEMS_ITEM_KIND,
            SearchNodeKeys.MODULAR_GOLEMS_FACTS,
            SearchNodeKeys.MODULAR_GOLEMS_GOLEM_TYPE,
            SearchNodeKeys.MODULAR_GOLEMS_PART,
            SearchNodeKeys.REQUIRED_TOOL,
            SearchNodeKeys.ACCESS_LEVEL,
            SearchNodeKeys.OBTAINABILITY,
            SearchNodeKeys.VARIANT_SOURCE,
            SearchNodeKeys.VARIANT_AXES,
            SearchNodeKeys.VARIANT_COLLAPSE_MODE,
            SearchNodeKeys.SEARCH_TOKENS,
            SearchNodeKeys.PLAIN_SEARCH_TOKENS,
            SearchNodeKeys.TOOLTIP_SEARCH_TOKENS
    );
    private final TrieNode root = new TrieNode();
    private final Set<SearchNode> allNodes = new LinkedHashSet<>();
    private final Map<SearchNode, String> searchableText = new LinkedHashMap<>();
    private final Map<String, ArrayList<SearchNode>> trigramIndex = new HashMap<>();
    // Reused across addNode() calls (build thread only) to avoid per-node HashSet allocation.
    private final IntOpenHashSet trigramSeenHashes = new IntOpenHashSet(512, 0.75f);
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
        addMetadataAliases(keys, node.meta(SearchNodeKeys.COLLAPSE_LABEL, ""));
        addPlainMetadataAliases(keys, node);

        if (includeMetadata) {
            for (var entry : node.metadata().entrySet()) {
                if (!isSearchableMetadataKey(entry.getKey())) continue;
                addMetadataAliases(keys, entry.getValue());
            }
        }
        return new ArrayList<>(keys);
    }

    private static void addPlainMetadataAliases(Set<String> keys, SearchNode node) {
        for (String key : PLAIN_SEARCHABLE_METADATA_KEYS) {
            addMetadataAliases(keys, node.meta(key, ""));
        }
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
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replace(':', ' ')
                .trim();
    }

    /**
     * Add a node into the trie. Must only be called from the build thread before the
     * SearchService is published; concurrent reads are safe after publication due to
     * the volatile write on AmiIndexerService.searchService establishing happens-before.
     */
    public void addNode(SearchNode node) {
        List<String> keys = searchableKeys(node);
        for (String key : keys) {
            addKey(key, node);
        }
        allNodes.add(node);
        String haystack = buildHaystack(keys);
        searchableText.put(node, haystack);
        indexTrigrams(haystack, node);
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

        List<SearchNode> out = new ArrayList<>();
        Set<SearchNode> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<TrieNode> dq = new ArrayDeque<>();
        dq.add(cur);
        while (!dq.isEmpty()) {
            TrieNode t = dq.poll();
            if (t.hits != null) {
                for (SearchNode node : t.hits) {
                    if (seen.add(node)) out.add(node);
                }
            }
            for (TrieNode child : t.children.values()) {
                dq.add(child);
            }
        }
        return out;
    }

    /**
     * Substring search: trigram index for queries ≥3 chars, linear scan for shorter queries.
     */
    public List<SearchNode> substringSearch(String substring) {
        if (substring == null || substring.isEmpty()) return Collections.emptyList();
        String low = normalizeSearchText(substring);
        if (low.isEmpty()) return Collections.emptyList();
        if (low.length() >= 3) {
            return trigramSubstringSearch(low);
        }
        // Short query: linear scan (uncommon path)
        List<SearchNode> out = new ArrayList<>();
        for (SearchNode n : allNodes) {
            if (searchableText.getOrDefault(n, n.displayName().toLowerCase(Locale.ROOT)).contains(low)) {
                out.add(n);
            }
        }
        return out;
    }

    private void indexTrigrams(String haystack, SearchNode node) {
        int len = haystack.length();
        if (len < 3) return;
        // Pack three chars into one int (each char fits in 7 bits for normalized lowercase text)
        // to avoid allocating a substring just to check if the trigram was already seen.
        trigramSeenHashes.clear();
        for (int i = 0; i <= len - 3; i++) {
            int hash = (haystack.charAt(i) << 14) | (haystack.charAt(i + 1) << 7) | haystack.charAt(i + 2);
            if (trigramSeenHashes.add(hash)) {
                String tri = haystack.substring(i, i + 3);
                trigramIndex.computeIfAbsent(tri, k -> new ArrayList<>()).add(node);
            }
        }
    }

    private List<SearchNode> trigramSubstringSearch(String low) {
        int len = low.length();
        // Collect all trigrams from the query
        List<String> trigrams = new ArrayList<>(len - 2);
        for (int i = 0; i <= len - 3; i++) {
            trigrams.add(low.substring(i, i + 3));
        }

        // Find the smallest bucket to minimize the initial candidate set
        ArrayList<SearchNode> smallest = null;
        for (String tri : trigrams) {
            ArrayList<SearchNode> bucket = trigramIndex.get(tri);
            if (bucket == null) return Collections.emptyList(); // no match possible
            if (smallest == null || bucket.size() < smallest.size()) {
                smallest = bucket;
            }
        }
        if (smallest == null) return Collections.emptyList();

        // Build candidate set from smallest bucket (identity comparison — instances are canonical)
        Set<SearchNode> candidates = Collections.newSetFromMap(new IdentityHashMap<>(smallest.size() * 2));
        candidates.addAll(smallest);

        // Intersect with remaining trigram buckets
        for (String tri : trigrams) {
            ArrayList<SearchNode> bucket = trigramIndex.get(tri);
            if (bucket == smallest) continue;
            Set<SearchNode> bucketSet = Collections.newSetFromMap(new IdentityHashMap<>(bucket.size() * 2));
            bucketSet.addAll(bucket);
            candidates.retainAll(bucketSet);
            if (candidates.isEmpty()) return Collections.emptyList();
        }

        // Exact verification against stored haystack (trigrams can produce false positives across word boundaries)
        List<SearchNode> out = new ArrayList<>(candidates.size());
        for (SearchNode n : candidates) {
            String haystack = searchableText.get(n);
            if (haystack != null && haystack.contains(low)) {
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
        if (cur.hits == null) cur.hits = new ArrayList<>(2);
        cur.hits.add(node);
    }

    private List<String> searchableKeys(SearchNode node) {
        return searchableKeys(node, includeMetadata);
    }

    private String buildHaystack(List<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            sb.append(key).append(" ");
        }
        return normalizeSearchText(sb.toString());
    }

    private static final class TrieNode {
        // Initial capacity 2 — most trie nodes have 1–2 children.
        final Char2ObjectOpenHashMap<TrieNode> children = new Char2ObjectOpenHashMap<>(2, 0.9f);
        // Lazily initialized — most intermediate trie nodes have no hits.
        @org.jetbrains.annotations.Nullable List<SearchNode> hits = null;
    }
}
