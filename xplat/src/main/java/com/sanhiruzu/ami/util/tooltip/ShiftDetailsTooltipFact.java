package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class ShiftDetailsTooltipFact implements AmiTooltipFact {
    private static boolean hasShiftOnlyDetails(SearchNode entry) {
        return !entry.meta(SearchNodeKeys.CREATIVE_TAB_LABEL, "").isEmpty()
                || !entry.meta(SearchNodeKeys.ACCESS_LEVEL, "").isEmpty()
                || !entry.meta(SearchNodeKeys.OBTAINABILITY, "").isEmpty();
    }

    private static String formatObtainability(String raw) {
        return switch (raw) {
            case "no_recipe" -> Component.translatable("ami.tooltip.obtainability.no_recipe").getString();
            default -> "";
        };
    }

    private static String formatAccessLevel(String raw) {
        return switch (raw) {
            case "survival" -> Component.translatable("ami.tooltip.access.survival").getString();
            case "creative" -> Component.translatable("ami.tooltip.access.creative").getString();
            case "cheat" -> Component.translatable("ami.tooltip.access.cheat").getString();
            case "dev" -> AmiConfig.devMode ? Component.translatable("ami.tooltip.access.dev").getString() : "";
            default -> "";
        };
    }

    @Override
    public List<Component> build(SearchNode entry) {
        if (Screen.hasShiftDown()) {
            List<Component> lines = new ArrayList<>();
            lines.addAll(TooltipFactSupport.line("ami.tooltip.registry_id", entry.id().toString()));
            lines.addAll(TooltipFactSupport.line("ami.tooltip.creative_tab", entry.meta(SearchNodeKeys.CREATIVE_TAB_LABEL, "")));
            lines.addAll(TooltipFactSupport.line("ami.tooltip.obtainability", formatObtainability(entry.meta(SearchNodeKeys.OBTAINABILITY, ""))));
            lines.addAll(TooltipFactSupport.line("ami.tooltip.access", formatAccessLevel(entry.meta(SearchNodeKeys.ACCESS_LEVEL, ""))));
            return lines;
        }

        if (hasShiftOnlyDetails(entry)) {
            return TooltipFactSupport.message("ami.tooltip.shift_for_details");
        }

        return List.of();
    }
}
