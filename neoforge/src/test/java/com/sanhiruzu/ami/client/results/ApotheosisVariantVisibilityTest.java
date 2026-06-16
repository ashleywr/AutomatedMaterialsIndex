package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.SearchService;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApotheosisVariantVisibilityTest {
    private static final Identifier TWILIGHT_FOREST_GEM_ID =
            new Identifier("apotheosis", "gem/variant/gem_of_the_twilight_forest_101");

    @BeforeEach
    void setUp() {
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
    }

    @Test
    void twilightForestGemVariantSurvivesSearchAndProjection() {
        GlobalIndex index = GlobalIndex.getInstance();
        SearchNode twilightForestGem = gemVariant(TWILIGHT_FOREST_GEM_ID, "Gem of the Twilight Forest");
        index.addNode(twilightForestGem);
        index.addNode(gemVariant("gem_of_the_sky_102", "Gem of the Sky"));
        index.addNode(gemVariant("gem_of_the_earth_103", "Gem of the Earth"));
        index.addNode(gemVariant("gem_of_the_seas_104", "Gem of the Seas"));

        SearchService service = SearchService.buildFrom(index, false);

        List<SearchNode> directHits = service.query("@apotheosis twilight forest gem")
                .getOrDefault(NodeType.ITEM, List.of());
        assertEquals(List.of(twilightForestGem), directHits);

        SearchState state = new SearchState();
        state.setQuery("@apotheosis twilight forest gem");
        state.setViewMode(ResultsToolbar.ViewMode.LIST);
        state.setGroupBy(ResultsProcessor.GroupBy.CATEGORY);

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(
                index.getNodes(NodeType.ITEM),
                state,
                service,
                false,
                false
        );

        assertEquals(1, projection.displayedItemCount());
        assertTrue(projectedLeafIds(projection.roots()).contains(TWILIGHT_FOREST_GEM_ID));
    }

    private static SearchNode gemVariant(String variantPath, String displayName) {
        return gemVariant(new Identifier("apotheosis", "gem/variant/" + variantPath), displayName);
    }

    private static SearchNode gemVariant(Identifier id, String displayName) {
        return new SearchNode(
                id,
                NodeType.ITEM,
                displayName,
                0,
                0,
                Map.of(
                        SearchNodeKeys.MOD_ID, "apotheosis",
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "apotheosis",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "gems",
                        SearchNodeKeys.SUBTYPE_OF, "apotheosis:gem",
                        SearchNodeKeys.VARIANT_SOURCE, "creative_tab",
                        SearchNodeKeys.VARIANT_COLLAPSE_MODE, "auto"
                )
        );
    }

    private static List<Identifier> projectedLeafIds(List<TreeNode> roots) {
        List<Identifier> result = new ArrayList<>();
        collectProjectedLeafIds(roots, result);
        return result;
    }

    private static void collectProjectedLeafIds(List<TreeNode> nodes, List<Identifier> result) {
        for (TreeNode node : nodes) {
            if (node.isLeaf()) {
                result.add(node.getEntry().id());
            } else {
                collectProjectedLeafIds(node.getChildren(), result);
            }
        }
    }
}
