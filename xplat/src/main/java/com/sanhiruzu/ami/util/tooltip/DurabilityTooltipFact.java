package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class DurabilityTooltipFact implements AmiTooltipFact {
    @Override
    public List<Component> build(SearchNode entry) {
        return TooltipFactSupport.line(
                "ami.tooltip.durability",
                formatDurability(entry.meta(SearchNodeKeys.MAX_DURABILITY, ""))
        );
    }

    static String formatDurability(String raw) {
        String formatted = TooltipFactSupport.formatNumber(raw, " uses");
        if (formatted.isBlank()) return "";

        long durability = TooltipFactSupport.parseLong(raw, -1L);
        if (durability > 0 && durability <= 32L) {
            return formatted + " (low)";
        }
        return formatted;
    }
}
