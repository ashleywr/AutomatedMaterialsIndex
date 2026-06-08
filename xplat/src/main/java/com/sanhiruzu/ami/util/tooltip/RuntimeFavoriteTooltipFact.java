package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class RuntimeFavoriteTooltipFact implements AmiTooltipFact {
    @Override
    public List<Component> build(SearchNode entry) {
        if (entry == null || !"stale".equals(entry.meta(SearchNodeKeys.RUNTIME_FAVORITE_STATE, ""))) {
            return List.of();
        }

        String key = switch (entry.type()) {
            case PLAYER -> "ami.tooltip.runtime_favorite_player_offline";
            case WAYPOINT -> "ami.tooltip.runtime_favorite_waypoint_unavailable";
            default -> "";
        };
        if (key.isBlank()) {
            return List.of();
        }
        return TooltipFactSupport.line("ami.tooltip.runtime_favorite_state", Component.translatable(key).getString());
    }
}
