package com.sanhiruzu.searchableitems.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Viewer-neutral context for optional item-result actions.
 */
public record SearchableItemActionContext(
        Identifier id,
        String type,
        Component displayName,
        ItemStack stack,
        Map<String, String> metadata,
        boolean cheatEnabled,
        Consumer<String> tokenInject
) {
    public SearchableItemActionContext {
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
