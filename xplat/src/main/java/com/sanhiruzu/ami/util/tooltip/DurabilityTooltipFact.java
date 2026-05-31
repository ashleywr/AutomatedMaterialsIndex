package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class DurabilityTooltipFact implements AmiTooltipFact {
    private static final long LOW_THRESHOLD = 32L;

    public static String formatDurability(String raw) {
        long durability = TooltipFactSupport.parseLong(raw, -1L);
        if (durability <= 0) {
            return "";
        }
        String formatted = TooltipFactSupport.formatNumber(raw, "");
        return durability <= LOW_THRESHOLD ? formatted + " uses (low)" : formatted + " uses";
    }

    @Override
    public List<Component> build(SearchNode entry) {
        String raw = entry.meta(SearchNodeKeys.MAX_DURABILITY, "");
        long durability = TooltipFactSupport.parseLong(raw, -1L);
        if (durability <= 0) return List.of();

        String formatted = TooltipFactSupport.formatNumber(raw, "");
        boolean low = durability <= LOW_THRESHOLD;
        ChatFormatting valueColor = low ? ChatFormatting.RED : ChatFormatting.GREEN;
        String durKey = low ? "ami.tooltip.durability_low" : "ami.tooltip.durability";

        return List.of(
            Component.translatable(durKey,
                Component.literal(formatted).withStyle(valueColor))
            .withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE))
        );
    }
}
