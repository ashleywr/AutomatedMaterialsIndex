package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiQuestDocument;
import com.sanhiruzu.ami.api.AmiQuestTaskDocument;
import com.sanhiruzu.ami.index.AmiQuestSearchIndex;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestResultsProjectorTest {
    @Test
    void questRowsExposeSourceStatusAndTaskEvidence() {
        AmiQuestDocument document = AmiQuestDocument.builder("ftbquests:runtime_test", "ftbquests", "4x Redstone Dust")
                .sourceId("ftbquests")
                .chapterTitle("AMI Runtime Test")
                .status(AmiQuestDocument.Status.AVAILABLE)
                .task(AmiQuestTaskDocument.builder("ftbquests:runtime_test/task", "ftbquests:runtime_test",
                                AmiQuestTaskDocument.Role.REQUIREMENT)
                        .taskType("ftb:item")
                        .title("4x Redstone Dust")
                        .itemId(new Identifier("minecraft", "redstone"))
                        .requiredCount(4)
                        .build())
                .task(AmiQuestTaskDocument.builder("ftbquests:runtime_test/reward", "ftbquests:runtime_test",
                                AmiQuestTaskDocument.Role.REWARD)
                        .taskType("ftb:item_reward")
                        .itemId(new Identifier("minecraft", "diamond"))
                        .requiredCount(1)
                        .build())
                .build();
        AmiQuestSearchIndex index = new AmiQuestSearchIndex(List.of(document));

        QuestResultRow row = QuestResultsProjector.project("redstone", index).getFirst();

        assertEquals("4x Redstone Dust", row.title());
        assertEquals("FTB Quests > AMI Runtime Test > Quest", row.sourceLine());
        assertEquals("Available - 1 requirement - 1 reward", row.provenanceLine());
        assertEquals(1, row.requirementCount());
        assertEquals(1, row.rewardCount());
        assertTrue(row.evidence().stream().anyMatch(e -> e.sourceType() == MatchEvidence.SourceType.QUEST_ITEM));
        assertTrue(row.evidence().stream().anyMatch(e -> e.snippet().contains("4x")));
    }

    @Test
    void blankQueryProducesNoRows() {
        AmiQuestSearchIndex index = new AmiQuestSearchIndex(List.of());

        assertTrue(QuestResultsProjector.project(" ", index).isEmpty());
        assertTrue(QuestResultsProjector.project("redstone", null).isEmpty());
    }
}
