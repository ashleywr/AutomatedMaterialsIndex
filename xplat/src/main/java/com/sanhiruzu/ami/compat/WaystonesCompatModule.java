package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;

public final class WaystonesCompatModule implements ModCompatModule {
    @Override
    public String modId() {
        return "waystones";
    }

    @Override
    public String probeClassName() {
        return "net.blay09.mods.waystones.api.WaystonesAPI";
    }

    @Override
    public boolean handleResultClick(SearchNode node, int button) {
        if (button != 1 || node == null || node.type() != NodeType.WAYPOINT) {
            return false;
        }
        if (!"waystones".equals(node.meta(SearchNodeKeys.WAYPOINT_PROVIDER, ""))) {
            return false;
        }
        return false;
    }
}
