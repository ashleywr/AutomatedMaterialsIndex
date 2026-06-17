package com.sanhiruzu.ami.client.tooltip;

import com.mojang.datafixers.util.Either;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.util.AmiTooltipComposer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class AmiResultTooltipElements {
    private AmiResultTooltipElements() {
    }

    public static List<Either<FormattedText, TooltipComponent>> buildItemTooltip(ItemStack stack, SearchNode entry) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }

        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
        if (entry != null && entry.type() == NodeType.ITEM) {
            lines.addAll(AmiTooltipComposer.buildItemTooltipFooter(entry));
            AmiTooltipComposer.appendModNameIfMissing(lines, entry);
        }

        List<Either<FormattedText, TooltipComponent>> elements = new ArrayList<>(lines.size() + 1);
        for (Component line : AmiTooltipComposer.normalizeTooltipLines(lines)) {
            elements.add(Either.left(line));
        }
        stack.getTooltipImage().ifPresent(image -> elements.add(Math.min(1, elements.size()), Either.right(image)));
        return elements;
    }
}
