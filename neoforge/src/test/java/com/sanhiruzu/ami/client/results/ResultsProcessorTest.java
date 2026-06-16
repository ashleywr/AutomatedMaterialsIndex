package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.util.StorageDisplayFormatter;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


public class ResultsProcessorTest {

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

    private static List<String> leafLabels(List<TreeNode> nodes) {
        List<String> labels = new ArrayList<>();
        collectLeafLabels(nodes, labels);
        return labels;
    }

    private static void collectLeafLabels(List<TreeNode> nodes, List<String> labels) {
        for (TreeNode node : nodes) {
            if (node.isLeaf()) {
                labels.add(node.getLabel().getString());
            } else {
                collectLeafLabels(node.getChildren(), labels);
            }
        }
    }

    private static SearchNode item(String path, String displayName, Map<String, String> metadata) {
        return new SearchNode(
                new Identifier("minecraft:" + path),
                NodeType.ITEM,
                displayName,
                0,
                0,
                metadata
        );
    }

    private static SearchNode patchouliGuide(String namespace, String bookPath, String displayName) {
        String slug = displayName.toLowerCase(java.util.Locale.ROOT)
                .replace("é", "e")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return new SearchNode(
                new Identifier("patchouli", "guide_book/variant/" + slug),
                NodeType.ITEM,
                displayName,
                0,
                0,
                Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "books",
                        SearchNodeKeys.FACETS, "book,guide_book",
                        SearchNodeKeys.GUIDE_BOOK_CANDIDATE, "true",
                        SearchNodeKeys.GUIDE_BOOK_SYSTEM, "patchouli",
                        SearchNodeKeys.GUIDE_BOOK_ID, namespace + ":" + bookPath,
                        SearchNodeKeys.SUBTYPE_OF, "patchouli:guide_book",
                        SearchNodeKeys.MATERIAL_GROUP, "patchouli:guide_book",
                        SearchNodeKeys.VARIANT_COLLAPSE_MODE, "never"
                )
        );
    }

    private static SearchNode pokemon(String species, String displayName, int dexNumber) {
        return new SearchNode(
                new Identifier("cobblemon", "species/" + species),
                NodeType.ENTITY,
                displayName,
                0,
                0,
                Map.of(
                        SearchNodeKeys.ENTITY_CATEGORY, "pokemon_species",
                        SearchNodeKeys.POKEMON_SPECIES, "cobblemon:" + species,
                        SearchNodeKeys.POKEMON_DEX_NUMBER, Integer.toString(dexNumber)
                )
        );
    }

    private static SearchNode virtualEntity(String namespace, String path, String displayName) {
        return new SearchNode(
                new Identifier(namespace, path),
                NodeType.ENTITY,
                displayName,
                0,
                0,
                Map.of()
        );
    }

    private static SearchNode node(NodeType type, String namespace, String path, String displayName,
                                   Map<String, String> metadata) {
        return new SearchNode(
                new Identifier(namespace, path),
                type,
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
                item("magenta_folding_locometal_door", "Magenta Folding Locometal Door", baseMetadata),
                item("magenta_glass_locometal_door", "Magenta Glass Locometal Door", baseMetadata),
                item("magenta_panel_locometal_door", "Magenta Panel Locometal Door", baseMetadata),
                item("magenta_windowed_locometal_door", "Magenta Windowed Locometal Door", baseMetadata),
                item("magenta_barred_locometal_door", "Magenta Barred Locometal Door", baseMetadata),
                item("magenta_reinforced_locometal_door", "Magenta Reinforced Locometal Door", baseMetadata)
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

    private static SearchNode collapsedDye(String path, String displayName, String color) {
        return item(path, displayName, Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "dyes",
                SearchNodeKeys.SUBTYPE_OF, "minecraft:dye",
                SearchNodeKeys.MATERIAL_GROUP, "minecraft:dye",
                SearchNodeKeys.COLOR_BUCKET, color,
                SearchNodeKeys.COLLAPSE_FAMILY, "minecraft:dye",
                SearchNodeKeys.COLLAPSE_LABEL, "Dyes",
                SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed"
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

    private static SearchNode textileBanner(String path, String displayName) {
        String color = path.endsWith("_banner") ? path.substring(0, path.length() - "_banner".length()) : "";
        return item(path, displayName, Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "textiles",
                SearchNodeKeys.COLLAPSE_FAMILY, "banners",
                SearchNodeKeys.COLLAPSE_LABEL, "Banners",
                SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed",
                SearchNodeKeys.MATERIAL_GROUP, "minecraft:banner",
                SearchNodeKeys.COLOR_BUCKET, color
        ));
    }

    private static SearchNode potterySherd(String path, String displayName) {
        return item(path, displayName, Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "mineral",
                SearchNodeKeys.SUBTYPE_OF, "minecraft:pottery_sherd",
                SearchNodeKeys.MATERIAL_GROUP, "minecraft:pottery_sherd"
        ));
    }

    @BeforeEach
    void setUp() {
        AmiConfig.resetToDefaults();
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
    }

    @Test
    void defaultRegistrySortOrdersPokemonSpeciesByDexNumber() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.NONE,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                pokemon("abomasnow", "Abomasnow", 460),
                pokemon("bulbasaur", "Bulbasaur", 1),
                pokemon("charizard", "Charizard", 6),
                pokemon("mewtwo", "Mewtwo", 150)
        ));

        assertEquals(List.of("Bulbasaur", "Charizard", "Mewtwo", "Abomasnow"), leafLabels(root));
    }

    @Test
    void disabledAutoIndexingPlaceholderDoesNotClaimBackgroundIndexing() {
        AmiConfig.enableAutoIndexing = false;

        assertEquals("ami.gui.indexing_disabled", ResultsProcessor.indexingPlaceholderKey());
    }

    @Test
    void defaultRegistrySortKeepsMixedPokemonAndVirtualEntitiesComparatorTransitive() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.NONE,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                pokemon("charizard", "Charizard", 6),
                virtualEntity("example", "aardvark", "Aardvark"),
                pokemon("bulbasaur", "Bulbasaur", 1),
                item("stone", "Stone", Map.of())
        ));

        assertEquals(List.of("Bulbasaur", "Charizard", "Aardvark", "Stone"), leafLabels(root));
    }

    @Test
    void categoryGroupingCollapsesCollectibleExplicitFamilies() {
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

        TreeNode discsGroup = miscGroup.getChildren().stream()
                .filter(node -> "cardinality:family:music_discs".equals(node.getKey()))
                .findFirst()
                .orElseThrow();
        assertEquals("Music Discs", discsGroup.getLabel().getString());
        assertTrue(discsGroup.isHighCardinality());
        assertEquals(List.of("Music Disc 13", "Music Disc Blocks", "Music Disc Cat", "Music Disc Chirp"),
                leafLabels(List.of(discsGroup)));
    }

    @Test
    void categoryGroupingUsesUtilityBooksHeaderForBooksAndGuides() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                item("book", "Book", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "books"
                )),
                item("guide_book", "Guide Book", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "utility",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "books"
                ))
        ));

        assertEquals(1, root.size());
        TreeNode categoryGroup = root.get(0);
        assertEquals("utility", categoryGroup.getKey());
        assertEquals(1, categoryGroup.getChildren().size());
        TreeNode booksGroup = categoryGroup.getChildren().get(0);
        assertEquals("utility/books", booksGroup.getKey());
        assertEquals(List.of("Book", "Guide Book"), leafLabels(List.of(booksGroup)));
    }

    @Test
    void categoryGroupingCollapsesBannerPatternCollectibles() {
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
        assertEquals(1, miscGroup.getChildren().size());
        TreeNode patternsGroup = miscGroup.getChildren().get(0);
        assertEquals("cardinality:family:banner_patterns", patternsGroup.getKey());
        assertEquals("Banner Patterns", patternsGroup.getLabel().getString());
        assertTrue(patternsGroup.isHighCardinality());
        assertEquals(List.of("Banner Pattern", "Banner Pattern", "Banner Pattern", "Banner Pattern"),
                leafLabels(List.of(patternsGroup)));
    }

    @Test
    void collectibleSubtypeFamiliesCollapseBelowSubtypeCardinalityThreshold() {
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
        TreeNode hornsGroup = miscGroup.getChildren().get(0);
        assertEquals("cardinality:family:goat_horns", hornsGroup.getKey());
        assertEquals("Goat Horns", hornsGroup.getLabel().getString());
        assertTrue(hornsGroup.isHighCardinality());
        assertEquals(List.of("Goat Horn Admire", "Goat Horn Seek", "Goat Horn Sing", "Goat Horn Yearn"),
                leafLabels(List.of(hornsGroup)));
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
    void cheatAccessNodesOnlyShowInCheatOrDevMode() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.NONE,
                Set.of(),
                Set.of()
        );

        SearchNode survival = item("energy_tablet", "Energy Tablet", Map.of(
                SearchNodeKeys.ACCESS_LEVEL, "survival"
        ));
        SearchNode full = item("energy_tablet_full", "Energy Tablet", Map.of(
                SearchNodeKeys.ACCESS_LEVEL, "cheat"
        ));

        List<TreeNode> survivalOnly = processor.processFlat(List.of(survival, full));
        assertEquals(1, survivalOnly.size());
        assertEquals("minecraft:energy_tablet", survivalOnly.get(0).getEntry().id().toString());

        AmiConfig.cheatMode = true;
        List<TreeNode> cheatVisible = processor.processFlat(List.of(survival, full));
        assertEquals(2, cheatVisible.size());
    }

    @Test
    void hiddenNodesFollowShowHiddenModItemsConfig() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.NONE,
                Set.of(),
                Set.of()
        );

        SearchNode visible = item("controller", "ME Controller", Map.of());
        SearchNode hidden = item("missing_content", "Missing Content", Map.of(
                SearchNodeKeys.VISIBILITY, "hidden",
                SearchNodeKeys.ACCESS_LEVEL, "dev"
        ));

        assertEquals(List.of("ME Controller"), leafLabels(processor.processFlat(List.of(visible, hidden))));

        AmiConfig.showHiddenModItems = true;
        assertEquals(List.of("ME Controller"), leafLabels(processor.processFlat(List.of(visible, hidden))));

        AmiConfig.devMode = true;
        assertEquals(List.of("ME Controller", "Missing Content"),
                leafLabels(processor.processFlat(List.of(visible, hidden))));
    }

    @Test
    void hiddenSurvivalNodesShowWhenHiddenModItemsAreEnabled() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.NONE,
                Set.of(),
                Set.of()
        );

        SearchNode visible = item("controller", "ME Controller", Map.of());
        SearchNode hidden = item("facade", "Cable Facade", Map.of(
                SearchNodeKeys.VISIBILITY, "hidden",
                SearchNodeKeys.ACCESS_LEVEL, "survival"
        ));

        assertEquals(List.of("ME Controller"), leafLabels(processor.processFlat(List.of(visible, hidden))));

        AmiConfig.showHiddenModItems = true;
        assertEquals(List.of("Cable Facade", "ME Controller"),
                leafLabels(processor.processFlat(List.of(visible, hidden))));
    }

    @Test
    void processFlatWithCardGroupingKeepsFlatResultsButCollapsesFamiliesAsCards() {
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
        assertTrue(flat.get(0).isHighCardinality());
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
        assertEquals(8, doorsGroup.getChildren().size());
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

        List<SearchNode> nodes = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            nodes.add(item("oak_chair_" + i, "Oak Chair " + i, Map.of(
                    SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                    SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "furniture",
                    SearchNodeKeys.FACETS, "placeable,decorative_block"
            )));
            nodes.add(item("oak_table_" + i, "Oak Table " + i, Map.of(
                    SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                    SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "furniture",
                    SearchNodeKeys.FACETS, "placeable,decorative_block"
            )));
            nodes.add(item("crusher_" + i, "Crusher " + i, Map.of(
                    SearchNodeKeys.ONTOLOGY_CATEGORY, "tech",
                    SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "machines",
                    SearchNodeKeys.FACETS, "placeable,machine"
            )));
            nodes.add(item("copper_pipe_" + i, "Copper Pipe " + i, Map.of(
                    SearchNodeKeys.ONTOLOGY_CATEGORY, "tech",
                    SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "parts",
                    SearchNodeKeys.FACETS, "tech_component"
            )));
            nodes.add(item("tomato_seed_" + i, "Tomato Seed " + i, Map.of(
                    SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                    SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "seeds",
                    SearchNodeKeys.FACETS, "seed"
            )));
        }

        List<TreeNode> root = processor.process(nodes);

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
                item("barrel", "Barrel", Map.of(
                        SearchNodeKeys.FACETS, "storage",
                        SearchNodeKeys.STORAGE_ITEM_KIND, "barrel"
                )),
                item("sword", "Sword", Map.of(SearchNodeKeys.DPS, "8.0"))
        ), state, null, false, false);

        assertEquals(1, projection.displayedItemCount());
        assertEquals(List.of("Barrel"), leafLabels(projection.roots()));
    }

    @Test
    void gridProjectionUsesRegistryOrderAndKeepsReverseInsteadOfSpecialSort() {
        SearchState state = new SearchState();
        state.setViewMode(ResultsToolbar.ViewMode.GRID);
        state.setGroupBy(ResultsProcessor.GroupBy.NONE);
        state.setSortField(ResultsProcessor.SortField.ALPHABETICAL);

        List<SearchNode> nodes = List.of(
                pokemon("abomasnow", "Abomasnow", 460),
                pokemon("bulbasaur", "Bulbasaur", 1)
        );

        ResultsViewProjector.Projection ascending = ResultsViewProjector.project(nodes, state, null, false, false);
        assertEquals(List.of("Bulbasaur", "Abomasnow"), leafLabels(ascending.roots()));

        state.setAscending(false);
        ResultsViewProjector.Projection descending = ResultsViewProjector.project(nodes, state, null, false, false);
        assertEquals(List.of("Abomasnow", "Bulbasaur"), leafLabels(descending.roots()));
    }

    @Test
    void compactGridProjectionUsesRegistryOrderAndKeepsReverseInsteadOfSpecialSort() {
        SearchState state = new SearchState();
        state.setViewMode(ResultsToolbar.ViewMode.LIST);
        state.setGroupBy(ResultsProcessor.GroupBy.NONE);
        state.setSortField(ResultsProcessor.SortField.ALPHABETICAL);

        List<SearchNode> nodes = List.of(
                pokemon("abomasnow", "Abomasnow", 460),
                pokemon("bulbasaur", "Bulbasaur", 1)
        );

        ResultsViewProjector.Projection ascending = ResultsViewProjector.project(nodes, state, null, true, false);
        assertEquals(List.of("Bulbasaur", "Abomasnow"), leafLabels(ascending.roots()));

        state.setAscending(false);
        ResultsViewProjector.Projection descending = ResultsViewProjector.project(nodes, state, null, true, false);
        assertEquals(List.of("Abomasnow", "Bulbasaur"), leafLabels(descending.roots()));
    }

    @Test
    void damageFieldIncludesItemsAndEntitiesWithAttackDamage() {
        SearchNode mace = item("mace", "Mace", Map.of(SearchNodeKeys.ATTACK_DAMAGE, "6.0"));
        SearchNode zombie = new SearchNode(
                new Identifier("minecraft:zombie"),
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
    void compatCategoryHeadersUseResolvedNameOrderButKeepRegistrySortedContents() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        SearchNode cobblemon = item("stick", "Cobblemon Entry", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "cobblemon",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "misc"
        ));
        SearchNode ae2Second = item("oak_planks", "AE2 Later Registry Entry", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "ae2",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "storage"
        ));
        SearchNode ae2First = item("stone", "AE2 Earlier Registry Entry", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "ae2",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "storage"
        ));

        List<TreeNode> root = processor.process(List.of(cobblemon, ae2Second, ae2First));

        assertEquals(List.of("ae2", "cobblemon"), root.stream().map(TreeNode::getKey).toList());
        assertEquals(List.of("AE2 Earlier Registry Entry", "AE2 Later Registry Entry"),
                leafLabels(root.get(0).getChildren()));
    }

    @Test
    void smallDyeKindSetStaysDirectlyUnderDyes() {
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
        assertEquals(4, dyesGroup.getChildren().size());
        assertTrue(dyesGroup.getChildren().stream().allMatch(TreeNode::isLeaf));
    }

    @Test
    void explodedDyeKindSetUsesSingleFamilyCardUnderDyesSubcategory() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                collapsedDye("white_dye", "White Dye", "white"),
                collapsedDye("orange_dye", "Orange Dye", "orange"),
                collapsedDye("magenta_dye", "Magenta Dye", "magenta"),
                collapsedDye("light_blue_dye", "Light Blue Dye", "light_blue"),
                collapsedDye("yellow_dye", "Yellow Dye", "yellow"),
                collapsedDye("lime_dye", "Lime Dye", "lime"),
                collapsedDye("pink_dye", "Pink Dye", "pink"),
                collapsedDye("gray_dye", "Gray Dye", "gray"),
                collapsedDye("light_gray_dye", "Light Gray Dye", "light_gray"),
                collapsedDye("cyan_dye", "Cyan Dye", "cyan"),
                collapsedDye("purple_dye", "Purple Dye", "purple"),
                collapsedDye("blue_dye", "Blue Dye", "blue"),
                collapsedDye("brown_dye", "Brown Dye", "brown"),
                collapsedDye("green_dye", "Green Dye", "green"),
                collapsedDye("red_dye", "Red Dye", "red"),
                collapsedDye("black_dye", "Black Dye", "black"),
                item("ink_sac", "Ink Sac", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "dyes"
                )),
                item("glow_ink_sac", "Glow Ink Sac", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "dyes"
                ))
        ));

        assertEquals("""
                Ingredients [expanded]
                  Dyes & Pigments [expanded]
                    Dyes [expanded] [cardinality]
                      Black Dye
                      Blue Dye
                      Brown Dye
                      Cyan Dye
                      Gray Dye
                      Green Dye
                      Light Blue Dye
                      Light Gray Dye
                      Lime Dye
                      Magenta Dye
                      Orange Dye
                      Pink Dye
                      Purple Dye
                      Red Dye
                      White Dye
                      Yellow Dye
                    Glow Ink Sac
                    Ink Sac
                """, ResultsTreeDump.dump(root));
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
    void smallCuratedKindSetsStayDirectlyUnderSubcategory() {
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

        TreeNode candleGroup = lightingGroup.getChildren().get(0);
        assertTrue(candleGroup.isHighCardinality());
        assertEquals(4, candleGroup.getChildren().size());
        assertEquals("Candle", candleGroup.getChildren().get(0).getLabel().getString());
    }

    @Test
    void explicitColoredFamilyIncludesUncoloredBaseItem() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
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
                        SearchNodeKeys.COLLAPSE_FAMILY, "minecraft:candle",
                        SearchNodeKeys.COLLAPSE_LABEL, "Candles",
                        SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed"
                )),
                item("black_candle", "Black Candle", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting",
                        SearchNodeKeys.COLLAPSE_FAMILY, "minecraft:candle",
                        SearchNodeKeys.COLLAPSE_LABEL, "Candles",
                        SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed"
                )),
                item("red_candle", "Red Candle", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting",
                        SearchNodeKeys.COLLAPSE_FAMILY, "minecraft:candle",
                        SearchNodeKeys.COLLAPSE_LABEL, "Candles",
                        SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed"
                )),
                item("blue_candle", "Blue Candle", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting",
                        SearchNodeKeys.COLLAPSE_FAMILY, "minecraft:candle",
                        SearchNodeKeys.COLLAPSE_LABEL, "Candles",
                        SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed"
                ))
        ));

        TreeNode candlesGroup = root.get(0).getChildren().get(0).getChildren().get(0);
        assertEquals("cardinality:family:minecraft:candle", candlesGroup.getKey());
        assertEquals(List.of("Candle", "Black Candle", "Blue Candle", "Red Candle", "White Candle"),
                leafLabels(List.of(candlesGroup)));
    }

    @Test
    void guideBookVariantsDoNotCollapseAsHighCardinalityFamily() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                patchouliGuide("apotheosis", "apoth_chronicle", "Chronicle of Shadows"),
                patchouliGuide("cobblepedia", "cobblepedia", "Poképedia"),
                patchouliGuide("reactive", "journal", "Journal of Alchemy"),
                patchouliGuide("modulargolems", "golem_guide", "Modular Golem Guide")
        ));

        TreeNode booksGroup = root.get(0).getChildren().get(0);
        assertEquals("utility/books", booksGroup.getKey());
        assertTrue(booksGroup.getChildren().stream().allMatch(TreeNode::isLeaf),
                ResultsTreeDump.dump(List.of(booksGroup)));
        assertEquals(List.of("Chronicle of Shadows", "Journal of Alchemy", "Modular Golem Guide", "Poképedia"),
                leafLabels(List.of(booksGroup)));
    }

    @Test
    void potterySherdsCollapseUnderMineralIngredientsSubcategory() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                item("amethyst_shard", "Amethyst Shard", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "mineral"
                )),
                potterySherd("angler_pottery_sherd", "Angler Pottery Sherd"),
                potterySherd("archer_pottery_sherd", "Archer Pottery Sherd"),
                potterySherd("brewer_pottery_sherd", "Brewer Pottery Sherd"),
                potterySherd("explorer_pottery_sherd", "Explorer Pottery Sherd"),
                item("breeze_rod", "Breeze Rod", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "mineral"
                ))
        ));

        TreeNode mineralGroup = root.get(0).getChildren().get(0);
        assertEquals("ingredients/mineral", mineralGroup.getKey());
        assertEquals(3, mineralGroup.getChildren().size());

        TreeNode potteryGroup = mineralGroup.getChildren().stream()
                .filter(node -> "cardinality:minecraft:pottery_sherd".equals(node.getKey()))
                .findFirst()
                .orElseThrow();
        assertEquals("Pottery Sherd", potteryGroup.getLabel().getString());
        assertTrue(potteryGroup.isHighCardinality());
        assertTrue(potteryGroup.isExpanded());
        assertEquals(List.of(
                "Angler Pottery Sherd",
                "Archer Pottery Sherd",
                "Brewer Pottery Sherd",
                "Explorer Pottery Sherd"
        ), leafLabels(List.of(potteryGroup)));
    }

    @Test
    void categoryGroupingDoesNotCreateMaterialShapeVariantGroups() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                item("spruce_planks_brick_pattern", "Spruce Plank Brick Pattern", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "full_block",
                        SearchNodeKeys.SUBTYPE_OF, "rechiseled:spruce_patterned"
                )),
                item("spruce_planks_small_brick_pattern", "Spruce Plank Small Brick Pattern", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "full_block",
                        SearchNodeKeys.SUBTYPE_OF, "rechiseled:spruce_patterned"
                )),
                item("spruce_planks_diagonal_pattern", "Spruce Plank Diagonal Pattern", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "full_block",
                        SearchNodeKeys.SUBTYPE_OF, "rechiseled:spruce_patterned"
                )),
                item("spruce_planks_large_tiles", "Spruce Plank Large Tiles", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "full_block",
                        SearchNodeKeys.SUBTYPE_OF, "rechiseled:spruce_patterned"
                ))
        ));

        TreeNode fullBlocks = root.get(0).getChildren().get(0);
        assertEquals("masonry/full_block", fullBlocks.getKey());
        assertEquals(4, fullBlocks.getChildren().size());
        assertTrue(fullBlocks.getChildren().stream().allMatch(TreeNode::isLeaf));
    }

    @Test
    void categoryGroupingCondensesSlabsWithSharedMaterialRoots() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                item("sandstone_slab", "Sandstone Slab", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "slab",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:sandstone"
                )),
                item("cut_sandstone_slab", "Cut Sandstone Slab", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "slab",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:sandstone"
                )),
                item("red_sandstone_slab", "Red Sandstone Slab", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "slab",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:red_sandstone"
                )),
                item("cut_red_sandstone_slab", "Cut Red Sandstone Slab", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "slab",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:red_sandstone"
                )),
                item("stone_slab", "Stone Slab", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "slab",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:stone"
                ))
        ));

        assertEquals("""
                Building [expanded]
                  Slab [expanded]
                    Red Sandstone Slabs [expanded] [cardinality]
                      Cut Red Sandstone Slab
                      Red Sandstone Slab
                    Sandstone Slabs [expanded] [cardinality]
                      Cut Sandstone Slab
                      Sandstone Slab
                    Stone Slab
                """, ResultsTreeDump.dump(root));
    }

    @Test
    void categoryGroupingCondensesStairsAndWallsWithSharedMaterialRoots() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                item("sandstone_stairs", "Sandstone Stairs", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "stairs",
                        SearchNodeKeys.MATERIAL_GROUP, "minecraft:sandstone"
                )),
                item("smooth_sandstone_stairs", "Smooth Sandstone Stairs", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "stairs",
                        SearchNodeKeys.MATERIAL_GROUP, "minecraft:sandstone"
                )),
                item("short_red_brick_wall", "Short Red Brick Wall", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "wall",
                        SearchNodeKeys.SUBTYPE_OF, "createdeco:red_brick"
                )),
                item("tall_red_brick_wall", "Tall Red Brick Wall", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "wall",
                        SearchNodeKeys.SUBTYPE_OF, "createdeco:red_brick"
                ))
        ));

        assertEquals("""
                Building [expanded]
                  Stairs [expanded]
                    Sandstone Stairs [expanded] [cardinality]
                      Sandstone Stairs
                      Smooth Sandstone Stairs
                  Wall [expanded]
                    Red Brick Walls [expanded] [cardinality]
                      Short Red Brick Wall
                      Tall Red Brick Wall
                """, ResultsTreeDump.dump(root));
    }

    @Test
    void categoryGroupingSplitsMixedFullBlocksByGlassAndSpecialBehavior() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        Map<String, String> fullBlockBase = Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "full_block",
                "blockShape", "full_block"
        );
        SearchNode stone = item("polished_stone", "Polished Stone", fullBlockBase);
        SearchNode glass = item("clear_glass", "Clear Glass", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "full_block",
                SearchNodeKeys.FACETS, "placeable,glass_block",
                SearchNodeKeys.BLOCKS_MATERIAL, "glass",
                "blockShape", "full_block"
        ));
        SearchNode idol = node(NodeType.ITEM, "pastel", "ender_dragon_idol", "Mighty Idol", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "full_block",
                SearchNodeKeys.FACETS, "placeable",
                SearchNodeKeys.BLOCK_STATE_PROPERTIES, "cooldown",
                "blockShape", "full_block"
        ));
        SearchNode bomb = node(NodeType.ITEM, "alexscaves", "nuclear_bomb", "Nuclear Bomb", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "full_block",
                SearchNodeKeys.FACETS, "placeable",
                SearchNodeKeys.BLOCK_TAGS, "alexscaves:remote_detonator_activates",
                "blockShape", "full_block"
        ));

        List<TreeNode> root = processor.process(List.of(stone, glass, idol, bomb));

        assertEquals("""
                Building [expanded]
                  Full Block [expanded]
                    Glass [expanded]
                      Clear Glass
                    Special Blocks [expanded]
                      Mighty Idol
                      Nuclear Bomb
                    Plain Blocks [expanded]
                      Polished Stone
                """, ResultsTreeDump.dump(root));
    }

    @Test
    void categoryGroupingCondensesRechiseledMaterialFamilies() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        Map<String, String> meta = Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "full_block",
                SearchNodeKeys.COLLAPSE_FAMILY, "rechiseled:acacia_planks",
                SearchNodeKeys.COLLAPSE_LABEL, "Acacia Planks",
                SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed"
        );

        List<TreeNode> root = processor.process(List.of(
                item("acacia_planks_beams", "Acacia Plank Beams", meta),
                item("acacia_planks_bricks", "Acacia Plank Bricks", meta),
                item("acacia_planks_crate", "Acacia Planks Crate", meta),
                item("acacia_planks_tiles", "Acacia Plank Tiles", meta)
        ));

        assertTrue(hasKeyStartingWith(root, "cardinality:family:rechiseled:acacia_planks"),
                ResultsTreeDump.dump(root));
    }

    @Test
    void categoryGroupingCollapsesTwoDefaultCollapsedCreativeStackVariants() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        Map<String, String> meta = Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "pastel",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "equipment",
                SearchNodeKeys.COLLAPSE_FAMILY, "pastel:heartsingers_reward",
                SearchNodeKeys.COLLAPSE_LABEL, "Heartsingers Reward",
                SearchNodeKeys.SUBTYPE_OF, "pastel:heartsingers_reward",
                SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed"
        );

        List<TreeNode> root = processor.process(List.of(
                node(NodeType.ITEM, "pastel", "heartsingers_reward/variant/heartsinger_s_reward_65823d7a6d05", "Heartsinger's Reward", meta),
                node(NodeType.ITEM, "pastel", "heartsingers_reward/variant/heartsinger_s_reward_d0db18426600", "Heartsinger's Reward", meta)
        ));

        assertEquals("""
                Pastel [expanded]
                  Equipment [expanded]
                    Heartsingers Reward [expanded] [cardinality]
                      Heartsinger's Reward
                      Heartsinger's Reward
                """, ResultsTreeDump.dump(root));
    }

    @Test
    void flatCardGroupingCollapsesTwoDefaultCollapsedCreativeStackVariants() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        Map<String, String> meta = Map.of(
                SearchNodeKeys.COLLAPSE_FAMILY, "pastel:heartsingers_reward",
                SearchNodeKeys.COLLAPSE_LABEL, "Heartsingers Reward",
                SearchNodeKeys.SUBTYPE_OF, "pastel:heartsingers_reward",
                SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed"
        );

        List<TreeNode> root = processor.processFlatWithCardGrouping(List.of(
                node(NodeType.ITEM, "pastel", "heartsingers_reward/variant/heartsinger_s_reward_65823d7a6d05", "Heartsinger's Reward", meta),
                node(NodeType.ITEM, "pastel", "heartsingers_reward/variant/heartsinger_s_reward_d0db18426600", "Heartsinger's Reward", meta)
        ));

        assertEquals("""
                Heartsingers Reward [expanded] [cardinality]
                  Heartsinger's Reward
                  Heartsinger's Reward
                """, ResultsTreeDump.dump(root));
    }

    @Test
    void categoryGroupingCollapsesSilentGemsGemNamedBlockVariantsByShape() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        Map<String, String> meta = Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "full_block",
                SearchNodeKeys.COLLAPSE_FAMILY, "silentgems:smooth_stone",
                SearchNodeKeys.COLLAPSE_LABEL, "Smooth Gem Stones",
                SearchNodeKeys.MATERIAL_GROUP, "silentgems:smooth_stone",
                SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed"
        );

        List<TreeNode> root = processor.process(List.of(
                node(NodeType.ITEM, "silentgems", "smooth_rose_quartz", "Smooth Rose Quartz Stone", meta),
                node(NodeType.ITEM, "silentgems", "smooth_ammolite", "Smooth Ammolite Stone", meta)
        ));

        assertEquals("""
                Building [expanded]
                  Full Block [expanded]
                    Smooth Gem Stones [expanded] [cardinality]
                      Smooth Ammolite Stone
                      Smooth Rose Quartz Stone
                """, ResultsTreeDump.dump(root));
    }

    @Test
    void categoryGroupingUsesHalcyonHeaderForDatanessenceCompatRoute() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                node(NodeType.ITEM, "datanessence", "item_filter", "Item Filter", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "halcyon",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "machines"
                )),
                node(NodeType.ITEM, "datanessence", "tag_filter", "Tag Filter Label", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "halcyon",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "parts"
                ))
        ));

        assertEquals("""
                Halcyon [expanded]
                  Machines [expanded]
                    Item Filter
                  Parts [expanded]
                    Tag Filter Label
                """, ResultsTreeDump.dump(root));
    }

    @Test
    void categoryGroupingCollapsesCreativeStackVariants() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        Map<String, String> variantMeta = Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "sophisticated",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "storage",
                SearchNodeKeys.SUBTYPE_OF, "sophisticatedstorage:barrel",
                SearchNodeKeys.VARIANT_SOURCE, "creative_tab",
                SearchNodeKeys.VARIANT_COLLAPSE_MODE, "auto"
        );

        List<TreeNode> root = processor.process(List.of(
                item("oak_barrel_variant", "Oak Barrel", variantMeta),
                item("spruce_barrel_variant", "Spruce Barrel", variantMeta),
                item("birch_barrel_variant", "Birch Barrel", variantMeta),
                item("jungle_barrel_variant", "Jungle Barrel", variantMeta)
        ));

        assertTrue(hasKeyStartingWith(root, "cardinality:sophisticatedstorage:barrel"),
                ResultsTreeDump.dump(root));
    }

    @Test
    void categoryGroupingCondensesTintableGeneratedShapeFamilies() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        Map<String, String> baseMeta = Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "redstone",
                SearchNodeKeys.COLLAPSE_FAMILY, "colors:tintable/yellow/buttons",
                SearchNodeKeys.COLLAPSE_LABEL, "Yellow Buttons",
                SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed"
        );

        List<TreeNode> root = processor.process(List.of(
                item("yellow_stone_button", "Yellow Stone Button", baseMeta),
                item("yellow_prismarine_button", "Yellow Prismarine Button", baseMeta),
                item("yellow_deepslate_button", "Yellow Deepslate Button", baseMeta),
                item("yellow_calcite_button", "Yellow Calcite Button", baseMeta)
        ));

        assertEquals("""
                Building [expanded]
                  Redstone [expanded]
                    Yellow Buttons [expanded] [cardinality]
                      Yellow Calcite Button
                      Yellow Deepslate Button
                      Yellow Prismarine Button
                      Yellow Stone Button
                """, ResultsTreeDump.dump(root));
    }

    @Test
    void categoryGroupingCondensesColoredLinguisticGlyphFamilies() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        Map<String, String> baseMeta = Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "misc",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "glyphs",
                SearchNodeKeys.COLLAPSE_FAMILY, "atlantis:linguistic_glyph/yellow",
                SearchNodeKeys.COLLAPSE_LABEL, "Yellow Linguistic Glyphs",
                SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed"
        );

        List<TreeNode> root = processor.process(List.of(
                item("yellow_linguistic_glyph_a", "Yellow Linguistic Glyph A", baseMeta),
                item("yellow_linguistic_glyph_b", "Yellow Linguistic Glyph B", baseMeta),
                item("yellow_linguistic_glyph_e", "Yellow Linguistic Glyph E", baseMeta),
                item("yellow_linguistic_glyph_f", "Yellow Linguistic Glyph F", baseMeta)
        ));

        assertTrue(hasKeyStartingWith(root, "cardinality:family:atlantis:linguistic_glyph/yellow"),
                ResultsTreeDump.dump(root));
    }

    @Test
    void categoryGroupingSkipsNonItemTerminalMiscNodes() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                item("paper_scrap", "Paper Scrap", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "misc",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "unknown"
                )),
                node(NodeType.INGREDIENT, "minecraft", "nether/obtain_crying_obsidian", "Who is Cutting Onions?", Map.of()),
                node(NodeType.FLUID, "minecraft", "water", "Water", Map.of()),
                node(NodeType.ENTITY, "minecraft", "area_effect_cloud", "Area Effect Cloud", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "misc",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "unknown"
                )),
                node(NodeType.ENTITY, "minecraft", "allay", "Allay", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "bestiary",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "passive"
                ))
        ));

        assertEquals("""
                Bestiary [expanded]
                  Passive [expanded]
                    Allay
                Misc [expanded]
                  Unknown [expanded]
                    Paper Scrap
                """, ResultsTreeDump.dump(root));
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
                  Red Banner
                  White Banner
                  Flower Banner Pattern
                """, ResultsTreeDump.dump(List.of(bannersGroup)));
    }

    @Test
    void textileBannersCollapseUnderTextilesSubcategory() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(
                textileBanner("white_banner", "White Banner"),
                textileBanner("black_banner", "Black Banner"),
                textileBanner("red_banner", "Red Banner"),
                textileBanner("blue_banner", "Blue Banner")
        ));

        assertEquals("""
                Decoration [expanded]
                  Textiles [expanded]
                    Banners [expanded] [cardinality]
                      Black Banner
                      Blue Banner
                      Red Banner
                      White Banner
                """, ResultsTreeDump.dump(root));
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
                    Candle [expanded] [cardinality]
                      Candle
                      Black Candle
                      Red Candle
                      White Candle
                Ingredients [expanded]
                  Dyes & Pigments [expanded]
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

    @Test
    void categoryGroupingShowsCustomCategoryFixes() {
        ResultsProcessor processor = new ResultsProcessor(
                ResultsProcessor.SortField.ALPHABETICAL,
                true,
                ResultsProcessor.GroupBy.CATEGORY,
                Set.of(),
                Set.of()
        );

        List<TreeNode> root = processor.process(List.of(item("scanner", "Scanner", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "automation",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "diagnostics"
        ))));

        assertEquals("""
                Automation [expanded]
                  Diagnostics [expanded]
                    Scanner
                """, ResultsTreeDump.dump(root));
    }
}
