package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class FluidTooltipFact implements AmiTooltipFact {
    @Override
    public List<Component> build(SearchNode entry) {
        return TooltipFactSupport.line(
                "ami.tooltip.fluid_capacity",
                TooltipFactSupport.formatNumber(entry.meta(SearchNodeKeys.FLUID_CAPACITY, ""), " B"),
                ChatFormatting.AQUA
        );
    }
}
