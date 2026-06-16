package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.index.AmiGuideSearchIndex;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultsViewProjectorGuideTest {
    @BeforeEach
    void setUp() {
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
    }

    @Test
    void projectionIncludesGuideRowsWithoutChangingItemTree() {
        SearchNode item = new SearchNode(
                new Identifier("botania", "mana_spreader"),
                NodeType.ITEM,
                "Mana Spreader",
                0,
                0,
                Map.of()
        );
        AmiGuideDocument guide = AmiGuideDocument.builder(
                        new Identifier("ami", "guide/botania/mana_spreader"),
                        "patchouli",
                        "botania",
                        "Mana Spreaders"
                )
                .chapter("Basics")
                .referencedItem(new Identifier("botania", "mana_spreader"))
                .build();
        AmiGuideSearchIndex guideIndex = new AmiGuideSearchIndex(List.of(guide),
                AmiGuideSearchIndex.GuideIndexingMode.TITLES);
        SearchState state = new SearchState();
        state.setQuery("mana spreader");
        state.setGroupBy(ResultsProcessor.GroupBy.NONE);

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(
                List.of(item),
                state,
                null,
                guideIndex,
                false,
                false
        );

        assertEquals(1, projection.roots().size());
        assertEquals("Mana Spreader", projection.roots().getFirst().getLabel().getString());
        assertEquals(1, projection.guideRows().size());
        assertEquals("Mana Spreaders", projection.guideRows().getFirst().title());
        assertTrue(projection.summary().contains("guides=1"));
    }

    @Test
    void compactAndFavoritesProjectionsSuppressGuideRows() {
        AmiGuideDocument guide = AmiGuideDocument.builder(
                new Identifier("ami", "guide/example"),
                "plugin",
                "example",
                "Example Guide"
        ).build();
        AmiGuideSearchIndex guideIndex = new AmiGuideSearchIndex(List.of(guide),
                AmiGuideSearchIndex.GuideIndexingMode.TITLES);
        SearchState state = new SearchState();
        state.setQuery("example");

        assertTrue(ResultsViewProjector.project(List.of(), state, null, guideIndex, true, false).guideRows().isEmpty());
        assertTrue(ResultsViewProjector.project(List.of(), state, null, guideIndex, false, true).guideRows().isEmpty());
    }

    @Test
    void fullGridProjectionStillIncludesGuideRows() {
        AmiGuideDocument guide = AmiGuideDocument.builder(
                new Identifier("ami", "guide/example"),
                "plugin",
                "example",
                "Example Guide"
        ).build();
        AmiGuideSearchIndex guideIndex = new AmiGuideSearchIndex(List.of(guide),
                AmiGuideSearchIndex.GuideIndexingMode.TITLES);
        SearchState state = new SearchState();
        state.setQuery("example");
        state.setViewMode(ResultsToolbar.ViewMode.GRID);

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(
                List.of(),
                state,
                null,
                guideIndex,
                false,
                false
        );

        assertEquals(1, projection.guideRows().size());
    }
}
