package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.api.AmiQuestDocument;
import com.sanhiruzu.ami.api.AmiQuestTaskDocument;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmiQuestSearchIndexTest {
    @Test
    void searchesQuestTaskAndItemFields() {
        AmiQuestDocument power = quest(
                "ftbquests:power",
                "Basic Power",
                "Generators unlock early automation.",
                new ResourceLocation("minecraft", "redstone")
        );
        AmiQuestDocument storage = quest(
                "ftbquests:storage",
                "Storage",
                "Drawers and chests.",
                new ResourceLocation("minecraft", "chest")
        );
        AmiQuestSearchIndex index = new AmiQuestSearchIndex(List.of(storage, power));

        assertEquals(List.of(power), index.search("basic power"));
        assertEquals(List.of(power), index.search("minecraft redstone"));
        assertEquals(List.of(storage), index.search("drawers"));
        assertEquals(List.of(power), index.findItem(new ResourceLocation("minecraft", "redstone"))
                .stream()
                .map(match -> match.quest())
                .toList());
    }

    @Test
    void emptyQueriesAndUnknownItemsReturnNothing() {
        AmiQuestSearchIndex index = new AmiQuestSearchIndex(List.of(quest(
                "ftbquests:test",
                "Test",
                "",
                new ResourceLocation("minecraft", "apple")
        )));

        assertTrue(index.search("").isEmpty());
        assertTrue(index.findItem(new ResourceLocation("minecraft", "stone")).isEmpty());
    }

    private static AmiQuestDocument quest(String id, String title, String description, ResourceLocation itemId) {
        return AmiQuestDocument.builder(id, "ftbquests", title)
                .sourceId("ftbquests")
                .chapterId("getting_started")
                .chapterTitle("Getting Started")
                .description(description)
                .task(AmiQuestTaskDocument.builder(id + "/task", id, AmiQuestTaskDocument.Role.REQUIREMENT)
                        .taskType("item")
                        .title(title + " Task")
                        .itemId(itemId)
                        .requiredCount(3)
                        .tag("starter")
                        .build())
                .build();
    }
}
