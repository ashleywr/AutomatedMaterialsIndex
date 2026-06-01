package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.api.AmiQuestEntry;
import com.sanhiruzu.ami.api.AmiQuestDocument;
import com.sanhiruzu.ami.api.AmiQuestGroup;
import com.sanhiruzu.ami.api.AmiQuestTaskDocument;
import com.sanhiruzu.ami.client.results.TreeNode;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestSidebarProjectorTest {
    @Test
    void projectsQuestGroupsIntoExpandedTreeNodes() {
        ResourceLocation iron = new ResourceLocation("minecraft", "iron_ingot");
        SearchNode ironNode = item(iron, "Iron Ingot");
        AmiQuestGroup group = new AmiQuestGroup(
                "ftbquests:chapter/start",
                Component.literal("Chapter Start"),
                List.of(
                        new AmiQuestEntry(iron, 2),
                        new AmiQuestEntry(iron, 3),
                        new AmiQuestEntry(new ResourceLocation("example", "missing_machine"), 1)
                ),
                0
        );

        List<TreeNode> roots = QuestSidebarProjector.project(List.of(group), id ->
                id.equals(iron) ? Optional.of(ironNode) : Optional.empty());

        assertEquals(1, roots.size());
        TreeNode root = roots.getFirst();
        assertEquals("ftbquests:chapter/start", root.getKey());
        assertEquals("Chapter Start", root.getLabel().getString());
        assertTrue(root.isExpanded());
        assertEquals(2, root.getChildren().size());

        TreeNode ironLeaf = root.getChildren().getFirst();
        assertTrue(ironLeaf.isLeaf());
        assertEquals(ironNode, ironLeaf.getEntry());
        assertEquals("ami.tooltip.quest_item_count", ironLeaf.getLabel().getString());

        TreeNode fallbackLeaf = root.getChildren().get(1);
        assertEquals(new ResourceLocation("example", "missing_machine"), fallbackLeaf.getEntry().id());
        assertEquals("example:missing_machine", fallbackLeaf.getLabel().getString());
        assertEquals("example", fallbackLeaf.getEntry().meta(SearchNodeKeys.MOD_ID));
        assertEquals("true", fallbackLeaf.getEntry().meta(QuestSidebarProjector.QUEST_FALLBACK));
    }

    @Test
    void skipsEmptyGroups() {
        AmiQuestGroup empty = new AmiQuestGroup("ftbquests:empty", Component.literal("Empty"), List.of());

        assertTrue(QuestSidebarProjector.project(List.of(empty), ignored -> Optional.empty()).isEmpty());
        assertTrue(QuestSidebarProjector.project(null, ignored -> Optional.empty()).isEmpty());
    }

    @Test
    void fallbackSingleCountUsesItemIdLabel() {
        ResourceLocation itemId = new ResourceLocation("minecraft", "lodestone");
        AmiQuestGroup group = new AmiQuestGroup("ftbquests:test", Component.literal("Test"), List.of(
                new AmiQuestEntry(itemId, 1)
        ));

        TreeNode leaf = QuestSidebarProjector.project(List.of(group), ignored -> Optional.empty())
                .getFirst()
                .getChildren()
                .getFirst();

        assertFalse(leaf.getEntry().metadata().isEmpty());
        assertEquals("minecraft:lodestone", leaf.getLabel().getString());
    }

    @Test
    void projectsRichQuestDocumentsIntoChapterQuestRequirementTree() {
        ResourceLocation redstone = new ResourceLocation("minecraft", "redstone");
        SearchNode redstoneNode = item(redstone, "Redstone Dust");
        AmiQuestDocument document = AmiQuestDocument.builder("ftbquests:quest/redstone", "ftbquests", "Make Power")
                .sourceId("ftbquests")
                .chapterId("ftbquests:chapter/start")
                .chapterTitle("Getting Started")
                .task(AmiQuestTaskDocument.builder("ftbquests:quest/redstone/task", "ftbquests:quest/redstone",
                                AmiQuestTaskDocument.Role.REQUIREMENT)
                        .itemId(redstone)
                        .requiredCount(4)
                        .build())
                .task(AmiQuestTaskDocument.builder("ftbquests:quest/redstone/reward", "ftbquests:quest/redstone",
                                AmiQuestTaskDocument.Role.REWARD)
                        .itemId(new ResourceLocation("minecraft", "diamond"))
                        .requiredCount(1)
                        .build())
                .build();

        List<TreeNode> roots = QuestSidebarProjector.project(List.of(), List.of(document), id ->
                id.equals(redstone) ? Optional.of(redstoneNode) : Optional.empty());

        assertEquals(1, roots.size());
        TreeNode chapter = roots.getFirst();
        assertEquals("Getting Started", chapter.getLabel().getString());
        assertTrue(chapter.isExpanded());
        TreeNode quest = chapter.getChildren().getFirst();
        assertEquals("Make Power", quest.getLabel().getString());
        assertTrue(quest.isExpanded());
        assertEquals(1, quest.getChildren().size());
        TreeNode redstoneLeaf = quest.getChildren().getFirst();
        assertEquals(redstoneNode, redstoneLeaf.getEntry());
        assertEquals("ami.tooltip.quest_item_count", redstoneLeaf.getLabel().getString());
    }

    private static SearchNode item(ResourceLocation id, String displayName) {
        return new SearchNode(id, NodeType.ITEM, displayName, 0, 0, Map.of(SearchNodeKeys.MOD_ID, id.getNamespace()));
    }
}
