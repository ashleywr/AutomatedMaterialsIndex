package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.SearchService;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollapseLabelSearchTest {
    @BeforeEach
    void setUp() {
        AmiConfig.resetToDefaults();
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
    }

    @Test
    void collapsedFamilyLabelKeepsPluralSearchResultsVisible() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(shard("magenta", "Magenta Glass Shard"));
        index.addNode(shard("red", "Red Glass Shard"));
        index.addNode(shard("blue", "Blue Glass Shard"));
        index.addNode(shard("green", "Green Glass Shard"));

        SearchService service = SearchService.buildFrom(index, false);
        assertEquals(4, service.query("shard").getOrDefault(NodeType.ITEM, List.of()).size());
        assertEquals(4, service.query("shards").getOrDefault(NodeType.ITEM, List.of()).size());

        SearchState state = new SearchState();
        state.setQuery("shards");
        state.setViewMode(ResultsToolbar.ViewMode.GRID);
        state.setGroupBy(ResultsProcessor.GroupBy.CATEGORY);

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(
                index.getNodes(NodeType.ITEM),
                state,
                service,
                false,
                false
        );

        assertEquals(4, projection.displayedItemCount());
        assertTrue(containsLabel(projection.roots(), "Glass Shards"), ResultsTreeDump.dump(projection.roots()));
    }

    private static SearchNode shard(String color, String displayName) {
        return new SearchNode(
                new Identifier("quark", color + "_shard"),
                NodeType.ITEM,
                displayName,
                0,
                0,
                Map.of(
                        SearchNodeKeys.MOD_ID, "quark",
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "mineral",
                        SearchNodeKeys.COLLAPSE_FAMILY, "quark:shards",
                        SearchNodeKeys.COLLAPSE_LABEL, "Glass Shards",
                        SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed",
                        SearchNodeKeys.COLOR_BUCKET, color
                )
        );
    }

    private static boolean containsLabel(List<TreeNode> nodes, String label) {
        for (TreeNode node : nodes) {
            if (label.equals(node.getLabel().getString()) || containsLabel(node.getChildren(), label)) {
                return true;
            }
        }
        return false;
    }
}
