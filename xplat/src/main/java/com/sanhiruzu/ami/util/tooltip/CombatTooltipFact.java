package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class CombatTooltipFact implements AmiTooltipFact {
    @Override
    public List<Component> build(SearchNode entry) {
        List<Component> lines = new ArrayList<>();
        lines.addAll(TooltipFactSupport.line(
                "ami.tooltip.damage",
                TooltipFactSupport.firstNonBlank(
                        entry.meta(SearchNodeKeys.ATTACK_DAMAGE, ""),
                        entry.meta(SearchNodeKeys.ENTITY_ATTACK_DAMAGE, "")
                )
        ));
        lines.addAll(TooltipFactSupport.line("ami.tooltip.dps", entry.meta(SearchNodeKeys.DPS, "")));
        return lines;
    }
}
