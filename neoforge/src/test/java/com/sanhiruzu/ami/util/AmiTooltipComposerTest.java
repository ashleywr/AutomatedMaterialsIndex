package com.sanhiruzu.ami.util;

import com.sanhiruzu.ami.client.icon.FallbackTextRenderer;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.util.tooltip.DiscoveryTooltipFact;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
                new Identifier("ami:waypoint/test/home"),
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
    void playerTypeLabelIsSuppressedBecauseRendererProvidesSubtitle() {
        assertFalse(AmiTooltipComposer.shouldShowTypeLabel(NodeType.PLAYER));
        assertFalse(AmiTooltipComposer.shouldShowTypeLabel(NodeType.ENTITY));
        assertTrue(AmiTooltipComposer.shouldShowTypeLabel(NodeType.WAYPOINT));
    }

    @Test
    void discoveryTooltipShowsFoodChecklistStateWhenEnabled() {
        AmiConfig.enableDiscoveryChecklist = true;
        SearchNode apple = new SearchNode(
                new Identifier("minecraft:apple"),
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
                new Identifier("minecraft:apple"),
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
}
