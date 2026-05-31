package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ToolSpeedTooltipFact implements AmiTooltipFact {
    @Override
    public List<Component> build(SearchNode entry) {
        String speed = entry.meta(SearchNodeKeys.TOOL_SPEED, "");
        if (speed.isBlank()) return List.of();
        // Raw value is a decimal like "6.0"; strip trailing ".0" for tidiness
        String display = speed.endsWith(".0") ? speed.substring(0, speed.length() - 2) : speed;
        return TooltipFactSupport.line("ami.tooltip.tool_speed", display + "×");
    }
}
