package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResultsProcessorTest {

    @BeforeEach
    void setUp() {
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
    }

    @Test
    void explicitFamilyGroupingCollapsesRegistryFamilies() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        SearchNode disc13 = item("music_disc_13", "Music Disc 13", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc",
                SearchNodeKeys.COLLAPSE_FAMILY, "music_discs",
                SearchNodeKeys.COLLAPSE_LABEL, "Music Discs"
        ));
        SearchNode discCat = item("music_disc_cat", "Music Disc Cat", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc",
                SearchNodeKeys.COLLAPSE_FAMILY, "music_discs",
                SearchNodeKeys.COLLAPSE_LABEL, "Music Discs"
        ));
        SearchNode discBlocks = item("music_disc_blocks", "Music Disc Blocks", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc",
                SearchNodeKeys.COLLAPSE_FAMILY, "music_discs",
                SearchNodeKeys.COLLAPSE_LABEL, "Music Discs"
        ));
        SearchNode discChirp = item("music_disc_chirp", "Music Disc Chirp", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc",
                SearchNodeKeys.COLLAPSE_FAMILY, "music_discs",
                SearchNodeKeys.COLLAPSE_LABEL, "Music Discs"
        ));
        SearchNode stone = item("stone", "Stone", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc"
        ));

        List<TreeNode> root = processor.process(List.of(disc13, stone, discCat, discBlocks, discChirp));

        assertEquals(1, root.size());
        TreeNode categoryGroup = root.get(0);
        assertEquals("utility", categoryGroup.getKey());
        assertEquals(1, categoryGroup.getChildren().size());

        TreeNode miscGroup = categoryGroup.getChildren().get(0);
        assertEquals(2, miscGroup.getChildren().size());

        TreeNode collapsed = miscGroup.getChildren().get(0);
        assertTrue(collapsed.isHighCardinality());
        assertEquals("Music Discs", collapsed.getLabel().getString());
        assertEquals(4, collapsed.getChildren().size());

        TreeNode leaf = miscGroup.getChildren().get(1);
        assertTrue(leaf.isLeaf());
        assertEquals("Stone", leaf.getLabel().getString());
    }

    @Test
    void subtypeFamiliesCanCollapseBelowSubtypeCardinalityThreshold() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        SearchNode admire = item("goat_horn/minecraft/admire", "Goat Horn Admire", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc",
                SearchNodeKeys.SUBTYPE_OF, "minecraft:goat_horn",
                SearchNodeKeys.COLLAPSE_FAMILY, "goat_horns",
                SearchNodeKeys.COLLAPSE_LABEL, "Goat Horns"
        ));
        SearchNode yearn = item("goat_horn/minecraft/yearn", "Goat Horn Yearn", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc",
                SearchNodeKeys.SUBTYPE_OF, "minecraft:goat_horn",
                SearchNodeKeys.COLLAPSE_FAMILY, "goat_horns",
                SearchNodeKeys.COLLAPSE_LABEL, "Goat Horns"
        ));
        SearchNode sing = item("goat_horn/minecraft/sing", "Goat Horn Sing", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc",
                SearchNodeKeys.SUBTYPE_OF, "minecraft:goat_horn",
                SearchNodeKeys.COLLAPSE_FAMILY, "goat_horns",
                SearchNodeKeys.COLLAPSE_LABEL, "Goat Horns"
        ));
        SearchNode seek = item("goat_horn/minecraft/seek", "Goat Horn Seek", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc",
                SearchNodeKeys.SUBTYPE_OF, "minecraft:goat_horn",
                SearchNodeKeys.COLLAPSE_FAMILY, "goat_horns",
                SearchNodeKeys.COLLAPSE_LABEL, "Goat Horns"
        ));

        List<TreeNode> root = processor.process(List.of(admire, yearn, sing, seek));

        assertEquals(1, root.size());
        TreeNode categoryGroup = root.get(0);
        assertEquals(1, categoryGroup.getChildren().size());

        TreeNode miscGroup = categoryGroup.getChildren().get(0);
        assertEquals(1, miscGroup.getChildren().size());

        TreeNode collapsed = miscGroup.getChildren().get(0);
        assertTrue(collapsed.isHighCardinality());
        assertEquals("Goat Horns", collapsed.getLabel().getString());
        assertEquals(4, collapsed.getChildren().size());
    }

    @Test
    void explicitFamilyGroupingLeavesSmallFamiliesExpanded() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        SearchNode redBanner = item("red_banner", "Red Banner", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc",
                SearchNodeKeys.COLLAPSE_FAMILY, "banners",
                SearchNodeKeys.COLLAPSE_LABEL, "Banners"
        ));
        SearchNode blueBanner = item("blue_banner", "Blue Banner", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc",
                SearchNodeKeys.COLLAPSE_FAMILY, "banners",
                SearchNodeKeys.COLLAPSE_LABEL, "Banners"
        ));
        SearchNode greenBanner = item("green_banner", "Green Banner", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc",
                SearchNodeKeys.COLLAPSE_FAMILY, "banners",
                SearchNodeKeys.COLLAPSE_LABEL, "Banners"
        ));

        List<TreeNode> root = processor.process(List.of(redBanner, blueBanner, greenBanner));

        assertEquals(1, root.size());
        TreeNode categoryGroup = root.get(0);
        assertEquals(1, categoryGroup.getChildren().size());
        TreeNode miscGroup = categoryGroup.getChildren().get(0);
        assertEquals(3, miscGroup.getChildren().size());
        assertTrue(miscGroup.getChildren().stream().allMatch(TreeNode::isLeaf));
    }

    @Test
    void processFlatReturnsUngroupedSortedLeaves() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        SearchNode disc13 = item("music_disc_13", "Music Disc 13", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                SearchNodeKeys.COLLAPSE_FAMILY, "music_discs",
                SearchNodeKeys.COLLAPSE_LABEL, "Music Discs"
        ));
        SearchNode stone = item("stone", "Stone", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry"
        ));
        SearchNode zincPlate = item("zinc_plate", "Zinc Plate", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients"
        ));

        List<TreeNode> flat = processor.processFlat(List.of(stone, disc13, zincPlate));

        assertEquals(3, flat.size());
        assertTrue(flat.stream().allMatch(TreeNode::isLeaf));
        assertEquals("Music Disc 13", flat.get(0).getLabel().getString());
        assertEquals("Stone", flat.get(1).getLabel().getString());
        assertEquals("Zinc Plate", flat.get(2).getLabel().getString());
    }

    private static SearchNode item(String path, String displayName, Map<String, String> metadata) {
        return new SearchNode(
                ResourceLocation.parse("minecraft:" + path),
                NodeType.ITEM,
                displayName,
                0,
                0,
                metadata
        );
    }
}
