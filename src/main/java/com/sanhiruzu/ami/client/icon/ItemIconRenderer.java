package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.client.ItemIconCache;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemIconRenderer implements IIconRenderer {

    /** 
     * Stacks for synthetic nodes (potions, enchanted books) registered during indexing.
     * These are NOT cleared on invalidate because they cannot be recovered from the registry.
     */
    private static final Map<ResourceLocation, ItemStack> persistentStacks = new HashMap<>();
    
    /** Lazy cache for regular items; cleared on resource reload. */
    private static final Map<ResourceLocation, ItemStack> lazyCache = new HashMap<>();

    @Override
    public void render(GuiGraphics g, SearchNode node, int x, int y, int size) {
        ItemStack stack = resolveStack(node.id());
        if (stack.isEmpty()) {
            FallbackTextRenderer.renderFallback(g, node, x, y, size);
            return;
        }

        if (size == 16) {
            if (ItemIconCache.isCached(node.id())) {
                ItemIconCache.blit(g, node.id(), x, y);
            } else {
                g.renderItem(stack, x, y);
            }
            return;
        }

        // Scale item rendering to requested size
        var poses = g.pose();
        poses.pushPose();
        poses.translate(x, y, 0);
        float s = size / 16f;
        poses.scale(s, s, 1f);
        g.renderItem(stack, 0, 0);
        poses.popPose();
    }

    /** Pre-register a custom ItemStack for a synthetic node id (e.g. subtype nodes). */
    public static void registerStack(ResourceLocation id, ItemStack stack) {
        persistentStacks.put(id, stack.copy());
    }

    public static ItemStack resolveStack(ResourceLocation id) {
        ItemStack persistent = persistentStacks.get(id);
        if (persistent != null) return persistent;

        return lazyCache.computeIfAbsent(id,
                k -> BuiltInRegistries.ITEM.getOptional(k).map(ItemStack::new).orElse(ItemStack.EMPTY));
    }

    @Override
    public List<Component> getTooltip(SearchNode node) {
        // Caller should use ItemStack tooltip for richer item data; return null to signal that.
        return null;
    }

    @Override
    public void invalidate() {
        lazyCache.clear();
    }

    /** Clear synthetic stacks; called when the entire index is being rebuilt. */
    public static void clearPersistent() {
        persistentStacks.clear();
    }
}
