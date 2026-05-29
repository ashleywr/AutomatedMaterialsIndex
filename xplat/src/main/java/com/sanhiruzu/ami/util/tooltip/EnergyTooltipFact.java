package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class EnergyTooltipFact implements AmiTooltipFact {
    @Override
    public List<Component> build(SearchNode entry) {
        java.util.ArrayList<Component> lines = new java.util.ArrayList<>();
        lines.addAll(TooltipFactSupport.line(
                "ami.tooltip.energy",
                TooltipFactSupport.formatNumber(entry.meta(SearchNodeKeys.ENERGY_CAPACITY, ""), " FE")
        ));
        lines.addAll(TooltipFactSupport.line(
                "ami.tooltip.energy_generation",
                TooltipFactSupport.formatNumber(entry.meta(SearchNodeKeys.ENERGY_GENERATION, ""), " FE/t")
        ));
        return lines;
    }
}
