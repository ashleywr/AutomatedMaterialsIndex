package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.client.discovery.AmiDiscoveryState;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class DiscoveryTooltipFact implements AmiTooltipFact {
    @Override
    public List<Component> build(SearchNode entry) {
        if (!AmiConfig.enableDiscoveryChecklist || entry == null) {
            return List.of();
        }

        String state = entry.meta(SearchNodeKeys.DISCOVERY_STATE, "");
        if (state.isBlank()) {
            return List.of();
        }

        String lineKey = lineKey(entry, state);
        if (lineKey.isBlank()) {
            return List.of();
        }

        return List.of(Component.translatable(lineKey));
    }

    private static String lineKey(SearchNode entry, String state) {
        if (entry.type() == NodeType.ITEM && !entry.meta(SearchNodeKeys.FOOD_NUTRITION, "").isBlank()) {
            return "ami.tooltip.discovery_state.food." + state;
        }
        if (entry.type() == NodeType.BIOME || entry.type() == NodeType.STRUCTURE) {
            return AmiDiscoveryState.STATE_DISCOVERED.equals(state)
                    ? "ami.tooltip.discovery_state.world.discovered"
                    : "ami.tooltip.discovery_state.world.undiscovered";
        }
        return "";
    }
}
