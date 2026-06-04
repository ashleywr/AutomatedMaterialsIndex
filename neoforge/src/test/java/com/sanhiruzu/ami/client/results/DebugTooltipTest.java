package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DebugTooltipTest {

    private static SearchNode node() {
        return new SearchNode(
                new ResourceLocation("create:cardboard_sword"),
                NodeType.ITEM,
                "Cardboard Sword",
                0,
                0,
                Map.of(
                        SearchNodeKeys.DPS, "30.0",
                        SearchNodeKeys.MAX_DURABILITY, "13"
                )
        );
    }

    @AfterEach
    void resetDevMode() {
        AmiConfig.devMode = false;
    }

    @Test
    void debugTooltipDoesNotExposeMetadataWhenDevModeIsOff() {
        AmiConfig.devMode = false;

        String tooltip = DebugTooltip.build(node()).stream()
                .map(component -> component.getString())
                .reduce("", (left, right) -> left + "\n" + right);

        assertTrue(tooltip.contains("Cardboard Sword"));
        assertFalse(tooltip.contains("Metadata"));
        assertFalse(tooltip.contains(SearchNodeKeys.DPS));
        assertFalse(tooltip.contains("create:cardboard_sword"));
    }

    @Test
    void debugTooltipRendersClassificationTraceOutsideRawMetadata() {
        var steps = DebugTooltip.traceSteps(
                "input[quark:ancient_hedge modFamily=generic] | facts[facets=placeable shape=partial blockClass=HedgeBlock] | primary_rule:leaves: skip - predicate false | primary_rule:partial placeables[masonry/other_building]: matched",
                ""
        );

        assertTrue(steps.contains("primary_rule:leaves: skip - predicate false"));
        assertTrue(steps.contains("primary_rule:partial placeables[masonry/other_building]: matched"));
        assertTrue(DebugTooltip.isRenderedElsewhere(SearchNodeKeys.CLASSIFICATION_TRACE));
        assertTrue(DebugTooltip.isRenderedElsewhere(SearchNodeKeys.CLASSIFICATION_ROUTE_RULE));
        assertTrue(DebugTooltip.isRenderedElsewhere(SearchNodeKeys.FACETS));
        assertTrue(DebugTooltip.isRenderedElsewhere(SearchNodeKeys.BLOCK_TAGS));
        assertFalse(DebugTooltip.isRenderedElsewhere(SearchNodeKeys.DPS));
    }
}
