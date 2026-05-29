package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.util.StorageDisplayFormatter;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


public class ResultsProcessorTest {

    @BeforeEach
    void setUp() {
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
    }

    @Test
    void categoryGroupingDoesNotCollapseExplicitFamilies() {
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
        assertEquals(5, miscGroup.getChildren().size());
        assertTrue(miscGroup.getChildren().stream().allMatch(TreeNode::isLeaf));
        assertEquals(List.of("Music Disc 13", "Music Disc Blocks", "Music Disc Cat", "Music Disc Chirp", "Stone"),
                miscGroup.getChildren().stream().map(node -> node.getLabel().getString()).toList());
    }

    @Test
    void categoryGroupingDoesNotCollapseDuplicateLeafLabels() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                item("creeper_banner_pattern", "Banner Pattern", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc"
                )),
                item("skull_banner_pattern", "Banner Pattern", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc"
                )),
                item("globe_banner_pattern", "Banner Pattern", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc"
                )),
                item("piglin_banner_pattern", "Banner Pattern", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc"
                ))
        ));

        TreeNode miscGroup = root.get(0).getChildren().get(0);
        assertEquals(4, miscGroup.getChildren().size());
        assertTrue(miscGroup.getChildren().stream().allMatch(TreeNode::isLeaf));
        assertTrue(miscGroup.getChildren().stream()
                .allMatch(node -> node.getLabel().getString().equals("Banner Pattern")));
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
        assertEquals(4, miscGroup.getChildren().size());
        assertTrue(miscGroup.getChildren().stream().allMatch(TreeNode::isLeaf));
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

    @Test
    void processFlatWithCardGroupingKeepsFlatResultsButCollapsesFamilies() {
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
        SearchNode discCat = item("music_disc_cat", "Music Disc Cat", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                SearchNodeKeys.COLLAPSE_FAMILY, "music_discs",
                SearchNodeKeys.COLLAPSE_LABEL, "Music Discs"
        ));
        SearchNode discBlocks = item("music_disc_blocks", "Music Disc Blocks", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                SearchNodeKeys.COLLAPSE_FAMILY, "music_discs",
                SearchNodeKeys.COLLAPSE_LABEL, "Music Discs"
        ));
        SearchNode discChirp = item("music_disc_chirp", "Music Disc Chirp", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                SearchNodeKeys.COLLAPSE_FAMILY, "music_discs",
                SearchNodeKeys.COLLAPSE_LABEL, "Music Discs"
        ));
        SearchNode stone = item("stone", "Stone", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry"
        ));

        List<TreeNode> flat = processor.processFlatWithCardGrouping(List.of(stone, disc13, discCat, discBlocks, discChirp));

        assertEquals(2, flat.size());
        assertFalse(flat.get(0).isHighCardinality());
        assertTrue(flat.get(0).isExpanded());
        assertEquals("Music Discs", flat.get(0).getLabel().getString());
        assertEquals(4, flat.get(0).getChildren().size());
        assertTrue(flat.get(1).isLeaf());
        assertEquals("Stone", flat.get(1).getLabel().getString());
    }

    @Test
    void categoryGroupingDoesNotEmitColorGroups() {
        AmiConfig.enableMaterialRootUI = true;
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(railwaysMagentaDoorVariants());

        assertFalse(hasKeyStartingWith(root, "color_group:"));
        TreeNode functionalGroup = root.get(0).getChildren().get(0);
        assertEquals("masonry/functional", functionalGroup.getKey());
        assertEquals(1, functionalGroup.getChildren().size());

        TreeNode doorsGroup = functionalGroup.getChildren().get(0);
        assertEquals("masonry/functional/doors", doorsGroup.getKey());
        assertEquals("Doors", doorsGroup.getLabel().getString());
        assertEquals(3, doorsGroup.getChildren().size());
        assertTrue(doorsGroup.getChildren().stream().allMatch(TreeNode::isLeaf));
    }

    @Test
    void categoryGroupingUsesCuratedKindsAcrossLargeModdedBuckets() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                item("oak_chair", "Oak Chair", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "furniture",
                        SearchNodeKeys.FACETS, "placeable,decorative_block"
                )),
                item("oak_table", "Oak Table", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "furniture",
                        SearchNodeKeys.FACETS, "placeable,decorative_block"
                )),
                item("crusher", "Crusher", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "tech",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "machines",
                        SearchNodeKeys.FACETS, "placeable,machine"
                )),
                item("copper_pipe", "Copper Pipe", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "tech",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "parts",
                        SearchNodeKeys.FACETS, "tech_component"
                )),
                item("tomato_seed", "Tomato Seed", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "seeds",
                        SearchNodeKeys.FACETS, "seed"
                ))
        ));

        assertTrue(hasKeyStartingWith(root, "decoration/furniture/chairs"));
        assertTrue(hasKeyStartingWith(root, "decoration/furniture/tables"));
        assertTrue(hasKeyStartingWith(root, "tech/machines/processors"));
        assertTrue(hasKeyStartingWith(root, "tech/parts/pipes"));
        assertTrue(hasKeyStartingWith(root, "nature/seeds/seeds"));
    }

    @Test
    void materialGroupingMayEmitColorGroups() {
        AmiConfig.enableMaterialRootUI = true;
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.MATERIAL,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(railwaysMagentaDoorVariants());

        assertTrue(hasKeyStartingWith(root, "color_group:railways:magenta_hinged_locometal_door"));
    }

    @Test
    void listProjectionFiltersWithSelectedLens() {
        SearchState state = new SearchState();
        state.setViewMode(ResultsToolbar.ViewMode.LIST);
        state.setListLens(ListLens.WEAPONS);

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(List.of(
                item("mace", "Mace", Map.of(SearchNodeKeys.DPS, "3.6")),
                item("stick", "Stick", Map.of())
        ), state, null, false, false);

        assertEquals(1, projection.displayedItemCount());
        assertEquals("Mace", projection.roots().get(0).getLabel().getString());
    }

    @Test
    void storageLensDoesNotRequireEveryDisplayedBadgeField() {
        SearchState state = new SearchState();
        state.setViewMode(ResultsToolbar.ViewMode.LIST);
        state.setListLens(ListLens.STORAGE);

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(List.of(
                item("battery_sword", "Battery Sword", Map.of(
                        SearchNodeKeys.ESM_CAPACITY, "10000",
                        SearchNodeKeys.DPS, "8.0"
                )),
                item("barrel", "Barrel", Map.of(SearchNodeKeys.FACETS, "storage")),
                item("sword", "Sword", Map.of(SearchNodeKeys.DPS, "8.0"))
        ), state, null, false, false);

        assertEquals(2, projection.displayedItemCount());
        assertEquals(List.of("Battery Sword", "Barrel"), projection.roots().stream()
                .map(node -> node.getLabel().getString())
                .toList());
    }

    @Test
    void damageFieldIncludesItemsAndEntitiesWithAttackDamage() {
        SearchNode mace = item("mace", "Mace", Map.of(SearchNodeKeys.ATTACK_DAMAGE, "6.0"));
        SearchNode zombie = new SearchNode(
                new ResourceLocation("minecraft:zombie"),
                NodeType.ENTITY,
                "Zombie",
                0,
                0,
                Map.of(SearchNodeKeys.ENTITY_ATTACK_DAMAGE, "3")
        );

        assertTrue(RowField.DAMAGE.hasValue(mace));
        assertTrue(RowField.DAMAGE.hasValue(zombie));
    }

    @Test
    void storageFormatterUsesVanillaChestEquivalents() {
        assertEquals("1x chest", StorageDisplayFormatter.formatChestEquivalent("1728"));
        assertEquals("2x chest", StorageDisplayFormatter.formatChestEquivalent("3456"));
        assertEquals("1.5x chest", StorageDisplayFormatter.formatChestEquivalent("2592"));
        assertEquals("1x chest (1,728 items)", StorageDisplayFormatter.formatChestEquivalent("1728", true));
    }

    @Test
    void damageSortUsesRawDamageInsteadOfDps() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.DAMAGE,
                false,
                ResultsProcessor.GroupBy.NONE,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.processFlat(List.of(
                item("mace", "Mace", Map.of(
                        SearchNodeKeys.ATTACK_DAMAGE, "6.0",
                        SearchNodeKeys.DPS, "12.0"
                )),
                item("netherite_axe", "Netherite Axe", Map.of(
                        SearchNodeKeys.ATTACK_DAMAGE, "10.0",
                        SearchNodeKeys.DPS, "10.0"
                )),
                item("stick", "Stick", Map.of())
        ));

        assertEquals("Netherite Axe", root.get(0).getLabel().getString());
        assertEquals("Mace", root.get(1).getLabel().getString());
        assertEquals("Stick", root.get(2).getLabel().getString());
    }

    @Test
    void dpsSortStillUsesSpeedAdjustedDps() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.DPS,
                false,
                ResultsProcessor.GroupBy.NONE,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.processFlat(List.of(
                item("mace", "Mace", Map.of(
                        SearchNodeKeys.ATTACK_DAMAGE, "6.0",
                        SearchNodeKeys.DPS, "12.0"
                )),
                item("netherite_axe", "Netherite Axe", Map.of(
                        SearchNodeKeys.ATTACK_DAMAGE, "10.0",
                        SearchNodeKeys.DPS, "10.0"
                ))
        ));

        assertEquals("Mace", root.get(0).getLabel().getString());
        assertEquals("Netherite Axe", root.get(1).getLabel().getString());
    }

    @Test
    void categoryGroupHeadersUseNameOrderForDefaultSort() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        SearchNode compass = item("compass", "Compass", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "navigation"
        ));
        SearchNode helmet = item("iron_helmet", "Iron Helmet", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "armor",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "head"
        ));

        List<TreeNode> root = processor.process(List.of(compass, helmet));

        assertEquals(2, root.size());
        assertEquals("armor", root.get(0).getKey());
        assertEquals("utility", root.get(1).getKey());
    }

    @Test
    void materialRootCollapseFlattensOnlyDyeSubgroupUnderDyes() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                dye("white_dye", "White Dye", "white"),
                dye("black_dye", "Black Dye", "black"),
                dye("red_dye", "Red Dye", "red"),
                dye("blue_dye", "Blue Dye", "blue")
        ));

        assertEquals(1, root.size());
        TreeNode categoryGroup = root.get(0);
        assertEquals("ingredients", categoryGroup.getKey());
        assertEquals(1, categoryGroup.getChildren().size());

        TreeNode dyesGroup = categoryGroup.getChildren().get(0);
        assertEquals(1, dyesGroup.getChildren().size());
        TreeNode dyeKindGroup = dyesGroup.getChildren().get(0);
        assertEquals("ingredients/dyes/dyes", dyeKindGroup.getKey());
        assertEquals(4, dyeKindGroup.getChildren().size());
        assertTrue(dyeKindGroup.getChildren().stream().allMatch(TreeNode::isLeaf));
    }

    @Test
    void materialRootCollapseKeepsNonDyeSubgroupExpandedByDefault() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                item("red_mushroom", "Red Mushroom", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "flora",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:mushroom"
                )),
                item("brown_mushroom", "Brown Mushroom", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "flora",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:mushroom"
                )),
                item("crimson_fungus", "Crimson Fungus", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "flora",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:mushroom"
                )),
                item("warped_fungus", "Warped Fungus", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "flora",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:mushroom"
                ))
        ));

        TreeNode floraGroup = root.get(0).getChildren().get(0);
        assertEquals(1, floraGroup.getChildren().size());

        TreeNode mushroomsGroup = floraGroup.getChildren().get(0);
        assertTrue(mushroomsGroup.isHighCardinality());
        assertTrue(mushroomsGroup.isExpanded());
        assertEquals(4, mushroomsGroup.getChildren().size());
    }

    @Test
    void baseItemJoinsItsMaterialVariantGroup() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                item("candle", "Candle", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting"
                )),
                item("white_candle", "White Candle", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:candle"
                )),
                item("black_candle", "Black Candle", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:candle"
                )),
                item("red_candle", "Red Candle", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:candle"
                ))
        ));

        TreeNode lightingGroup = root.get(0).getChildren().get(0);
        assertEquals(1, lightingGroup.getChildren().size());

        TreeNode candlesKindGroup = lightingGroup.getChildren().get(0);
        assertEquals("decoration/lighting/candles", candlesKindGroup.getKey());
        assertEquals(1, candlesKindGroup.getChildren().size());

        TreeNode candleGroup = candlesKindGroup.getChildren().get(0);
        assertTrue(candleGroup.isHighCardinality());
        assertEquals(4, candleGroup.getChildren().size());
        assertEquals("Candle", candleGroup.getChildren().get(0).getLabel().getString());
    }

    @Test
    void noneGroupingReturnsLiteralSortedLeaves() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.NONE,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                item("candle", "Candle", Map.of()),
                item("white_candle", "White Candle", Map.of(SearchNodeKeys.SUBTYPE_OF, "minecraft:candle")),
                item("black_candle", "Black Candle", Map.of(SearchNodeKeys.SUBTYPE_OF, "minecraft:candle")),
                item("red_candle", "Red Candle", Map.of(SearchNodeKeys.SUBTYPE_OF, "minecraft:candle")),
                item("brown_mushroom", "Brown Mushroom", Map.of(SearchNodeKeys.SUBTYPE_OF, "minecraft:mushroom")),
                item("red_mushroom", "Red Mushroom", Map.of(SearchNodeKeys.SUBTYPE_OF, "minecraft:mushroom"))
        ));

        assertEquals("""
                Black Candle
                Brown Mushroom
                Candle
                Red Candle
                Red Mushroom
                White Candle
                """, ResultsTreeDump.dump(root));
    }

    @Test
    void explicitFamilyCollapseFlattensDuplicateParentLabel() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                banner("white_banner", "White Banner"),
                banner("black_banner", "Black Banner"),
                banner("red_banner", "Red Banner"),
                banner("blue_banner", "Blue Banner")
        ));

        TreeNode decorationGroup = root.get(0);
        assertEquals("decoration", decorationGroup.getKey());
        assertEquals(1, decorationGroup.getChildren().size());

        TreeNode bannersGroup = decorationGroup.getChildren().get(0);
        assertEquals("Banners", bannersGroup.getLabel().getString());
        assertEquals(4, bannersGroup.getChildren().size());
        assertTrue(bannersGroup.getChildren().stream().allMatch(TreeNode::isLeaf));
    }

    @Test
    void explicitFamilyCollapseFlattensDuplicateParentLabelWithSiblingItems() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                banner("white_banner", "White Banner"),
                banner("black_banner", "Black Banner"),
                banner("red_banner", "Red Banner"),
                banner("blue_banner", "Blue Banner"),
                item("flower_banner_pattern", "Flower Banner Pattern", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "banners"
                ))
        ));

        TreeNode bannersGroup = root.get(0).getChildren().get(0);
        assertEquals("""
                Banners [expanded]
                  Black Banner
                  Blue Banner
                  Flower Banner Pattern
                  Red Banner
                  White Banner
                """, ResultsTreeDump.dump(List.of(bannersGroup)));
    }

    @Test
    void defaultCategoryGridTreeShapeMatchesExpectedPresentation() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                banner("white_banner", "White Banner"),
                banner("black_banner", "Black Banner"),
                banner("red_banner", "Red Banner"),
                banner("blue_banner", "Blue Banner"),
                item("candle", "Candle", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting"
                )),
                item("white_candle", "White Candle", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:candle"
                )),
                item("black_candle", "Black Candle", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:candle"
                )),
                item("red_candle", "Red Candle", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:candle"
                )),
                dye("white_dye", "White Dye", "white"),
                dye("black_dye", "Black Dye", "black"),
                dye("red_dye", "Red Dye", "red"),
                dye("blue_dye", "Blue Dye", "blue"),
                item("red_mushroom", "Red Mushroom", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "fungi",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:mushroom"
                )),
                item("brown_mushroom", "Brown Mushroom", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "fungi",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:mushroom"
                )),
                item("crimson_fungus", "Crimson Fungus", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "fungi",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:mushroom"
                )),
                item("warped_fungus", "Warped Fungus", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "fungi",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:mushroom"
                )),
                item("crimson_nylium", "Crimson Nylium", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "fungi"
                )),
                item("warped_nylium", "Warped Nylium", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "fungi"
                ))
        ));

        assertAllGroupsExpanded(root);
        assertNoRedundantOnlyChildGroups(root);
        assertEquals("""
                Decoration [expanded]
                  Banners [expanded]
                    Black Banner
                    Blue Banner
                    Red Banner
                    White Banner
                  Lighting [expanded]
                    Candles [expanded]
                      Candle [expanded] [cardinality]
                        Candle
                        Black Candle
                        Red Candle
                        White Candle
                Ingredients [expanded]
                  Dyes & Pigments [expanded]
                    Dyes [expanded]
                      Black Dye
                      Blue Dye
                      Red Dye
                      White Dye
                Nature [expanded]
                  Fungi & Forage [expanded]
                    Brown Mushroom
                    Crimson Fungus
                    Red Mushroom
                    Warped Fungus
                    Crimson Nylium
                    Warped Nylium
                """, ResultsTreeDump.dump(root));
    }

    private static void assertAllGroupsExpanded(List<TreeNode> nodes) {
        for (TreeNode node : nodes) {
            if (!node.isLeaf()) {
                assertTrue(node.isExpanded(), "Expected group to start expanded: " + node.getLabel().getString());
                assertAllGroupsExpanded(node.getChildren());
            }
        }
    }

    private static void assertNoRedundantOnlyChildGroups(List<TreeNode> nodes) {
        for (TreeNode node : nodes) {
            if (node.isLeaf()) {
                continue;
            }

            if (node.getChildren().size() == 1) {
                TreeNode onlyChild = node.getChildren().get(0);
                assertFalse(!onlyChild.isLeaf()
                                && node.getLabel().getString().equalsIgnoreCase(onlyChild.getLabel().getString()),
                        "Redundant nested group:\n" + ResultsTreeDump.dump(List.of(node)));
            }
            assertNoRedundantOnlyChildGroups(node.getChildren());
        }
    }

    private static SearchNode item(String path, String displayName, Map<String, String> metadata) {
        return new SearchNode(
                new ResourceLocation("minecraft:" + path),
                NodeType.ITEM,
                displayName,
                0,
                0,
                metadata
        );
    }

    private static List<SearchNode> railwaysMagentaDoorVariants() {
        Map<String, String> baseMetadata = Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "functional",
                SearchNodeKeys.MATERIAL_GROUP, "railways:magenta_hinged_locometal_door",
                SearchNodeKeys.COLOR_BUCKET, "magenta",
                SearchNodeKeys.VARIANT_GROUP, "doors",
                "blockShape", "door"
        );
        return List.of(
                item("magenta_hinged_locometal_door", "Magenta Hinged Locometal Door", baseMetadata),
                item("magenta_sliding_locometal_door", "Magenta Sliding Locometal Door", baseMetadata),
                item("magenta_folding_locometal_door", "Magenta Folding Locometal Door", baseMetadata)
        );
    }

    private static boolean hasKeyStartingWith(List<TreeNode> nodes, String prefix) {
        for (TreeNode node : nodes) {
            String key = node.getKey();
            if ((key != null && key.startsWith(prefix)) || hasKeyStartingWith(node.getChildren(), prefix)) {
                return true;
            }
        }
        return false;
    }

    private static SearchNode dye(String path, String displayName, String color) {
        return item(path, displayName, Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "dyes",
                SearchNodeKeys.SUBTYPE_OF, "minecraft:dye",
                SearchNodeKeys.MATERIAL_GROUP, "minecraft:dye",
                SearchNodeKeys.COLOR_BUCKET, color
        ));
    }

    private static SearchNode banner(String path, String displayName) {
        String color = path.endsWith("_banner") ? path.substring(0, path.length() - "_banner".length()) : "";
        return item(path, displayName, Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "banners",
                SearchNodeKeys.COLLAPSE_FAMILY, "banners",
                SearchNodeKeys.COLLAPSE_LABEL, "Banners",
                SearchNodeKeys.MATERIAL_GROUP, "minecraft:banner",
                SearchNodeKeys.COLOR_BUCKET, color
        ));
    }
}
