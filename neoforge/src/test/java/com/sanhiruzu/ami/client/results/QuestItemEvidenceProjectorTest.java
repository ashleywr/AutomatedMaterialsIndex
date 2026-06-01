package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiQuestDocument;
import com.sanhiruzu.ami.api.AmiQuestItemMatch;
import com.sanhiruzu.ami.api.AmiQuestTaskDocument;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestItemEvidenceProjectorTest {
    @Test
    void evidenceSummarizesRequirementAndRewardMatches() {
        ResourceLocation redstone = new ResourceLocation("minecraft", "redstone");
        AmiQuestTaskDocument requirement = AmiQuestTaskDocument.builder("quest/task", "quest",
                        AmiQuestTaskDocument.Role.REQUIREMENT)
                .title("4x Redstone Dust")
                .itemId(redstone)
                .requiredCount(4)
                .build();
        AmiQuestTaskDocument reward = AmiQuestTaskDocument.builder("quest/reward", "quest",
                        AmiQuestTaskDocument.Role.REWARD)
                .itemId(redstone)
                .requiredCount(1)
                .build();
        AmiQuestDocument quest = AmiQuestDocument.builder("ftbquests:quest", "ftbquests", "A Redstone Quest")
                .sourceId("ftbquests")
                .chapterTitle("Power")
                .task(requirement)
                .task(reward)
                .build();

        QuestItemEvidence evidence = QuestItemEvidenceProjector.project(List.of(
                new AmiQuestItemMatch(quest, requirement, redstone),
                new AmiQuestItemMatch(quest, reward, redstone)
        ));

        assertEquals(2, evidence.totalCount());
        assertEquals(1, evidence.requirementCount());
        assertEquals(1, evidence.rewardCount());
        assertEquals("Q2", evidence.badgeLabel());
        assertTrue(evidence.tooltipLines().stream().anyMatch(line -> line.equals("Quests: 1 requirement, 1 reward")));
        assertTrue(evidence.tooltipLines().stream().anyMatch(line -> line.contains("Requirement: Power > A Redstone Quest")));
        assertTrue(evidence.tooltipLines().stream().anyMatch(line -> line.contains("Reward: Power > A Redstone Quest")));
    }

    @Test
    void emptyOrInvalidMatchesProduceNoEvidence() {
        assertEquals(0, QuestItemEvidenceProjector.project(List.of()).totalCount());
        assertEquals(0, QuestItemEvidenceProjector.project(List.of(new AmiQuestItemMatch(null, null, null))).totalCount());
    }
}
