package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.ItemFilter;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessLevelVisualsTest {
    @BeforeEach
    void setUp() {
        AmiConfig.resetToDefaults();
    }

    @AfterEach
    void tearDown() {
        AmiConfig.resetToDefaults();
    }

    @Test
    void markerRequiresDevMode() {
        SearchNode node = item(ItemFilter.ACCESS_DEV, "hidden");

        assertFalse(AccessLevelVisuals.hasDevOnlyMarker(node));

        AmiConfig.devMode = true;

        assertTrue(AccessLevelVisuals.hasDevOnlyMarker(node));
    }

    @Test
    void survivalItemsDoNotReceiveMarker() {
        AmiConfig.devMode = true;

        assertFalse(AccessLevelVisuals.hiddenFromNormalPlayers(item(ItemFilter.ACCESS_SURVIVAL, "")));
        assertFalse(AccessLevelVisuals.hasDevOnlyMarker(item(ItemFilter.ACCESS_SURVIVAL, "")));
    }

    @Test
    void hiddenSurvivalItemsReceiveMarker() {
        AmiConfig.devMode = true;

        assertTrue(AccessLevelVisuals.hiddenFromNormalPlayers(item(ItemFilter.ACCESS_SURVIVAL, "hidden")));
        assertTrue(AccessLevelVisuals.hasDevOnlyMarker(item(ItemFilter.ACCESS_SURVIVAL, "hidden")));
    }

    @Test
    void restrictedAccessLevelsReceiveMarker() {
        AmiConfig.devMode = true;

        assertTrue(AccessLevelVisuals.hasDevOnlyMarker(item(ItemFilter.ACCESS_CREATIVE, "")));
        assertTrue(AccessLevelVisuals.hasDevOnlyMarker(item(ItemFilter.ACCESS_CHEAT, "")));
        assertTrue(AccessLevelVisuals.hasDevOnlyMarker(item(ItemFilter.ACCESS_DEV, "")));
    }

    private static SearchNode item(String accessLevel, String visibility) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(SearchNodeKeys.ACCESS_LEVEL, accessLevel);
        if (!visibility.isBlank()) {
            metadata.put(SearchNodeKeys.VISIBILITY, visibility);
        }
        return new SearchNode(new Identifier("test", accessLevel + "_item"), NodeType.ITEM, "Test Item", 0, 0, metadata);
    }
}
