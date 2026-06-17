package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.AmiGuideSearchIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AmiTooltipFactsTest {

    private static SearchNode node(Map<String, String> metadata) {
        return new SearchNode(
                new ResourceLocation("create:cardboard_sword"),
                NodeType.ITEM,
                "Cardboard Sword",
                0,
                0,
                metadata
        );
    }

    @AfterEach
    void resetConfig() {
        AmiConfig.devMode = false;
        AmiConfig.showTooltipTags = false;
    }

    @Test
    void durabilityFactCallsOutLowDurability() {
        assertEquals("13 uses (low)", DurabilityTooltipFact.formatDurability("13"));
        assertEquals("1,561 uses", DurabilityTooltipFact.formatDurability("1561"));
    }

    @Test
    void shiftDetailsDoesNotPromptForModIdOnly() {
        SearchNode node = node(Map.of(SearchNodeKeys.MOD_ID, "create"));

        assertTrue(new ShiftDetailsTooltipFact().build(node).isEmpty());
    }

    @Test
    void tagTooltipFactIsUserConfigurableAndSorted() {
        SearchNode node = node(Map.of(
                SearchNodeKeys.TAGS, "minecraft:tools,minecraft:swords,minecraft:tools",
                SearchNodeKeys.BLOCK_TAGS, "minecraft:mineable/axe,minecraft:logs"
        ));

        assertTrue(new TagTooltipFact().build(node).isEmpty());

        assertEquals(
                java.util.List.of("minecraft:swords", "minecraft:tools"),
                TagTooltipFact.parseTags(node.meta(SearchNodeKeys.TAGS, ""))
        );
        assertEquals(
                java.util.List.of("minecraft:logs", "minecraft:mineable/axe"),
                TagTooltipFact.parseTags(node.meta(SearchNodeKeys.BLOCK_TAGS, ""))
        );
    }

    @Test
    void requiredToolMetadataDoesNotRenderAsTooltipFact() throws Exception {
        String source = Files.readString(Path.of("..", "xplat", "src", "main", "java", "com", "sanhiruzu",
                "ami", "util", "tooltip", "AmiTooltipFacts.java"));

        assertFalse(source.contains("RequiredToolTooltipFact"));
    }

    @Test
    void guideBookTooltipShowsIndexedPageCountWhenBookMatchesGuideIndex() {
        AmiGuideSearchIndex index = new AmiGuideSearchIndex(java.util.List.of(
                AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/apotheosis/enchanting"),
                        "patchouli",
                        "apotheosis",
                        "Enchanting")
                        .bookId(new ResourceLocation("apotheosis", "apoth_chronicle"))
                        .build()
        ), AmiGuideSearchIndex.GuideIndexingMode.TITLES);
        SearchNode node = node(Map.of(
                SearchNodeKeys.GUIDE_BOOK_CANDIDATE, "true",
                SearchNodeKeys.GUIDE_BOOK_SYSTEM, "patchouli",
                SearchNodeKeys.GUIDE_BOOK_ID, "apotheosis:apoth_chronicle",
                SearchNodeKeys.FACETS, "book,guide_book",
                SearchNodeKeys.MOD_ID, "patchouli"
        ));

        assertEquals(1, new GuideBookTooltipFact(() -> index).indexedPageCount(node));
    }

    @Test
    void guideBookTooltipShowsNotIndexedForCandidateWithoutDocuments() {
        SearchNode node = node(Map.of(
                SearchNodeKeys.GUIDE_BOOK_CANDIDATE, "true",
                SearchNodeKeys.GUIDE_BOOK_ID, "spectrum:guidebook",
                SearchNodeKeys.FACETS, "book,guide_book",
                SearchNodeKeys.MOD_ID, "spectrum"
        ));

        assertEquals(0, new GuideBookTooltipFact(() -> new AmiGuideSearchIndex(java.util.List.of(), AmiGuideSearchIndex.GuideIndexingMode.TITLES))
                .indexedPageCount(node));
    }

    @Test
    void plainBooksDoNotShowGuideBookIndexTooltip() {
        SearchNode node = node(Map.of(SearchNodeKeys.FACETS, "book"));

        assertTrue(new GuideBookTooltipFact().build(node).isEmpty());
    }
}
