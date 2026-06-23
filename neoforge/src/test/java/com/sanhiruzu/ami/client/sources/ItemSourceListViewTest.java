package com.sanhiruzu.ami.client.sources;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemSourceListViewTest {
    @Test
    void leftClickOnRecipeSourceRowNavigatesToPrimaryMethod() {
        SearchNode craftingTable = new SearchNode(new ResourceLocation("minecraft:crafting_table"), NodeType.ITEM,
                "Crafting Table", 0, 0, Map.of());
        ItemSourceReport report = new ItemSourceReport(
                Component.literal("Sources: Leather"),
                List.of(new ItemSourceRow(
                        ItemSourceType.RECIPE,
                        "Crafting Table -> Leather",
                        List.of(new ItemSourceLink("Crafting Table", craftingTable))
                ))
        );
        ItemSourceListView view = new ItemSourceListView(0, 0, 240, 120);
        AtomicReference<SearchNode> opened = new AtomicReference<>();
        AtomicReference<ItemSourceListView.SourceAction> action = new AtomicReference<>();
        view.setReport(report);
        view.setActionHandler((node, sourceAction, mouseX, mouseY) -> {
            opened.set(node);
            action.set(sourceAction);
            return true;
        });

        assertFalse(view.mouseClicked(4, 5, 0), "group header rows are not navigation targets");
        assertTrue(view.mouseClicked(4, 21, 0));

        assertEquals(craftingTable, opened.get());
        assertEquals(ItemSourceListView.SourceAction.OPEN_LINK, action.get());
    }

    @Test
    void leftClickOnMobDropCardBodyWithoutBiomeLinksDoesNotNavigateToMobSearch() {
        SearchNode cow = new SearchNode(new ResourceLocation("minecraft:cow"), NodeType.ENTITY, "Cow", 0, 0, Map.of());
        ItemSourceReport report = new ItemSourceReport(
                Component.literal("Sources: Leather"),
                List.of(new ItemSourceRow(
                        ItemSourceType.MOB_DROP,
                        "Cow -> drops Leather",
                        List.of(new ItemSourceLink("Cow", cow))
                ))
        );
        ItemSourceListView view = new ItemSourceListView(0, 0, 240, 120);
        AtomicReference<SearchNode> opened = new AtomicReference<>();
        view.setReport(report);
        view.setActionHandler((node, sourceAction, mouseX, mouseY) -> {
            opened.set(node);
            return true;
        });

        assertFalse(view.mouseClicked(12, 25, 0));

        assertNull(opened.get());
    }

    @Test
    void leftClickOnBiomeChipLocatesBiomeInsteadOfOpeningSearch() {
        SearchNode cow = new SearchNode(new ResourceLocation("minecraft:cow"), NodeType.ENTITY, "Cow", 0, 0, Map.of());
        SearchNode savanna = new SearchNode(new ResourceLocation("minecraft:savanna"), NodeType.BIOME, "Savanna", 0, 0, Map.of());
        ItemSourceReport report = new ItemSourceReport(
                Component.literal("Sources: Leather"),
                List.of(new ItemSourceRow(
                        ItemSourceType.MOB_DROP,
                        "Cow -> drops Leather -> spawns in Savanna",
                        List.of(new ItemSourceLink("Cow", cow), new ItemSourceLink("Savanna", savanna)),
                        new ItemSourceLink("Cow", cow),
                        "drops Leather",
                        List.of(new ItemSourceLink("Savanna", savanna))
                ))
        );
        ItemSourceListView view = new ItemSourceListView(0, 0, 240, 120);
        AtomicReference<SearchNode> opened = new AtomicReference<>();
        AtomicReference<ItemSourceListView.SourceAction> action = new AtomicReference<>();
        view.setReport(report);
        view.setActionHandler((node, sourceAction, mouseX, mouseY) -> {
            opened.set(node);
            action.set(sourceAction);
            return true;
        });

        assertTrue(view.mouseClicked(74, 54, 0));

        assertEquals(savanna, opened.get());
        assertEquals(ItemSourceListView.SourceAction.LOCATE_BIOME, action.get());
    }

    @Test
    void leftClickOnRouteOutputItemOpensRecipesForThatItem() {
        SearchNode cow = new SearchNode(new ResourceLocation("minecraft:cow"), NodeType.ENTITY, "Cow", 0, 0, Map.of());
        SearchNode leather = new SearchNode(new ResourceLocation("minecraft:leather"), NodeType.ITEM, "Leather", 0, 0, Map.of());
        ItemSourceReport report = new ItemSourceReport(
                Component.literal("Sources: Leather"),
                List.of(new ItemSourceRow(
                        ItemSourceType.MOB_DROP,
                        "Cow -> drops Leather",
                        List.of(new ItemSourceLink("Cow", cow), new ItemSourceLink("Leather", leather)),
                        new ItemSourceLink("Cow", cow),
                        "drops Leather",
                        List.of()
                ))
        );
        ItemSourceListView view = new ItemSourceListView(0, 0, 240, 120);
        AtomicReference<SearchNode> opened = new AtomicReference<>();
        AtomicReference<ItemSourceListView.SourceAction> action = new AtomicReference<>();
        view.setReport(report);
        view.setActionHandler((node, sourceAction, mouseX, mouseY) -> {
            opened.set(node);
            action.set(sourceAction);
            return true;
        });

        assertTrue(view.mouseClicked(84, 37, 0));

        assertEquals(leather, opened.get());
        assertEquals(ItemSourceListView.SourceAction.OPEN_OUTPUT_RECIPES, action.get());
    }

    @Test
    void rightClickOnRouteOutputItemOpensNormalItemContextMenu() {
        SearchNode cow = new SearchNode(new ResourceLocation("minecraft:cow"), NodeType.ENTITY, "Cow", 0, 0, Map.of());
        SearchNode leather = new SearchNode(new ResourceLocation("minecraft:leather"), NodeType.ITEM, "Leather", 0, 0, Map.of());
        ItemSourceReport report = new ItemSourceReport(
                Component.literal("Sources: Leather"),
                List.of(new ItemSourceRow(
                        ItemSourceType.MOB_DROP,
                        "Cow -> drops Leather",
                        List.of(new ItemSourceLink("Cow", cow), new ItemSourceLink("Leather", leather)),
                        new ItemSourceLink("Cow", cow),
                        "drops Leather",
                        List.of()
                ))
        );
        ItemSourceListView view = new ItemSourceListView(0, 0, 240, 120);
        AtomicReference<SearchNode> opened = new AtomicReference<>();
        AtomicReference<ItemSourceListView.SourceAction> action = new AtomicReference<>();
        view.setReport(report);
        view.setActionHandler((node, sourceAction, mouseX, mouseY) -> {
            opened.set(node);
            action.set(sourceAction);
            return true;
        });

        assertTrue(view.mouseClicked(84, 37, 1));

        assertEquals(leather, opened.get());
        assertEquals(ItemSourceListView.SourceAction.OPEN_OUTPUT_CONTEXT, action.get());
    }

    @Test
    void rightClickOnBiomeChipOpensBiomeContextAction() {
        SearchNode cow = new SearchNode(new ResourceLocation("minecraft:cow"), NodeType.ENTITY, "Cow", 0, 0, Map.of());
        SearchNode savanna = new SearchNode(new ResourceLocation("minecraft:savanna"), NodeType.BIOME, "Savanna", 0, 0, Map.of());
        ItemSourceReport report = new ItemSourceReport(
                Component.literal("Sources: Leather"),
                List.of(new ItemSourceRow(
                        ItemSourceType.MOB_DROP,
                        "Cow -> drops Leather -> spawns in Savanna",
                        List.of(new ItemSourceLink("Cow", cow), new ItemSourceLink("Savanna", savanna)),
                        new ItemSourceLink("Cow", cow),
                        "drops Leather",
                        List.of(new ItemSourceLink("Savanna", savanna))
                ))
        );
        ItemSourceListView view = new ItemSourceListView(0, 0, 240, 120);
        AtomicReference<SearchNode> opened = new AtomicReference<>();
        AtomicReference<ItemSourceListView.SourceAction> action = new AtomicReference<>();
        view.setReport(report);
        view.setActionHandler((node, sourceAction, mouseX, mouseY) -> {
            opened.set(node);
            action.set(sourceAction);
            return true;
        });

        assertTrue(view.mouseClicked(74, 54, 1));

        assertEquals(savanna, opened.get());
        assertEquals(ItemSourceListView.SourceAction.OPEN_BIOME_CONTEXT, action.get());
    }

    @Test
    void leftClickOnMobDropCardBodyWithBiomeLinksDoesNotNavigateToMobSearch() {
        SearchNode cow = new SearchNode(new ResourceLocation("minecraft:cow"), NodeType.ENTITY, "Cow", 0, 0, Map.of());
        SearchNode savanna = new SearchNode(new ResourceLocation("minecraft:savanna"), NodeType.BIOME, "Savanna", 0, 0, Map.of());
        ItemSourceReport report = new ItemSourceReport(
                Component.literal("Sources: Leather"),
                List.of(new ItemSourceRow(
                        ItemSourceType.MOB_DROP,
                        "Cow -> drops Leather -> spawns in Savanna",
                        List.of(new ItemSourceLink("Cow", cow), new ItemSourceLink("Savanna", savanna)),
                        new ItemSourceLink("Cow", cow),
                        "drops Leather",
                        List.of(new ItemSourceLink("Savanna", savanna))
                ))
        );
        ItemSourceListView view = new ItemSourceListView(0, 0, 240, 120);
        AtomicReference<SearchNode> opened = new AtomicReference<>();
        view.setReport(report);
        view.setActionHandler((node, sourceAction, mouseX, mouseY) -> {
            opened.set(node);
            return true;
        });

        assertFalse(view.mouseClicked(12, 25, 0));

        assertNull(opened.get());
    }

    @Test
    void leftClickOnMobDropCardBodyOpensEntityInfoOnlyWhenAvailable() {
        SearchNode cow = new SearchNode(new ResourceLocation("minecraft:cow"), NodeType.ENTITY, "Cow", 0, 0, Map.of());
        ItemSourceReport report = new ItemSourceReport(
                Component.literal("Sources: Leather"),
                List.of(new ItemSourceRow(
                        ItemSourceType.MOB_DROP,
                        "Cow -> drops Leather",
                        List.of(new ItemSourceLink("Cow", cow))
                ))
        );
        ItemSourceListView view = new ItemSourceListView(0, 0, 240, 120);
        AtomicReference<SearchNode> opened = new AtomicReference<>();
        AtomicReference<ItemSourceListView.SourceAction> action = new AtomicReference<>();
        view.setReport(report);
        view.setEntityInfoAvailable(node -> node == cow);
        view.setActionHandler((node, sourceAction, mouseX, mouseY) -> {
            opened.set(node);
            action.set(sourceAction);
            return true;
        });

        assertTrue(view.mouseClicked(12, 25, 0));

        assertEquals(cow, opened.get());
        assertEquals(ItemSourceListView.SourceAction.OPEN_ENTITY_INFO, action.get());
    }

    @Test
    void mobDropCardBodyStaysIdleWhenEntityInfoIsUnavailable() {
        SearchNode cow = new SearchNode(new ResourceLocation("minecraft:cow"), NodeType.ENTITY, "Cow", 0, 0, Map.of());
        ItemSourceReport report = new ItemSourceReport(
                Component.literal("Sources: Leather"),
                List.of(new ItemSourceRow(
                        ItemSourceType.MOB_DROP,
                        "Cow -> drops Leather",
                        List.of(new ItemSourceLink("Cow", cow))
                ))
        );
        ItemSourceListView view = new ItemSourceListView(0, 0, 240, 120);
        AtomicBoolean called = new AtomicBoolean(false);
        view.setReport(report);
        view.setEntityInfoAvailable(node -> false);
        view.setActionHandler((node, sourceAction, mouseX, mouseY) -> {
            called.set(true);
            return true;
        });

        assertFalse(view.mouseClicked(12, 25, 0));

        assertFalse(called.get());
    }

    @Test
    void reportCanRepresentPendingSourceIndexing() {
        ItemSourceReport report = new ItemSourceReport(Component.literal("Sources: Leather"), List.of(), true);

        assertTrue(report.loading());
        assertTrue(report.groupOrder().isEmpty());
    }

    @Test
    void reportCanExplainWhyNoSourcesAreVisible() {
        ItemSourceReport report = new ItemSourceReport(
                Component.literal("Sources: Leather"),
                List.of(),
                false,
                List.of(Component.literal("Mob drop data is unavailable in multiplayer."))
        );

        assertEquals(
                List.of("Mob drop data is unavailable in multiplayer."),
                report.diagnostics().stream().map(Component::getString).toList()
        );
    }

    @Test
    void emptySourceDiagnosticsUseWrappedRenderingContract() throws Exception {
        String source = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/client/sources/ItemSourceViewProjector.java"));

        assertTrue(source.contains("wrapLines(text, diagnostic.getString(), textW)"),
                "Empty-source diagnostics should wrap inside the result panel.");
        assertFalse(source.contains("truncate(font, diagnostic.getString(), textW)"),
                "Diagnostics should not be clipped to a single truncated line.");
    }

    @Test
    void diagnosticsCanRenderAboveNonEmptySourceGroups() throws Exception {
        String source = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/client/sources/ItemSourceViewProjector.java"));

        int emptyOnlyBranch = source.indexOf("if (safeReport.groupOrder().isEmpty())");
        int groupLoop = source.indexOf("for (ItemSourceType type : safeReport.groupOrder())", emptyOnlyBranch);
        int diagnosticsBeforeGroups = source.indexOf("projectDiagnostics", emptyOnlyBranch);
        assertTrue(diagnosticsBeforeGroups > emptyOnlyBranch && diagnosticsBeforeGroups < groupLoop,
                "Diagnostics should be visible even when mob-drop rows exist but biome enrichment is missing.");
    }

    @Test
    void sourceListRenderingIsClippedToPanelBounds() throws Exception {
        String source = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/client/sources/ItemSourceListView.java"));

        assertTrue(source.contains("g.enableScissor(x, y, x + width, y + height)"),
                "Source list rows should not paint outside the panel content area.");
        assertTrue(source.contains("g.disableScissor()"),
                "Source list clipping should be restored after rendering.");
    }

    @Test
    void biomeChipsUseFullTextColumnWithoutRepeatedLabel() throws Exception {
        String source = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/client/sources/ItemSourceViewProjector.java"));

        assertFalse(source.contains("Component.literal(\"Biomes\")"),
                "Each mob card should not spend horizontal space on a repeated Biomes label.");
        assertTrue(source.contains("biomeChips(row, text, textX,"),
                "Biome chips should start at the text column to keep names readable.");
        assertFalse(source.contains("textX + 36"),
                "Biome chips should not be indented after the old Biomes label.");
    }

    @Test
    void sourceGroupHeadersReuseNormalResultGroupChrome() throws Exception {
        String source = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/client/sources/ItemSourceListView.java"));

        assertTrue(source.contains("renderGroupHeader"),
                "Source groups should have a dedicated header renderer instead of ad hoc text.");
        assertTrue(source.contains("AMITheme.GRID_GROUP_ROOT_BG"),
                "Source group headers should share the normal result group row background.");
        assertTrue(source.contains("AMITheme.ACCENT_BLUE"),
                "Source group headers should use the same accent rail as root result groups.");
        assertTrue(source.contains("groupHeaderHeight()"),
                "Source group headers should read the live AMITheme row height instead of freezing it at class load.");
        assertFalse(source.contains("GROUP_H = AMITheme.ROW_HEIGHT"),
                "AMITheme row height is mutable after config sync and should not be copied into a static final.");
    }

    @Test
    void sourceListViewDelegatesLayoutProjectionAwayFromDrawing() throws Exception {
        String source = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/client/sources/ItemSourceListView.java"));

        assertTrue(source.contains("ItemSourceViewProjector.project("),
                "Source view should turn report data into a row model before drawing.");
        for (String forbidden : List.of(
                "report.groupOrder(",
                "report.rows(",
                "private static final int CARD_H",
                "private static final int CARD_GAP",
                "private static final int BIOME_CHIP_MAX_W",
                "rowHeight(",
                "chipWidth(",
                "private HitTarget hitTargetAt",
                "record HitTarget",
                "class HitTarget",
                "new HitTarget",
                "drawY += cardH + CARD_GAP"
        )) {
            assertFalse(source.contains(forbidden),
                    "Source view should not own grouping/layout/hit-target projection logic: " + forbidden);
        }
    }

    @Test
    void sourceProjectorBuildsRowsAndHitTargetsFromReportData() {
        SearchNode cow = new SearchNode(new ResourceLocation("minecraft:cow"), NodeType.ENTITY, "Cow", 0, 0, Map.of());
        SearchNode leather = new SearchNode(new ResourceLocation("minecraft:leather"), NodeType.ITEM, "Leather", 0, 0, Map.of());
        SearchNode savanna = new SearchNode(new ResourceLocation("minecraft:savanna"), NodeType.BIOME, "Savanna", 0, 0, Map.of());
        ItemSourceReport report = new ItemSourceReport(
                Component.literal("Sources: Leather"),
                List.of(new ItemSourceRow(
                        ItemSourceType.MOB_DROP,
                        "Cow -> drops Leather -> spawns in Savanna",
                        List.of(new ItemSourceLink("Cow", cow), new ItemSourceLink("Leather", leather), new ItemSourceLink("Savanna", savanna)),
                        new ItemSourceLink("Cow", cow),
                        "drops Leather",
                        List.of(new ItemSourceLink("Savanna", savanna))
                ))
        );

        ItemSourceViewProjector.Projection projection = ItemSourceViewProjector.project(
                report, onePixelText(), 0, 0, 240, 120, 16);

        assertEquals(
                List.of(ItemSourceViewProjector.RowKind.GROUP_HEADER, ItemSourceViewProjector.RowKind.CARD),
                projection.rows().stream().map(ItemSourceViewProjector.Row::kind).toList()
        );
        ItemSourceViewProjector.CardLayout card = projection.rows().get(1).card();
        assertEquals(16, card.bounds().y());
        assertEquals(52, card.bounds().height());
        assertEquals(35, card.textX());
        assertEquals(48, card.biomeChips().get(0).bounds().y());
        assertEquals(ItemSourceViewProjector.TargetKind.OUTPUT, projection.hitTargetAt(84, 37).kind());
        assertEquals(ItemSourceViewProjector.TargetKind.BIOME, projection.hitTargetAt(74, 54).kind());
    }

    @Test
    void sourceProjectorClipsCardsThatWouldOverflowThePanel() {
        SearchNode cow = new SearchNode(new ResourceLocation("minecraft:cow"), NodeType.ENTITY, "Cow", 0, 0, Map.of());
        ItemSourceReport report = new ItemSourceReport(
                Component.literal("Sources: Leather"),
                List.of(new ItemSourceRow(
                        ItemSourceType.MOB_DROP,
                        "Cow -> drops Leather",
                        List.of(new ItemSourceLink("Cow", cow))
                ))
        );

        ItemSourceViewProjector.Projection projection = ItemSourceViewProjector.project(
                report, onePixelText(), 0, 0, 240, 40, 16);

        assertEquals(List.of(ItemSourceViewProjector.RowKind.GROUP_HEADER),
                projection.rows().stream().map(ItemSourceViewProjector.Row::kind).toList());
        assertTrue(projection.hitTargets().isEmpty());
    }

    @Test
    void emptySourceDiagnosticCopyStaysSuccinct() throws Exception {
        String lang = Files.readString(Path.of("../xplat/src/main/resources/assets/ami/lang/en_us.json"));

        for (String key : List.of(
                "ami.sources.empty",
                "ami.sources.diagnostic.no_rows",
                "ami.sources.diagnostic.target_not_found",
                "ami.sources.diagnostic.loot.server_unavailable",
                "ami.sources.diagnostic.loot.no_resources",
                "ami.sources.diagnostic.loot.no_entity_tables",
                "ami.sources.diagnostic.loot.no_edges",
                "ami.sources.diagnostic.spawn.no_biomes"
        )) {
            String value = jsonStringValue(lang, key);
            assertTrue(value.length() <= 80, key + " is too wordy: " + value);
        }
    }

    private static String jsonStringValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        assertTrue(matcher.find(), "Missing lang key " + key);
        return matcher.group(1);
    }

    private static ItemSourceViewProjector.TextMeasurer onePixelText() {
        return new ItemSourceViewProjector.TextMeasurer() {
            @Override
            public int width(String text) {
                return text == null ? 0 : text.length();
            }

            @Override
            public String plainSubstrByWidth(String text, int width) {
                if (text == null || width <= 0) return "";
                return text.substring(0, Math.min(text.length(), width));
            }
        };
    }
}
