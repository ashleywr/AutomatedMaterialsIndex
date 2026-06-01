package com.sanhiruzu.ami.author;

import com.sanhiruzu.ami.api.AmiQuestDocument;
import com.sanhiruzu.ami.api.AmiQuestTaskDocument;
import com.sanhiruzu.ami.api.AmiQuestsApi;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PackAuthorDiagnosticsTest {
    @BeforeEach
    void resetQuests() {
        AmiQuestsApi.clearQuestGroups();
    }

    @Test
    void itemReportSurfacesCoverageAndRecipeWarnings() {
        SearchNode apple = item("apple", "Apple", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "food",
                SearchNodeKeys.ACCESS_LEVEL, "survival",
                SearchNodeKeys.OBTAINABILITY, "no_recipe",
                SearchNodeKeys.RECIPE_OUTPUT_COUNT, "0",
                SearchNodeKeys.RECIPE_USE_COUNT, "2"
        ));

        String report = PackAuthorDiagnostics.itemReport(apple, List.of());

        assertTrue(report.contains("ID: minecraft:apple"));
        assertTrue(report.contains("No quest references found."));
        assertTrue(report.contains("No indexed recipe output evidence"));
    }

    @Test
    void groupReportSummarizesCoverageAndQuestDiagnostics() {
        SearchNode apple = item("apple", "Apple", Map.of(
                SearchNodeKeys.ACCESS_LEVEL, "survival",
                SearchNodeKeys.OBTAINABILITY, "no_recipe",
                SearchNodeKeys.RECIPE_OUTPUT_COUNT, "0"
        ));
        SearchNode diamond = item("diamond", "Diamond", Map.of(
                SearchNodeKeys.ACCESS_LEVEL, "survival",
                SearchNodeKeys.RECIPE_OUTPUT_COUNT, "1"
        ));
        SearchNode commandBlock = item("command_block", "Command Block", Map.of(
                SearchNodeKeys.ACCESS_LEVEL, "cheat",
                SearchNodeKeys.RECIPE_OUTPUT_COUNT, "0"
        ));
        AmiQuestDocument quest = AmiQuestDocument.builder("ftbquests:food/apple", "ftbquests", "Find Food")
                .sourceId("ftbquests")
                .chapterTitle("Getting Started")
                .task(AmiQuestTaskDocument.builder("ftbquests:food/apple/task/apple", "ftbquests:food/apple",
                                AmiQuestTaskDocument.Role.REQUIREMENT)
                        .taskType("item")
                        .itemId(new ResourceLocation("minecraft", "apple"))
                        .requiredCount(4)
                        .build())
                .task(AmiQuestTaskDocument.builder("ftbquests:food/apple/task/missing", "ftbquests:food/apple",
                                AmiQuestTaskDocument.Role.REQUIREMENT)
                        .taskType("item")
                        .itemId(new ResourceLocation("example", "missing_item"))
                        .build())
                .task(AmiQuestTaskDocument.builder("ftbquests:food/apple/task/filter", "ftbquests:food/apple",
                                AmiQuestTaskDocument.Role.REQUIREMENT)
                        .taskType("item")
                        .highCardinality(true)
                        .tag("minecraft:logs")
                        .build())
                .build();
        AmiQuestsApi.registerQuestDocument(quest);

        String report = PackAuthorDiagnostics.groupReport(
                "Food",
                List.of(apple, diamond, commandBlock),
                AmiQuestsApi.getQuestDocuments()
        );

        assertTrue(report.contains("Items: 3"));
        assertTrue(report.contains("Unquested Items: 2"));
        assertTrue(report.contains("No Recipe Output Evidence: 2"));
        assertTrue(report.contains("Creative/Cheat/Dev Access: 1"));
        assertTrue(report.contains("example:missing_item"));
        assertTrue(report.contains("Getting Started > Find Food > item"));
        assertTrue(report.contains("minecraft:apple (Getting Started > Find Food)"));
    }

    private static SearchNode item(String path, String name, Map<String, String> metadata) {
        return new SearchNode(
                new ResourceLocation("minecraft:" + path),
                NodeType.ITEM,
                name,
                0,
                0,
                metadata
        );
    }
}
