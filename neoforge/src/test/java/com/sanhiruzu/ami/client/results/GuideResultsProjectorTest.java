package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.index.AmiGuideSearchIndex;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideResultsProjectorTest {
    @Test
    void guideRowsExposeSourceAndProvenance() {
        AmiGuideDocument document = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/botania/mana_spreader"),
                        "patchouli",
                        "botania",
                        "Mana Spreaders"
                )
                .bookId(new ResourceLocation("botania", "lexicon"))
                .chapter("Basics")
                .referencedItem(new ResourceLocation("botania", "mana_spreader"))
                .tag("mana")
                .build();
        AmiGuideSearchIndex index = new AmiGuideSearchIndex(List.of(document),
                AmiGuideSearchIndex.GuideIndexingMode.TITLES);

        List<GuideResultRow> rows = GuideResultsProjector.project("mana spreader", index);

        assertEquals(1, rows.size());
        GuideResultRow row = rows.getFirst();
        assertEquals("Mana Spreaders", row.title());
        assertEquals("Botania > Lexicon > Basics > Guide Page", row.sourceLine());
        assertEquals("Guide Page - mentions Mana Spreader", row.provenanceLine());
        assertEquals(1, row.referencedItemCount());
        assertTrue(row.evidence().stream().anyMatch(e -> e.sourceType() == MatchEvidence.SourceType.GUIDE_TITLE));
        assertTrue(row.evidence().stream().anyMatch(e -> e.sourceType() == MatchEvidence.SourceType.GUIDE_REFERENCE));
    }

    @Test
    void summaryMatchesCarryLazySnippetEvidence() {
        AmiGuideDocument document = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/botania/routing"),
                        "patchouli",
                        "botania",
                        "Routing"
                )
                .summaryText("Point a Mana Spreader at a Mana Pool using the Wand of the Forest.")
                .build();
        AmiGuideSearchIndex index = new AmiGuideSearchIndex(List.of(document),
                AmiGuideSearchIndex.GuideIndexingMode.SUMMARY);

        GuideResultRow row = GuideResultsProjector.project("wand", index).getFirst();

        MatchEvidence summaryEvidence = row.evidence().stream()
                .filter(e -> e.sourceType() == MatchEvidence.SourceType.GUIDE_SUMMARY)
                .findFirst()
                .orElseThrow();
        assertTrue(summaryEvidence.snippet().contains("Wand of the Forest"));
    }

    @Test
    void blankQueryProducesNoRows() {
        AmiGuideSearchIndex index = new AmiGuideSearchIndex(List.of(), AmiGuideSearchIndex.GuideIndexingMode.TITLES);

        assertTrue(GuideResultsProjector.project(" ", index).isEmpty());
        assertTrue(GuideResultsProjector.project("mana", null).isEmpty());
    }

    @Test
    void guidebookFilterQueryReturnsAllRows() {
        AmiGuideDocument guide = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/one"),
                        "patchouli",
                        "example",
                        "Guide One"
                )
                .build();
        AmiGuideDocument recipe = AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guide/two"),
                        "patchouli",
                        "example",
                        "Guide Two"
                )
                .build();
        AmiGuideSearchIndex index = new AmiGuideSearchIndex(List.of(guide, recipe),
                AmiGuideSearchIndex.GuideIndexingMode.TITLES);

        assertEquals(2, GuideResultsProjector.project("guidebooks", index).size());
        assertEquals("Guide One", GuideResultsProjector.project("guidebooks", index).get(0).title());
        assertEquals(2, GuideResultsProjector.project("?type:guidebook", index).size());
        assertEquals("Guide One", GuideResultsProjector.project("?type:guidebook", index).get(0).title());
    }
}
