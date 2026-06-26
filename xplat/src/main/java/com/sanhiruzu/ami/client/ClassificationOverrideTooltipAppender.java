package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.index.ClassificationOverride;
import com.sanhiruzu.ami.index.ClassificationOverrides;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ClassificationOverrideTooltipAppender {

    private ClassificationOverrideTooltipAppender() {}

    public static void appendTo(ItemStack stack, List<Component> tooltip) {
        if (stack == null || stack.isEmpty()) return;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return;
        ClassificationOverride ov = ClassificationOverrides.forItem(id).orElse(null);
        if (ov == null) return;
        for (String line : ov.tooltipLines()) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
    }
}
