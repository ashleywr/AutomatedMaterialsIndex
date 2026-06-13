package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiAdvancementDocument;
import com.sanhiruzu.ami.index.AmiAdvancementSearchIndex;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancementResultsProjectorTest {
    @Test
    void advancementRowsExposeSourceTypeAndDescriptionEvidence() {
        AmiAdvancementDocument document = AmiAdvancementDocument
                .builder(new ResourceLocation("minecraft", "story/mine_stone"), "Stone Age")
                .sourceId("minecraft")
                .tabTitle("Minecraft")
                .description("Mine stone with your new pickaxe")
                .type("task")
                .progressStatus(AmiAdvancementDocument.ProgressStatus.IN_PROGRESS)
                .iconItemId(new ResourceLocation("minecraft", "stone"))
                .build();
        AmiAdvancementSearchIndex index = new AmiAdvancementSearchIndex(List.of(document));

        AdvancementResultRow row = AdvancementResultsProjector.project("stone", index).getFirst();

        assertEquals("Stone Age", row.title());
        assertEquals("Minecraft > Minecraft > Advancement", row.sourceLine());
        assertEquals("Task - Mine stone with your new pickaxe", row.provenanceLine());
        assertTrue(row.evidence().stream().anyMatch(e -> e.sourceType() == MatchEvidence.SourceType.ADVANCEMENT_TITLE));
        assertTrue(row.evidence().stream().anyMatch(e -> e.sourceType() == MatchEvidence.SourceType.ADVANCEMENT_ICON));
        assertTrue(row.evidence().stream().anyMatch(e -> e.snippet().contains("stone")));
    }

    @Test
    void blankQueryProducesNoRows() {
        AmiAdvancementSearchIndex index = new AmiAdvancementSearchIndex(List.of());

        assertTrue(AdvancementResultsProjector.project(" ", index).isEmpty());
        assertTrue(AdvancementResultsProjector.project("stone", null).isEmpty());
    }

    @Test
    void unfinishedAdvancementsSortBeforeCompletedWhenScoresTie() {
        AmiAdvancementDocument completed = AmiAdvancementDocument
                .builder(new ResourceLocation("minecraft", "story/a_completed_stone"), "Stone")
                .progressStatus(AmiAdvancementDocument.ProgressStatus.COMPLETED)
                .build();
        AmiAdvancementDocument inProgress = AmiAdvancementDocument
                .builder(new ResourceLocation("minecraft", "story/z_in_progress_stone"), "Stone")
                .progressStatus(AmiAdvancementDocument.ProgressStatus.IN_PROGRESS)
                .build();
        AmiAdvancementSearchIndex index = new AmiAdvancementSearchIndex(List.of(completed, inProgress));

        List<AdvancementResultRow> rows = AdvancementResultsProjector.project("stone", index);

        assertEquals(AmiAdvancementDocument.ProgressStatus.IN_PROGRESS, rows.get(0).document().progressStatus());
        assertEquals(AmiAdvancementDocument.ProgressStatus.COMPLETED, rows.get(1).document().progressStatus());
    }
}
