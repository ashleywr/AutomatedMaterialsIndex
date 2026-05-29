package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AmiTooltipFactsTest {

    @AfterEach
    void resetDevMode() {
        AmiConfig.devMode = false;
    }

    @Test
    void durabilityFactCallsOutLowDurability() {
        assertEquals("13 uses (low)", DurabilityTooltipFact.formatDurability("13"));
        assertEquals("1,561 uses", DurabilityTooltipFact.formatDurability("1561"));
    }

    @Test
    void shiftDetailsDoesNotPromptForModIdOnly() {
        SearchNode node = node(Map.of(SearchNodeKeys.MOD_ID, "create"));

        assertTrue(new ShiftDetailsTooltipFact().build(node).isEmpty());
    }

    private static SearchNode node(Map<String, String> metadata) {
        return new SearchNode(
                new ResourceLocation("create:cardboard_sword"),
                NodeType.ITEM,
                "Cardboard Sword",
                0,
                0,
                metadata
        );
    }
}
