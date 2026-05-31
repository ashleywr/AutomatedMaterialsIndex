package com.sanhiruzu.ami.index;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class SearchNodeMirrorDump {
    private static final Gson GSON = new Gson();

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
        metadata.remove(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE);
        metadata.remove(SearchNodeKeys.CLASSIFICATION_ROUTE_RULE);
    }

    private static void normalizeConnectingVariantMaterial(Map<String, String> metadata) {
        canonicalizeResourcePathSuffix(metadata, SearchNodeKeys.MATERIAL_GROUP, "_connecting");
        canonicalizeResourcePathSuffix(metadata, SearchNodeKeys.SUBTYPE_OF, "_connecting");
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
        for (NodeType type : NodeType.atlasValues()) {
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
}
