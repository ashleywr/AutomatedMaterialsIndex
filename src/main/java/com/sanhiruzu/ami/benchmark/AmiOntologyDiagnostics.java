package com.sanhiruzu.ami.benchmark;

import com.sanhiruzu.ami.index.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AmiOntologyDiagnostics {
    private AmiOntologyDiagnostics() {
    }

    public static void exportOntologyCsv(Path csvFile) throws IOException {
        Files.createDirectories(csvFile.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(csvFile)) {
            writer.write("ItemID,ModID,DisplayName,AMI_Category,AMI_Subcategory,Facets,Tags,BlocksMaterial,VariantGroup,RequiredTool,CreativeTabLabel,Visibility,AccessLevel,Obtainability,SubtypeOf\n");

            List<SearchNode> items = GlobalIndex.getInstance().getNodes(NodeType.ITEM);
            for (SearchNode node : items) {
                String itemId = node.id().toString();
                String modId = node.id().getNamespace();
                String displayName = escapeCsv(node.displayName());
                String category = node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
                if (category.isEmpty()) {
                    category = AmiOntology.classifyNode(node).id;
                }
                String subcategory = escapeCsv(node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, ""));
                String facets = escapeCsv(node.meta(SearchNodeKeys.FACETS, ""));
                String tags = escapeCsv(node.meta(SearchNodeKeys.TAGS, ""));
                String blocksMaterial = escapeCsv(node.meta(SearchNodeKeys.BLOCKS_MATERIAL, ""));
                String variantGroup = escapeCsv(node.meta(SearchNodeKeys.VARIANT_GROUP, ""));
                String requiredTool = escapeCsv(node.meta(SearchNodeKeys.REQUIRED_TOOL, ""));
                String creativeTabLabel = escapeCsv(node.meta(SearchNodeKeys.CREATIVE_TAB_LABEL, ""));
                String visibility = escapeCsv(node.meta(SearchNodeKeys.VISIBILITY, ""));
                String accessLevel = escapeCsv(node.meta(SearchNodeKeys.ACCESS_LEVEL, ""));
                String obtainability = escapeCsv(node.meta(SearchNodeKeys.OBTAINABILITY, ""));
                String subtypeOf = escapeCsv(node.meta(SearchNodeKeys.SUBTYPE_OF, ""));

                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        itemId,
                        modId,
                        displayName,
                        category,
                        subcategory,
                        facets,
                        tags,
                        blocksMaterial,
                        variantGroup,
                        requiredTool,
                        creativeTabLabel,
                        visibility,
                        accessLevel,
                        obtainability,
                        subtypeOf));
            }
        }
    }

    public static FallbackSampleSummary exportFacetFallbackCsv(Path csvFile) throws IOException {
        Files.createDirectories(csvFile.getParent());

        int totalItems = 0;
        int facetlessItems = 0;
        int unresolvedFacetfulItems = 0;
        int playerVisibleItems = 0;
        int playerVisibleFacetlessItems = 0;
        int playerVisibleUnresolvedFacetfulItems = 0;
        Map<String, Integer> legacyCategoryCounts = new LinkedHashMap<>();
        Map<String, Integer> playerVisibleLegacyCategoryCounts = new LinkedHashMap<>();
        Map<String, Integer> visibilityCounts = new LinkedHashMap<>();
        Map<String, Integer> accessLevelCounts = new LinkedHashMap<>();

        try (BufferedWriter writer = Files.newBufferedWriter(csvFile)) {
            writer.write("ItemID,ModID,DisplayName,FacetCount,Facets,AssignedCategory,AssignedSubcategory,LegacyCategory,LegacySubcategory,FallbackCandidate,Visibility,AccessLevel,SubtypeOf,PlayerVisible\n");

            List<SearchNode> items = GlobalIndex.getInstance().getNodes(NodeType.ITEM);
            for (SearchNode node : items) {
                totalItems++;

                String itemId = node.id().toString();
                String modId = node.id().getNamespace();
                String displayName = escapeCsv(node.displayName());
                String encodedFacets = node.meta(SearchNodeKeys.FACETS, "");
                var facets = FacetCodec.decode(encodedFacets);
                boolean fallbackCandidate = facets.isEmpty();
                String assignedCategory = node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
                String assignedSubcategory = node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
                String visibility = node.meta(SearchNodeKeys.VISIBILITY, "");
                String accessLevel = node.meta(SearchNodeKeys.ACCESS_LEVEL, "");
                String subtypeOf = node.meta(SearchNodeKeys.SUBTYPE_OF, "");
                boolean playerVisible = isPlayerVisible(node, visibility, accessLevel, subtypeOf);

                visibilityCounts.merge(visibility.isBlank() ? "<default>" : visibility, 1, Integer::sum);
                accessLevelCounts.merge(accessLevel.isBlank() ? "<default>" : accessLevel, 1, Integer::sum);
                if (playerVisible) {
                    playerVisibleItems++;
                }

                String legacyCategory = "";
                String legacySubcategory = "";
                if (fallbackCandidate) {
                    facetlessItems++;
                    if (playerVisible) {
                        playerVisibleFacetlessItems++;
                    }
                    Item item = BuiltInRegistries.ITEM.getOptional(node.id()).orElse(null);
                    if (item != null) {
                        String[] ontology = OntologyClassifier.classifyItem(item, node.id());
                        if (ontology != null) {
                            legacyCategory = ontology[0];
                            if (ontology.length > 1) {
                                legacySubcategory = ontology[1];
                            }
                            legacyCategoryCounts.merge(legacyCategory, 1, Integer::sum);
                            if (playerVisible) {
                                playerVisibleLegacyCategoryCounts.merge(legacyCategory, 1, Integer::sum);
                            }
                        } else {
                            legacyCategoryCounts.merge("runtime", 1, Integer::sum);
                            if (playerVisible) {
                                playerVisibleLegacyCategoryCounts.merge("runtime", 1, Integer::sum);
                            }
                        }
                    } else {
                        legacyCategoryCounts.merge("missing_registry_item", 1, Integer::sum);
                        if (playerVisible) {
                            playerVisibleLegacyCategoryCounts.merge("missing_registry_item", 1, Integer::sum);
                        }
                    }
                } else if (assignedCategory.isEmpty()) {
                    unresolvedFacetfulItems++;
                    if (playerVisible) {
                        playerVisibleUnresolvedFacetfulItems++;
                    }
                }

                writer.write(String.format("\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,\"%s\",\"%s\",\"%s\",%s\n",
                        itemId,
                        modId,
                        displayName,
                        facets.size(),
                        escapeCsv(encodedFacets),
                        escapeCsv(assignedCategory),
                        escapeCsv(assignedSubcategory),
                        escapeCsv(legacyCategory),
                        escapeCsv(legacySubcategory),
                        Boolean.toString(fallbackCandidate),
                        escapeCsv(visibility),
                        escapeCsv(accessLevel),
                        escapeCsv(subtypeOf),
                        Boolean.toString(playerVisible)));
            }
        }

        return new FallbackSampleSummary(
                totalItems,
                facetlessItems,
                unresolvedFacetfulItems,
                playerVisibleItems,
                playerVisibleFacetlessItems,
                playerVisibleUnresolvedFacetfulItems,
                Map.copyOf(legacyCategoryCounts),
                Map.copyOf(playerVisibleLegacyCategoryCounts),
                Map.copyOf(visibilityCounts),
                Map.copyOf(accessLevelCounts)
        );
    }

    private static boolean isPlayerVisible(SearchNode node, String visibility, String accessLevel, String subtypeOf) {
        if ("hidden".equals(visibility)) {
            return false;
        }
        if ("dev".equals(accessLevel)) {
            return false;
        }
        if (!subtypeOf.isBlank()) {
            return false;
        }
        String id = node.id().toString();
        return !id.startsWith("ami:hero/");
    }

    private static String escapeCsv(String value) {
        return value == null ? "" : value.replace("\"", "\"\"");
    }

    public record FallbackSampleSummary(
            int totalItems,
            int facetlessItems,
            int unresolvedFacetfulItems,
            int playerVisibleItems,
            int playerVisibleFacetlessItems,
            int playerVisibleUnresolvedFacetfulItems,
            Map<String, Integer> legacyCategoryCounts,
            Map<String, Integer> playerVisibleLegacyCategoryCounts,
            Map<String, Integer> visibilityCounts,
            Map<String, Integer> accessLevelCounts
    ) {
    }
}
