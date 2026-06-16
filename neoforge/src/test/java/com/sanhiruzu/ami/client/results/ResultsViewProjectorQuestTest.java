package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiQuestDocument;
import com.sanhiruzu.ami.api.AmiQuestTaskDocument;
import com.sanhiruzu.ami.index.AmiQuestSearchIndex;
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

class ResultsViewProjectorQuestTest {
    @BeforeEach
    void setUp() {
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
    }

    @Test
    void projectionIncludesQuestRowsWithoutChangingItemTree() {
        SearchNode item = new SearchNode(
                new Identifier("minecraft", "redstone"),
                NodeType.ITEM,
                "Redstone Dust",
                0,
                0,
                Map.of()
        );
        AmiQuestSearchIndex questIndex = new AmiQuestSearchIndex(List.of(quest()));
        SearchState state = new SearchState();
        state.setQuery("redstone quest");
        state.setGroupBy(ResultsProcessor.GroupBy.NONE);

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(
                List.of(item),
                state,
                null,
                null,
                questIndex,
                false,
                false
        );

        assertEquals(1, projection.roots().size());
        assertEquals("Redstone Dust", projection.roots().getFirst().getLabel().getString());
        assertEquals(1, projection.questRows().size());
        assertEquals("4x Redstone Dust", projection.questRows().getFirst().title());
        assertTrue(projection.summary().contains("quests=1"));
    }

    @Test
    void compactAndFavoritesProjectionsSuppressQuestRows() {
        AmiQuestSearchIndex questIndex = new AmiQuestSearchIndex(List.of(quest()));
        SearchState state = new SearchState();
        state.setQuery("redstone");

        assertTrue(ResultsViewProjector.project(List.of(), state, null, null, questIndex, true, false).questRows().isEmpty());
        assertTrue(ResultsViewProjector.project(List.of(), state, null, null, questIndex, false, true).questRows().isEmpty());
    }

    private static AmiQuestDocument quest() {
        return AmiQuestDocument.builder("ftbquests:runtime_test", "ftbquests", "4x Redstone Dust")
                .sourceId("ftbquests")
                .chapterTitle("AMI Runtime Test")
                .task(AmiQuestTaskDocument.builder("ftbquests:runtime_test/task", "ftbquests:runtime_test",
                                AmiQuestTaskDocument.Role.REQUIREMENT)
                        .taskType("ftb:item")
                        .itemId(new Identifier("minecraft", "redstone"))
                        .requiredCount(4)
                        .build())
                .build();
    }
}
