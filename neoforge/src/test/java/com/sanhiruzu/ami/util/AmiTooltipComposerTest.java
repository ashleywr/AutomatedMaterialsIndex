package com.sanhiruzu.ami.util;

import com.sanhiruzu.ami.client.icon.FallbackTextRenderer;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.util.tooltip.DiscoveryTooltipFact;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AmiTooltipComposerTest {
    @BeforeEach
    void setUp() {
        AmiConfig.resetToDefaults();
    }

    @AfterEach
    void tearDown() {
        AmiConfig.resetToDefaults();
    }

    @Test
    void modNameDetectionIgnoresFormattingAndWhitespace() {
        assertTrue(TooltipLineMatcher.containsLine(
                List.of(Component.literal("\u00A79\u00A7o  Create  ")),
                "Create"
        ));
    }

    @Test
    void modNameDetectionRequiresWholeLineMatch() {
        assertFalse(TooltipLineMatcher.containsLine(
                List.of(Component.literal("Created item")),
                "Create"
        ));
    }

    @Test
    void duplicateModNameCleanupKeepsFirstFormattedMatchOnly() {
        List<Component> lines = new java.util.ArrayList<>(List.of(
                Component.literal("Golden Carrot"),
                Component.literal("\u00A79\u00A7oMinecraft"),
                Component.literal("Fulfilling Meal"),
                Component.literal("Minecraft"),
                Component.literal("Right-click gives one")
        ));

        TooltipLineMatcher.removeDuplicateLinesMatching(lines, "Minecraft");

        assertEquals(List.of(
                "Golden Carrot",
                "\u00A79\u00A7oMinecraft",
                "Fulfilling Meal",
                "Right-click gives one"
        ), lines.stream().map(Component::getString).toList());
    }

    @Test
    void tooltipNormalizerSplitsEscapedNewlinesFromTranslatedItemText() {
        List<Component> normalized = AmiTooltipComposer.normalizeTooltipLines(List.of(Component.literal(
                "Not attuned to any material.\\nDon't see anything?\\n"
                        + "Check the Troubleshooting page in the Dictionary of Spirits!\\n"
                        + "In the \"Getting Started\" tab find the Divination Rod item."
        )));

        assertEquals(List.of(
                "Not attuned to any material.",
                "Don't see anything?",
                "Check the Troubleshooting page in the Dictionary of Spirits!",
                "In the \"Getting Started\" tab find the Divination Rod item."
        ), normalized.stream().map(Component::getString).toList());
    }

    @Test
    void tooltipNormalizerSplitsActualNewlineCharacters() {
        List<Component> normalized = AmiTooltipComposer.normalizeTooltipLines(List.of(Component.literal("Alpha\r\nBeta\rGamma")));

        assertEquals(List.of("Alpha", "Beta", "Gamma"), normalized.stream().map(Component::getString).toList());
    }

    @Test
    void tooltipNormalizerKeepsSingleLineComponentsUntouched() {
        Component original = Component.literal("Occultism");

        List<Component> normalized = AmiTooltipComposer.normalizeTooltipLines(List.of(original));

        assertEquals(1, normalized.size());
        assertSame(original, normalized.get(0));
    }

    @Test
    void tooltipNormalizerCanBeAppliedToDebugMetadataLines() {
        List<Component> normalized = AmiTooltipComposer.normalizeTooltipLines(List.of(
                Component.literal("classificationRoute input[a]\\n-> evidence_fallback")
        ));

        assertEquals(List.of(
                "classificationRoute input[a]",
                "-> evidence_fallback"
        ), normalized.stream().map(Component::getString).toList());
    }

    @Test
    void fallbackRendererDoesNotReenterTooltipComposer() {
        SearchNode waypoint = new SearchNode(
                new ResourceLocation("ami:waypoint/test/home"),
                NodeType.WAYPOINT,
                "Home",
                0,
                0,
                Map.of()
        );

        List<Component> tooltip = new FallbackTextRenderer().getTooltip(waypoint);

        assertTrue(tooltip.isEmpty());
    }

    @Test
    void mergedWaypointTooltipShowsPrimaryProviderAndContributors() {
        SearchNode waypoint = new SearchNode(
                new ResourceLocation("ami:waypoint/merged/home"),
                NodeType.WAYPOINT,
                "Home",
                0xFFFFFF,
                1,
                Map.of(
                        SearchNodeKeys.WAYPOINT_PROVIDER, "ftbchunks",
                        SearchNodeKeys.WAYPOINT_PROVIDER_LABEL, "FTB Chunks",
                        "waypointPrimaryProvider", "ftbchunks",
                        "waypointPrimaryProviderLabel", "FTB Chunks",
                        "waypointMergedProviderLabels", "FTB Chunks,JourneyMap",
                        SearchNodeKeys.WAYPOINT_DIMENSION, "minecraft:overworld",
                        SearchNodeKeys.WAYPOINT_X, "10",
                        SearchNodeKeys.WAYPOINT_Y, "64",
                        SearchNodeKeys.WAYPOINT_Z, "20",
                        "waypointDeathpoint", "true"
                )
        );

        List<Component> tooltip = new com.sanhiruzu.ami.util.tooltip.PlayerTooltipFact().build(waypoint);
        List<String> lines = tooltip.stream().map(Component::getString).toList();

        assertTrue(lines.stream().anyMatch(line -> line.contains("Primary provider")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("FTB Chunks,JourneyMap")));
    }

    @Test
    void playerTypeLabelIsSuppressedBecauseRendererProvidesSubtitle() {
        assertFalse(AmiTooltipComposer.shouldShowTypeLabel(NodeType.PLAYER));
        assertFalse(AmiTooltipComposer.shouldShowTypeLabel(NodeType.ENTITY));
        assertTrue(AmiTooltipComposer.shouldShowTypeLabel(NodeType.WAYPOINT));
    }

    @Test
    void genericRightClickActionsHintIsNotAddedToResultTooltips() throws Exception {
        String source = Files.readString(Path.of("..", "xplat", "src", "main", "java", "com", "sanhiruzu",
                "ami", "util", "AmiTooltipComposer.java"));

        assertFalse(source.contains("hintLine(\"ami.gui.hint.right_click\", \"ami.gui.hint.action.actions\")"));
    }

    @Test
    void itemTooltipFooterDoesNotAddModNameBranding() {
        String footerBody = methodBody("buildItemTooltipFooter");

        assertFalse(footerBody.contains("appendModNameIfMissing"));
        assertFalse(footerBody.contains("modDisplayName"));
        assertFalse(footerBody.contains("SearchNodeKeys.MOD_ID"));
    }

    @Test
    void discoveryTooltipShowsFoodChecklistStateWhenEnabled() {
        AmiConfig.enableDiscoveryChecklist = true;
        SearchNode apple = new SearchNode(
                new ResourceLocation("minecraft:apple"),
                NodeType.ITEM,
                "Apple",
                0,
                0,
                Map.of(
                        SearchNodeKeys.FOOD_NUTRITION, "4",
                        SearchNodeKeys.DISCOVERY_STATE, "undiscovered"
                )
        );

        List<String> lines = new DiscoveryTooltipFact().build(apple).stream().map(Component::getString).toList();

        assertTrue(lines.contains("Discovery: Untasted")
                || lines.contains("ami.tooltip.discovery_state.food.undiscovered"));
    }

    @Test
    void discoveryTooltipIsHiddenWhenFeatureIsDisabled() {
        AmiConfig.enableDiscoveryChecklist = false;
        SearchNode apple = new SearchNode(
                new ResourceLocation("minecraft:apple"),
                NodeType.ITEM,
                "Apple",
                0,
                0,
                Map.of(
                        SearchNodeKeys.FOOD_NUTRITION, "4",
                        SearchNodeKeys.DISCOVERY_STATE, "undiscovered"
                )
        );

        List<String> lines = new DiscoveryTooltipFact().build(apple).stream().map(Component::getString).toList();

        assertTrue(lines.isEmpty());
    }

    private static String methodBody(String methodName) {
        try {
            String source = Files.readString(Path.of("..", "xplat", "src", "main", "java", "com", "sanhiruzu",
                    "ami", "util", "AmiTooltipComposer.java"));
            int nameIndex = source.indexOf(methodName);
            if (nameIndex < 0) {
                throw new AssertionError("Missing method: " + methodName);
            }
            int bodyStart = source.indexOf('{', nameIndex);
            if (bodyStart < 0) {
                throw new AssertionError("Missing method body: " + methodName);
            }
            int depth = 0;
            for (int i = bodyStart; i < source.length(); i++) {
                char c = source.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return source.substring(bodyStart + 1, i);
                    }
                }
            }
            throw new AssertionError("Unclosed method body: " + methodName);
        } catch (Exception e) {
            throw new AssertionError("Unable to read AmiTooltipComposer source", e);
        }
    }
}
