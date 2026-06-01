package com.sanhiruzu.ami.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Stable item-result context passed to AMI plugin menu contributors.
 */
public record AmiItemContext(
        ResourceLocation id,
        String type,
        Component displayName,
        ItemStack stack,
        Map<String, String> metadata,
        boolean cheatEnabled,
        Consumer<String> tokenInject
) {
    public AmiItemContext {
        stack = stack == null ? ItemStack.EMPTY : stack.copy();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public boolean isItem() {
        return "ITEM".equals(type);
    }

    public String meta(String key, String fallback) {
        if (key == null) {
            return fallback;
        }
        return metadata.getOrDefault(key, fallback);
    }
}
