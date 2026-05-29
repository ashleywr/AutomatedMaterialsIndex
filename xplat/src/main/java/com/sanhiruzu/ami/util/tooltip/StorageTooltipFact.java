package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.util.StorageDisplayFormatter;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class StorageTooltipFact implements AmiTooltipFact {
    @Override
    public List<Component> build(SearchNode entry) {
        return TooltipFactSupport.line(
                "ami.tooltip.storage",
                StorageDisplayFormatter.formatChestEquivalent(entry.meta(SearchNodeKeys.ESM_CAPACITY, ""), true)
        );
    }
}
