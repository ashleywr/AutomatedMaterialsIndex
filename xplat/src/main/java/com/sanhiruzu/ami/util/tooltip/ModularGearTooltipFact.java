package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.client.icon.EntityIconTooltipSupport;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class ModularGearTooltipFact implements AmiTooltipFact {
    @Override
    public List<Component> build(SearchNode entry) {
        List<Component> lines = new ArrayList<>();

        String family = EntityIconTooltipSupport.formatToken(entry.meta(SearchNodeKeys.MODULAR_GEAR_FAMILY, ""));
        String kind = EntityIconTooltipSupport.formatToken(entry.meta(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, ""));
        if (!family.isBlank() || !kind.isBlank()) {
            String detail = family.isBlank() ? kind : kind.isBlank() ? family : family + " / " + kind;
            lines.addAll(TooltipFactSupport.line("ami.tooltip.modular_gear", detail));
        }

        String material = EntityIconTooltipSupport.formatToken(entry.meta(SearchNodeKeys.MODULAR_GEAR_MATERIAL, ""));
        lines.addAll(TooltipFactSupport.line("ami.tooltip.modular_gear_material", material));

        String part = EntityIconTooltipSupport.formatToken(entry.meta(SearchNodeKeys.MODULAR_GEAR_PART, ""));
        lines.addAll(TooltipFactSupport.line("ami.tooltip.modular_gear_part", part));

        String facts = EntityIconTooltipSupport.formatTokenList(entry.meta(SearchNodeKeys.MODULAR_GEAR_FACTS, ""));
        lines.addAll(TooltipFactSupport.line("ami.tooltip.modular_gear_facts", facts));

        String runtimeMaterials = EntityIconTooltipSupport.formatTokenList(entry.meta(SearchNodeKeys.MODULAR_GEAR_RUNTIME_MATERIALS, ""));
        lines.addAll(TooltipFactSupport.line("ami.tooltip.modular_gear_runtime_materials", runtimeMaterials));

        String runtimeTraits = EntityIconTooltipSupport.formatTokenList(entry.meta(SearchNodeKeys.MODULAR_GEAR_RUNTIME_TRAITS, ""));
        lines.addAll(TooltipFactSupport.line("ami.tooltip.modular_gear_runtime_traits", runtimeTraits));

        lines.addAll(TooltipFactSupport.line(
                "ami.tooltip.modular_gear_runtime_stats",
                entry.meta(SearchNodeKeys.MODULAR_GEAR_RUNTIME_STATS, "")));

        return lines;
    }
}
