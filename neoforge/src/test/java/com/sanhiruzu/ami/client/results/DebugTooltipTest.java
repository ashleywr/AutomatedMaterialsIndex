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
}
