package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmiGuideSearchIndexTest {
    @AfterEach
    void resetConfig() {
        AmiConfig.resetToDefaults();
    }

    @Test
    void titleChapterTagsModBookAndReferencesAreSearchable() {
        AmiGuideDocument manaSpreader = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/botania/mana_spreader"),
                        "patchouli",
                        "botania",
                        "Mana Spreaders"
                )
                .bookId(new ResourceLocation("botania", "lexicon"))
                .pageId("basics/mana_spreader")
                .chapter("Basics")
                .referencedItem(new ResourceLocation("botania", "mana_spreader"))
                .tag("mana")
                .build();
        AmiGuideSearchIndex index = new AmiGuideSearchIndex(List.of(manaSpreader),
                AmiGuideSearchIndex.GuideIndexingMode.TITLES);

        assertEquals(List.of(manaSpreader), index.search("mana spreader"));
        assertEquals(List.of(manaSpreader), index.search("Basics"));
        assertEquals(List.of(manaSpreader), index.search("botania lexicon"));
        assertEquals(List.of(manaSpreader), index.search("botania:mana_spreader"));
        assertEquals(List.of(manaSpreader), index.search("patchouli"));
    }

    @Test
    void guidebookFilterQueryReturnsAllIndexedDocuments() {
        AmiGuideDocument documentOne = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/book_one"),
                        "patchouli",
                        "example",
                        "Guide One"
                )
                .build();
        AmiGuideDocument documentTwo = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/book_two"),
                        "patchouli",
                        "example",
                        "Guide Two"
                )
                .build();
        AmiGuideSearchIndex index = new AmiGuideSearchIndex(List.of(documentOne, documentTwo),
                AmiGuideSearchIndex.GuideIndexingMode.TITLES);

        assertEquals(List.of(documentOne, documentTwo), index.search("guidebooks"));
        assertEquals(List.of(documentOne, documentTwo), index.search("mana guidebooks"));
        assertEquals(List.of(documentOne, documentTwo), index.search("?guidebook"));
        assertEquals(List.of(documentOne, documentTwo), index.search("?type:guidebook"));
    }

    @Test
    void exposesIndexedPageCountsByBookId() {
        ResourceLocation bookId = new ResourceLocation("apotheosis", "apoth_chronicle");
        AmiGuideDocument documentOne = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/one"),
                        "patchouli",
                        "apotheosis",
                        "One")
                .bookId(bookId)
                .build();
        AmiGuideDocument documentTwo = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/two"),
                        "patchouli",
                        "apotheosis",
                        "Two")
                .bookId(bookId)
                .build();
        AmiGuideDocument documentWithoutBook = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/derived"),
                        "silentgear_traits",
                        "silentgear",
                        "Derived")
                .build();

        AmiGuideSearchIndex index = new AmiGuideSearchIndex(List.of(documentOne, documentTwo, documentWithoutBook),
                AmiGuideSearchIndex.GuideIndexingMode.TITLES);

        assertEquals(2, index.indexedPageCountForBook(bookId));
        assertEquals(0, index.indexedPageCountForBook(new ResourceLocation("silentgear", "guide_book")));
    }

    @Test
    void titleMatchesRankAboveSummaryOnlyMatches() {
        AmiGuideDocument titleMatch = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/title"),
                        "patchouli",
                        "example",
                        "Mana Automation"
                )
                .build();
        AmiGuideDocument summaryMatch = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/summary"),
                        "patchouli",
                        "example",
                        "Flower Fuel"
                )
                .summaryText("Mana automation is possible with spreaders.")
                .build();
        AmiGuideSearchIndex index = new AmiGuideSearchIndex(List.of(summaryMatch, titleMatch),
                AmiGuideSearchIndex.GuideIndexingMode.SUMMARY);

        assertEquals(List.of(titleMatch, summaryMatch), index.search("mana automation"));
    }

    @Test
    void titlesModeDoesNotSearchSummaryText() {
        AmiGuideDocument document = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/hidden_summary"),
                        "patchouli",
                        "example",
                        "Visible Title"
                )
                .summaryText("secret body token")
                .build();
        AmiGuideSearchIndex index = new AmiGuideSearchIndex(List.of(document),
                AmiGuideSearchIndex.GuideIndexingMode.TITLES);

        assertTrue(index.search("secret").isEmpty());
    }

    @Test
    void summaryTextIsCappedBeforeSearch() {
        AmiGuideDocument document = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/capped"),
                        "patchouli",
                        "example",
                        "Capped"
                )
                .summaryText("alpha beta gamma")
                .build();
        AmiGuideSearchIndex index = new AmiGuideSearchIndex(List.of(document),
                AmiGuideSearchIndex.GuideIndexingMode.SUMMARY,
                5);

        assertEquals(List.of(document), index.search("alpha"));
        assertTrue(index.search("gamma").isEmpty());
    }

    @Test
    void offModeReturnsNoGuideResults() {
        AmiGuideDocument document = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/off"),
                        "patchouli",
                        "example",
                        "Visible Title"
                )
                .build();
        AmiGuideSearchIndex index = new AmiGuideSearchIndex(List.of(document),
                AmiGuideSearchIndex.GuideIndexingMode.OFF);

        assertTrue(index.allDocuments().isEmpty());
        assertTrue(index.search("visible").isEmpty());
    }

    @Test
    void fromConfigUsesConfiguredModeAndTextCap() {
        AmiGuideDocument document = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/config"),
                        "patchouli",
                        "example",
                        "Configurable"
                )
                .summaryText("alpha beta gamma")
                .build();

        AmiConfig.guideIndexingMode = AmiConfig.GuideIndexingMode.OFF;
        assertTrue(AmiGuideSearchIndex.fromConfig(List.of(document)).search("configurable").isEmpty());

        AmiConfig.guideIndexingMode = AmiConfig.GuideIndexingMode.TITLES;
        assertEquals(List.of(document), AmiGuideSearchIndex.fromConfig(List.of(document)).search("configurable"));
        assertTrue(AmiGuideSearchIndex.fromConfig(List.of(document)).search("gamma").isEmpty());

        AmiConfig.guideIndexingMode = AmiConfig.GuideIndexingMode.SUMMARY;
        AmiConfig.guideSummaryTextCap = 5;
        assertEquals(List.of(document), AmiGuideSearchIndex.fromConfig(List.of(document)).search("alpha"));
        assertTrue(AmiGuideSearchIndex.fromConfig(List.of(document)).search("gamma").isEmpty());
    }
}
