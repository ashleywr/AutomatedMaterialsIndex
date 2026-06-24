package com.sanhiruzu.ami.index;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.client.AmiDebugSettings;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.config.ConfigValue;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.*;
import java.time.Instant;

public final class SearchNodeMirrorDump {
    private static final Gson GSON = new Gson();
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    private SearchNodeMirrorDump() {
    }

    public static int writeJsonl(Path path, List<SearchNode> nodes) throws IOException {
        Files.createDirectories(path.getParent());
        List<String> lines = new ArrayList<>(nodes.size());
        for (SearchNode node : nodes) {
            lines.add(GSON.toJson(Snapshot.from(node)));
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
        return lines.size();
    }

    public static void writeMeta(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, PRETTY_GSON.toJson(metaSnapshot()), StandardCharsets.UTF_8);
    }

    public static List<SearchNode> readJsonl(Path path) throws IOException {
        List<SearchNode> nodes = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                JsonObject object = JsonParser.parseString(line).getAsJsonObject();
                Snapshot snapshot = GSON.fromJson(object, Snapshot.class);
                nodes.add(snapshot.toNode());
            }
        }
        return nodes;
    }

    public static List<SearchNode> reclassifyItemOntology(List<SearchNode> nodes) {
        List<SearchNode> reclassified = new ArrayList<>(nodes.size());
        for (SearchNode node : nodes) {
            if (node.type() != NodeType.ITEM) {
                reclassified.add(node);
                continue;
            }

            Map<String, String> metadata = new LinkedHashMap<>(node.metadata());
            removeClassifierDiagnostics(metadata);
            normalizeConnectingVariantMaterial(metadata);
            enrichReplayClassificationFacts(node.id(), metadata);
            FacetProfile profile = new FacetProfile(
                    FacetCodec.decode(metadata.get(SearchNodeKeys.FACETS)),
                    metadata
            );
            CategoryAssignment assignment = PrimaryCategoryResolver.resolve(node.id(), profile);
            metadata.putAll(assignment.attributes());
            metadata.put(SearchNodeKeys.ONTOLOGY_CATEGORY, assignment.categoryId());
            metadata.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, assignment.subcategoryId());

            SearchNode copy = new SearchNode(
                    node.id(),
                    node.type(),
                    node.displayName(),
                    node.color(),
                    node.searchWeight(),
                    metadata
            );
            copyUnresolvedEdges(node, copy);
            reclassified.add(copy);
        }
        return reclassified;
    }

    private static void removeClassifierDiagnostics(Map<String, String> metadata) {
        metadata.remove("classificationMode");
        metadata.remove("classificationScore");
        metadata.remove("classificationScores");
        metadata.remove("classificationEvidence");
        metadata.remove(SearchNodeKeys.COMPAT_CATEGORY_POLICY);
        metadata.remove(SearchNodeKeys.CLASSIFICATION_ROUTE);
        metadata.remove(SearchNodeKeys.CLASSIFICATION_TRACE);
        metadata.remove(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE);
        metadata.remove(SearchNodeKeys.CLASSIFICATION_ROUTE_RULE);
    }

    private static void normalizeConnectingVariantMaterial(Map<String, String> metadata) {
        canonicalizeResourcePathSuffix(metadata, SearchNodeKeys.MATERIAL_GROUP, "_connecting");
        canonicalizeResourcePathSuffix(metadata, SearchNodeKeys.SUBTYPE_OF, "_connecting");
    }

    private static void enrichReplayClassificationFacts(ResourceLocation id, Map<String, String> metadata) {
        String componentFacts = metadata.getOrDefault(SearchNodeKeys.COMPONENT_FACTS, "");
        if (hasToken(componentFacts, "container")) {
            SemanticVerbCodec.add(metadata, SemanticVerb.STORES_ITEMS, "replay:component:container");
        }
        if (hasToken(componentFacts, "bundle_contents")) {
            SemanticVerbCodec.add(metadata, SemanticVerb.STORES_ITEMS, "replay:component:bundle_contents");
        }

        if (hasToken(metadata.getOrDefault(SearchNodeKeys.BLOCK_TAGS, ""), "minecraft:beds")) {
            SemanticVerbCodec.add(metadata, SemanticVerb.SLEEP_REST, "replay:block_tag:minecraft:beds");
        }

        String normalizedBlockClass = metadata.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "").toLowerCase(Locale.ROOT);
        if (containsAny(normalizedBlockClass,
                "bedblock", "petbedblock", "dogbedblock", "sleepingbagblock", "bedrollblock")) {
            SemanticVerbCodec.add(metadata, SemanticVerb.SLEEP_REST, "replay:block_class:" + normalizedBlockClass);
        }
        if (isMagicStructureBlockClass(metadata.getOrDefault(SearchNodeKeys.BLOCK_CLASS, ""))) {
            addReplayFacet(metadata, ItemFacet.MAGIC_ARTIFACT);
        }
        if (containsAny(normalizedBlockClass, "storageterminalblock", "lockerblock", "cofferblock")) {
            SemanticVerbCodec.add(metadata, SemanticVerb.STORES_ITEMS, "replay:block_class:" + normalizedBlockClass);
        }
        if ("minecolonies".equals(id.getNamespace()) && containsAny(normalizedBlockClass, "blockhut")) {
            SemanticVerbCodec.add(metadata, SemanticVerb.SETTLEMENT_WORKSITE, "replay:block_class:" + normalizedBlockClass);
        }

        if (containsStorageTerminalPhrase(id.getPath())
                || containsStorageTerminalPhrase(resourcePath(metadata.get(SearchNodeKeys.SUBTYPE_OF)))
                || containsStorageTerminalPhrase(resourcePath(metadata.get(SearchNodeKeys.MATERIAL_GROUP)))) {
            SemanticVerbCodec.add(metadata, SemanticVerb.STORES_ITEMS, "replay:path_phrase:storage_terminal");
        }
    }

    private static boolean isMagicStructureBlockClass(String blockClass) {
        return "net.minecraft.world.level.block.ConduitBlock".equals(blockClass)
                || "net.minecraft.world.level.block.BeaconBlock".equals(blockClass)
                || "net.minecraft.world.level.block.EnchantmentTableBlock".equals(blockClass)
                || "net.minecraft.world.level.block.EnchantingTableBlock".equals(blockClass)
                || "net.minecraft.world.level.block.EndPortalFrameBlock".equals(blockClass);
    }

    private static void addReplayFacet(Map<String, String> metadata, ItemFacet facet) {
        EnumSet<ItemFacet> facets = FacetCodec.decode(metadata.get(SearchNodeKeys.FACETS));
        if (facets.add(facet)) {
            metadata.put(SearchNodeKeys.FACETS, FacetCodec.encode(facets));
        }
    }

    private static boolean hasToken(String encoded, String token) {
        if (encoded == null || encoded.isBlank() || token == null || token.isBlank()) {
            return false;
        }
        for (String raw : encoded.split(",")) {
            if (raw.trim().equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsStorageTerminalPhrase(String path) {
        return containsPathPhrase(path, "storage_terminal");
    }

    private static boolean containsPathPhrase(String path, String phrase) {
        if (path == null || path.isBlank() || phrase == null || phrase.isBlank()) {
            return false;
        }
        String normalizedPath = path.toLowerCase(Locale.ROOT);
        String normalizedPhrase = phrase.toLowerCase(Locale.ROOT);
        return normalizedPath.equals(normalizedPhrase)
                || normalizedPath.endsWith("_" + normalizedPhrase)
                || normalizedPath.endsWith("/" + normalizedPhrase)
                || normalizedPath.contains("/" + normalizedPhrase + "/");
    }

    private static String resourcePath(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int separator = value.indexOf(':');
        return separator >= 0 && separator + 1 < value.length() ? value.substring(separator + 1) : value;
    }

    private static void canonicalizeResourcePathSuffix(Map<String, String> metadata, String key, String suffix) {
        String value = metadata.get(key);
        if (value == null || value.isBlank()) {
            return;
        }
        int separator = value.indexOf(':');
        if (separator < 0) {
            if (value.endsWith(suffix)) {
                metadata.put(key, value.substring(0, value.length() - suffix.length()));
            }
            return;
        }
        String namespace = value.substring(0, separator);
        String path = value.substring(separator + 1);
        if (path.endsWith(suffix)) {
            metadata.put(key, namespace + ":" + path.substring(0, path.length() - suffix.length()));
        }
    }

    public static List<SearchNode> runtimeAtlasNodes() {
        List<SearchNode> nodes = new ArrayList<>();
        GlobalIndex index = GlobalIndex.getInstance();
        for (NodeType type : NodeType.values()) {
            nodes.addAll(index.getNodes(type));
        }
        return nodes;
    }

    private static void copyUnresolvedEdges(SearchNode source, SearchNode target) {
        for (EdgeType edgeType : EdgeType.values()) {
            for (ResourceLocation edgeId : source.getUnresolvedEdgeIds(edgeType)) {
                target.addUnresolvedEdge(edgeType, edgeId);
            }
        }
    }

    public static RuntimeDumpMeta metaSnapshot() {
        GlobalIndex index = GlobalIndex.getInstance();
        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        int total = 0;
        for (NodeType type : NodeType.values()) {
            int count = index.getNodes(type).size();
            typeCounts.put(type.name(), count);
            total += count;
        }

        Map<String, String> config = new LinkedHashMap<>();
        for (Field field : AmiConfig.class.getFields()) {
            ConfigValue configValue = field.getAnnotation(ConfigValue.class);
            if (configValue == null) {
                continue;
            }
            try {
                Object value = field.get(null);
                if (value != null) {
                    config.put(configValue.value(), String.valueOf(value));
                }
            } catch (IllegalAccessException ignored) {
                // Metadata capture is best effort only.
            }
        }

        return new RuntimeDumpMeta(
                "search_nodes",
                Instant.now().toString(),
                AmiDebugSettings.versionLabel(),
                AmiDebugSettings.debugBuild(),
                indexRuntimeState(),
                index.isIndexReady(),
                typeCounts,
                total,
                GlobalIndexCache.computeModListHash(),
                config
        );
    }

    private static Map<String, String> indexRuntimeState() {
        Map<String, String> state = new LinkedHashMap<>();
        state.put("indexReady", String.valueOf(GlobalIndex.getInstance().isIndexReady()));
        state.put("structureLoading", String.valueOf(GlobalIndex.getInstance().isLoading(NodeType.STRUCTURE)));
        state.put("dimensionLoading", String.valueOf(GlobalIndex.getInstance().isLoading(NodeType.DIMENSION)));
        state.put("showCreativeItems", String.valueOf(AmiConfig.showCreativeItems));
        state.put("showHiddenModItems", String.valueOf(AmiConfig.showHiddenModItems));
        state.put("cheatMode", String.valueOf(AmiConfig.cheatMode));
        state.put("devMode", String.valueOf(AmiConfig.devMode));
        state.put("strictSurvivalMode", String.valueOf(AmiConfig.strictSurvivalMode));
        return state;
    }

    private record Snapshot(
            String id,
            String type,
            String displayName,
            int color,
            int searchWeight,
            Map<String, String> metadata,
            Map<String, List<String>> unresolvedEdges
    ) {
        static Snapshot from(SearchNode node) {
            Map<String, String> metadata = new LinkedHashMap<>(node.metadata());
            Map<String, List<String>> edges = new LinkedHashMap<>();
            for (EdgeType edgeType : EdgeType.values()) {
                List<String> ids = node.getUnresolvedEdgeIds(edgeType).stream()
                        .map(ResourceLocation::toString)
                        .sorted()
                        .toList();
                if (!ids.isEmpty()) {
                    edges.put(edgeType.name(), ids);
                }
            }
            return new Snapshot(
                    node.id().toString(),
                    node.type().name(),
                    node.displayName(),
                    node.color(),
                    node.searchWeight(),
                    metadata,
                    edges
            );
        }

        SearchNode toNode() {
            ResourceLocation parsedId = ResourceLocation.tryParse(id);
            if (parsedId == null) {
                throw new IllegalArgumentException("Invalid SearchNode id in mirror dump: " + id);
            }
            SearchNode node = new SearchNode(
                    parsedId,
                    NodeType.valueOf(type),
                    displayName,
                    color,
                    searchWeight,
                    metadata == null ? Map.of() : metadata
            );
            if (unresolvedEdges != null) {
                Map<EdgeType, List<String>> sorted = new EnumMap<>(EdgeType.class);
                for (var entry : unresolvedEdges.entrySet()) {
                    sorted.put(EdgeType.valueOf(entry.getKey()), entry.getValue());
                }
                sorted.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name)))
                        .forEach(entry -> {
                            for (String target : entry.getValue()) {
                                ResourceLocation parsedTarget = ResourceLocation.tryParse(target);
                                if (parsedTarget != null) {
                                    node.addUnresolvedEdge(entry.getKey(), parsedTarget);
                                }
                            }
                        });
            }
            return node;
        }
    }

    public record RuntimeDumpMeta(
            String dumpType,
            String generatedAtUtc,
            String amiVersion,
            boolean debugBuild,
            Map<String, String> runtimeState,
            boolean indexReady,
            Map<String, Integer> indexTypeCounts,
            int totalIndexNodes,
            String indexFingerprint,
            Map<String, String> config
    ) {
    }
}
