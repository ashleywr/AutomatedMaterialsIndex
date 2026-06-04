package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResultsShapeExplorerTest {
    private static SearchState state(ResultsProcessor.SortField sortField,
                                     ResultsProcessor.GroupBy groupBy,
                                     ResultsToolbar.ViewMode viewMode) {
        SearchState state = new SearchState();
        state.setSortField(sortField);
        state.setGroupBy(groupBy);
        state.setViewMode(viewMode);
        return state;
    }

    private static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if ((Files.exists(current.resolve("settings.gradle")) && Files.exists(current.resolve("gradle.properties")))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root");
    }

    private static List<SearchNode> fixtureNodes() {
        return List.of(
                item("minecraft", "white_banner", "White Banner", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "banners",
                        SearchNodeKeys.COLLAPSE_FAMILY, "banners",
                        SearchNodeKeys.COLLAPSE_LABEL, "Banners"
                )),
                item("minecraft", "black_banner", "Black Banner", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "banners",
                        SearchNodeKeys.COLLAPSE_FAMILY, "banners",
                        SearchNodeKeys.COLLAPSE_LABEL, "Banners"
                )),
                item("minecraft", "red_banner", "Red Banner", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "banners",
                        SearchNodeKeys.COLLAPSE_FAMILY, "banners",
                        SearchNodeKeys.COLLAPSE_LABEL, "Banners"
                )),
                item("minecraft", "blue_banner", "Blue Banner", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "banners",
                        SearchNodeKeys.COLLAPSE_FAMILY, "banners",
                        SearchNodeKeys.COLLAPSE_LABEL, "Banners"
                )),
                item("minecraft", "flower_banner_pattern", "Flower Banner Pattern", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "banners"
                )),
                item("minecraft", "candle", "Candle", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting"
                )),
                item("minecraft", "white_candle", "White Candle", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:candle"
                )),
                item("minecraft", "black_candle", "Black Candle", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:candle"
                )),
                item("minecraft", "red_candle", "Red Candle", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:candle"
                )),
                dye("white_dye", "White Dye", "white"),
                dye("black_dye", "Black Dye", "black"),
                dye("red_dye", "Red Dye", "red"),
                dye("blue_dye", "Blue Dye", "blue"),
                item("minecraft", "red_mushroom", "Red Mushroom", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "fungi",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:mushroom"
                )),
                item("minecraft", "brown_mushroom", "Brown Mushroom", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "fungi",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:mushroom"
                )),
                item("minecraft", "crimson_fungus", "Crimson Fungus", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "fungi",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:mushroom"
                )),
                item("minecraft", "warped_fungus", "Warped Fungus", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "fungi",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:mushroom"
                )),
                item("minecraft", "crimson_nylium", "Crimson Nylium", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "fungi"
                )),
                item("minecraft", "warped_nylium", "Warped Nylium", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "fungi"
                )),
                item("create", "zinc_ingot", "Zinc Ingot", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "metals",
                        SearchNodeKeys.MATERIAL_GROUP, "create:zinc"
                )),
                item("create", "zinc_nugget", "Zinc Nugget", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "metals",
                        SearchNodeKeys.MATERIAL_GROUP, "create:zinc"
                )),
                item("create", "zinc_sheet", "Zinc Sheet", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "metals",
                        SearchNodeKeys.MATERIAL_GROUP, "create:zinc"
                )),
                item("create", "zinc_block", "Block of Zinc", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "full_block",
                        SearchNodeKeys.MATERIAL_GROUP, "create:zinc"
                )),
                item("thermal", "tin_ingot", "Tin Ingot", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "metals",
                        SearchNodeKeys.MATERIAL_GROUP, "thermal:tin"
                )),
                item("thermal", "tin_nugget", "Tin Nugget", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "metals",
                        SearchNodeKeys.MATERIAL_GROUP, "thermal:tin"
                )),
                item("thermal", "tin_plate", "Tin Plate", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "metals",
                        SearchNodeKeys.MATERIAL_GROUP, "thermal:tin"
                )),
                item("thermal", "tin_gear", "Tin Gear", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "tech",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "parts",
                        SearchNodeKeys.MATERIAL_GROUP, "thermal:tin"
                )),
                item("botania", "livingwood_log", "Livingwood Log", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "nature",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "wood",
                        SearchNodeKeys.MATERIAL_GROUP, "botania:livingwood"
                )),
                item("botania", "livingwood_planks", "Livingwood Planks", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "full_block",
                        SearchNodeKeys.MATERIAL_GROUP, "botania:livingwood"
                )),
                item("botania", "livingwood_stairs", "Livingwood Stairs", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "stairs",
                        SearchNodeKeys.MATERIAL_GROUP, "botania:livingwood"
                )),
                item("examplemod", "black_pigment", "Black Pigment", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "dyes",
                        SearchNodeKeys.MATERIAL_GROUP, "examplemod:pigment",
                        SearchNodeKeys.COLOR_BUCKET, "black"
                )),
                item("examplemod", "white_pigment", "White Pigment", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "dyes",
                        SearchNodeKeys.MATERIAL_GROUP, "examplemod:pigment",
                        SearchNodeKeys.COLOR_BUCKET, "white"
                )),
                item("examplemod", "blue_pigment", "Blue Pigment", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "dyes",
                        SearchNodeKeys.MATERIAL_GROUP, "examplemod:pigment",
                        SearchNodeKeys.COLOR_BUCKET, "blue"
                )),
                item("examplemod", "pigment_base", "Pigment Base", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "dyes",
                        SearchNodeKeys.MATERIAL_GROUP, "examplemod:pigment"
                )),
                item("examplemod", "red_pigment", "Red Pigment", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "dyes",
                        SearchNodeKeys.MATERIAL_GROUP, "examplemod:pigment",
                        SearchNodeKeys.COLOR_BUCKET, "red"
                )),
                item("examplemod", "cobalt_chair", "Cobalt Chair", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "furniture",
                        SearchNodeKeys.MATERIAL_GROUP, "examplemod:cobalt"
                )),
                item("examplemod", "cobalt_table", "Cobalt Table", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "furniture",
                        SearchNodeKeys.MATERIAL_GROUP, "examplemod:cobalt"
                )),
                item("examplemod", "cobalt_lamp", "Cobalt Lamp", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting",
                        SearchNodeKeys.MATERIAL_GROUP, "examplemod:cobalt"
                )),
                item("examplemod", "cobalt_sconce", "Cobalt Sconce", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "decoration",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "lighting",
                        SearchNodeKeys.MATERIAL_GROUP, "examplemod:cobalt"
                )),
                item("examplemod", "small_rune_a", "Small Rune A", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "magic",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "reagents",
                        SearchNodeKeys.COLLAPSE_FAMILY, "small_runes",
                        SearchNodeKeys.COLLAPSE_LABEL, "Small Runes"
                )),
                item("examplemod", "small_rune_b", "Small Rune B", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "magic",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "reagents",
                        SearchNodeKeys.COLLAPSE_FAMILY, "small_runes",
                        SearchNodeKeys.COLLAPSE_LABEL, "Small Runes"
                )),
                item("examplemod", "small_rune_c", "Small Rune C", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "magic",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "reagents",
                        SearchNodeKeys.COLLAPSE_FAMILY, "small_runes",
                        SearchNodeKeys.COLLAPSE_LABEL, "Small Runes"
                )),
                item("examplemod", "large_rune_a", "Large Rune A", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "magic",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "reagents",
                        SearchNodeKeys.COLLAPSE_FAMILY, "large_runes",
                        SearchNodeKeys.COLLAPSE_LABEL, "Large Runes"
                )),
                item("examplemod", "large_rune_b", "Large Rune B", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "magic",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "reagents",
                        SearchNodeKeys.COLLAPSE_FAMILY, "large_runes",
                        SearchNodeKeys.COLLAPSE_LABEL, "Large Runes"
                )),
                item("examplemod", "large_rune_c", "Large Rune C", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "magic",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "reagents",
                        SearchNodeKeys.COLLAPSE_FAMILY, "large_runes",
                        SearchNodeKeys.COLLAPSE_LABEL, "Large Runes"
                )),
                item("examplemod", "large_rune_d", "Large Rune D", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "magic",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "reagents",
                        SearchNodeKeys.COLLAPSE_FAMILY, "large_runes",
                        SearchNodeKeys.COLLAPSE_LABEL, "Large Runes"
                )),
                item("moda", "copper_coin", "Copper Coin", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "metals",
                        SearchNodeKeys.MATERIAL_GROUP, "moda:copper_coin"
                )),
                item("modb", "copper_coin", "Copper Coin", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "metals",
                        SearchNodeKeys.MATERIAL_GROUP, "modb:copper_coin"
                )),
                item("partialmod", "mystery_variant_one", "Mystery Variant One", Map.of(
                        SearchNodeKeys.SUBTYPE_OF, "partialmod:mystery_set"
                )),
                item("partialmod", "mystery_variant_two", "Mystery Variant Two", Map.of(
                        SearchNodeKeys.SUBTYPE_OF, "partialmod:mystery_set"
                )),
                item("partialmod", "mystery_variant_three", "Mystery Variant Three", Map.of(
                        SearchNodeKeys.SUBTYPE_OF, "partialmod:mystery_set"
                )),
                item("partialmod", "mystery_variant_four", "Mystery Variant Four", Map.of(
                        SearchNodeKeys.SUBTYPE_OF, "partialmod:mystery_set"
                )),
                item("partialmod", "nameless_component", "Nameless Component", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients"
                )),
                item("minecraft", "stone", "Stone", Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "full_block"
                ))
        );
    }

    private static SearchNode dye(String path, String displayName, String color) {
        return item("minecraft", path, displayName, Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "dyes",
                SearchNodeKeys.SUBTYPE_OF, "minecraft:dye",
                SearchNodeKeys.MATERIAL_GROUP, "minecraft:dye",
                SearchNodeKeys.COLOR_BUCKET, color
        ));
    }

    private static SearchNode item(String namespace, String path, String displayName, Map<String, String> metadata) {
        return new SearchNode(
                new ResourceLocation(namespace + ":" + path),
                NodeType.ITEM,
                displayName,
                0,
                0,
                metadata
        );
    }

    @BeforeEach
    void setUp() {
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
    }

    @Test
    void writesResultShapeExplorationReport() throws IOException {
        List<SearchNode> fixture = fixtureNodes();
        StringBuilder report = new StringBuilder();
        report.append("# AMI Result Shape Exploration\n\n");
        report.append("This report is diagnostic. Use it to inspect tree and grid projection shapes before turning a concern into a hard regression assertion.\n\n");

        for (ResultsProcessor.GroupBy groupBy : List.of(
                ResultsProcessor.GroupBy.NONE,
                ResultsProcessor.GroupBy.CATEGORY,
                ResultsProcessor.GroupBy.MOD,
                ResultsProcessor.GroupBy.MATERIAL,
                ResultsProcessor.GroupBy.FAMILY
        )) {
            for (ResultsProcessor.SortField sortField : List.of(
                    ResultsProcessor.SortField.REGISTRY,
                    ResultsProcessor.SortField.ALPHABETICAL,
                    ResultsProcessor.SortField.MOD,
                    ResultsProcessor.SortField.COUNT
            )) {
                try {
                    SearchState listState = state(sortField, groupBy, ResultsToolbar.ViewMode.LIST);
                    SearchState compactState = state(sortField, groupBy, ResultsToolbar.ViewMode.GRID);
                    List<TreeNode> tree = ResultsViewProjector.project(fixture, listState, null, false, false).roots();
                    List<TreeNode> compact = ResultsViewProjector.project(fixture, compactState, null, true, false).roots();
                    report.append(ResultsShapeSnapshot.capture(groupBy, sortField, tree, compact, 6).toMarkdown());
                } catch (Throwable t) {
                    report.append("## group=").append(groupBy.name())
                            .append(" sort=").append(sortField.name())
                            .append("\n\n");
                    report.append("Unavailable in JVM explorer: ")
                            .append(t.getClass().getSimpleName())
                            .append(": ")
                            .append(t.getMessage())
                            .append("\n\n");
                    continue;
                }
            }
        }

        Path reportPath = repoRoot().resolve(Path.of("neoforge", "build", "reports", "ami-result-shapes", "result-shapes.md"));
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, report.toString());

        assertTrue(Files.exists(reportPath), "Expected diagnostic report at " + reportPath.toAbsolutePath());
    }
}
