package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class EnergyTooltipFact implements AmiTooltipFact {
    @Override
    public List<Component> build(SearchNode entry) {
        java.util.ArrayList<Component> lines = new java.util.ArrayList<>();
        lines.addAll(TooltipFactSupport.line(
                "ami.tooltip.energy",
                TooltipFactSupport.formatNumber(entry.meta(SearchNodeKeys.ENERGY_CAPACITY, ""), " FE"),
                ChatFormatting.RED
        ));
        lines.addAll(TooltipFactSupport.line(
                "ami.tooltip.energy_generation",
                TooltipFactSupport.formatNumber(entry.meta(SearchNodeKeys.ENERGY_GENERATION, ""), " FE/t"),
                ChatFormatting.GOLD
        ));
        lines.addAll(TooltipFactSupport.line(
                "ami.tooltip.energy_consumption",
                TooltipFactSupport.formatNumber(entry.meta(SearchNodeKeys.ENERGY_CONSUMPTION, ""), " FE/t"),
                ChatFormatting.YELLOW
        ));
        lines.addAll(TooltipFactSupport.line(
                "ami.tooltip.gregtech_eu_generation",
                TooltipFactSupport.formatNumber(entry.meta(SearchNodeKeys.GREGTECH_EU_GENERATION, ""), " EU/t"),
                ChatFormatting.GOLD
        ));
        lines.addAll(TooltipFactSupport.line(
                "ami.tooltip.gregtech_eu_consumption",
                TooltipFactSupport.formatNumber(entry.meta(SearchNodeKeys.GREGTECH_EU_CONSUMPTION, ""), " EU/t"),
                ChatFormatting.YELLOW
        ));
        lines.addAll(TooltipFactSupport.line(
                "ami.tooltip.gregtech_eu_input",
                TooltipFactSupport.formatNumber(entry.meta(SearchNodeKeys.GREGTECH_EU_INPUT, ""), " EU/t"),
                ChatFormatting.YELLOW
        ));
        lines.addAll(TooltipFactSupport.line(
                "ami.tooltip.gregtech_eu_output",
                TooltipFactSupport.formatNumber(entry.meta(SearchNodeKeys.GREGTECH_EU_OUTPUT, ""), " EU/t"),
                ChatFormatting.GOLD
        ));
        return lines;
    }
}
