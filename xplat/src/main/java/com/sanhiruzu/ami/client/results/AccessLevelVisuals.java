package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.ItemFilter;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;

final class AccessLevelVisuals {
    private AccessLevelVisuals() {
    }

    static boolean hasDevOnlyMarker(SearchNode node) {
        return AmiConfig.devMode && hiddenFromNormalPlayers(node);
    }

    static boolean hiddenFromNormalPlayers(SearchNode node) {
        if (node == null) {
            return false;
        }
        String accessLevel = node.meta(SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_SURVIVAL);
        return !ItemFilter.ACCESS_SURVIVAL.equals(accessLevel)
                || "hidden".equals(node.meta(SearchNodeKeys.VISIBILITY, ""));
    }
}
