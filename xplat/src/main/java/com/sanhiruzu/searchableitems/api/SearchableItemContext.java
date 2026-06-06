package com.sanhiruzu.searchableitems.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Viewer-neutral context for an item or generated item-stack result.
 */
public record SearchableItemContext(
        ResourceLocation id,
        ItemStack stack,
        @Nullable Level level
) {
    public SearchableItemContext {
        stack = stack == null ? ItemStack.EMPTY : stack.copy();
    }
}
