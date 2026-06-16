package com.sanhiruzu.ami.api;

import com.google.gson.JsonObject;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmiRuntimeDebugApiTest {
    @Test
    void nodeSummaryIncludesMetadataWithoutCompatFieldAllowList() throws Exception {
        SearchNode node = new SearchNode(
                new Identifier("futurecompat", "planning_tool"),
                NodeType.ITEM,
                "Planning Tool",
                0,
                0,
                Map.of(
                        SearchNodeKeys.MOD_ID, "futurecompat",
                        SearchNodeKeys.COMPAT_FAMILY, "future",
                        SearchNodeKeys.ACCESS_LEVEL, "survival",
                        "futureCompatFacts", "planner,tool",
                        "futureCompatItemKind", "planning"
                )
        );

        JsonObject summary = nodeSummary(node);
        JsonObject metadata = summary.getAsJsonObject("metadata");

        assertEquals("future", summary.get("family").getAsString());
        assertEquals("survival", summary.get("accessLevel").getAsString());
        assertEquals("planner,tool", metadata.get("futureCompatFacts").getAsString());
        assertEquals("planning", metadata.get("futureCompatItemKind").getAsString());
        assertFalse(summary.has("futureCompatFacts"));
        assertTrue(metadata.has(SearchNodeKeys.ACCESS_LEVEL));
    }

    private static JsonObject nodeSummary(SearchNode node) throws Exception {
        Method method = AmiRuntimeDebugApi.class.getDeclaredMethod("nodeSummary", SearchNode.class);
        method.setAccessible(true);
        return (JsonObject) method.invoke(null, node);
    }
}
