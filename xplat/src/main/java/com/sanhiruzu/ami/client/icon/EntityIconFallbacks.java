package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.client.overlay.OverlayLayers;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Shared failure policy for entity icons. Keep this xplat so Forge and NeoForge
 * agree on fallback order: spawn egg, known proxy item, then visible error icon.
 */
public final class EntityIconFallbacks {
    private static final Map<ResourceLocation, Optional<ItemStack>> FALLBACK_ITEM_CACHE = new HashMap<>();

    private EntityIconFallbacks() {
    }

    static ResourceLocation proxyItemId(ResourceLocation entityId) {
        // experience_orb has no item form; proxy to experience_bottle for the icon.
        if ("experience_orb".equals(entityId.getPath())) {
            return Services.PLATFORM.rl("minecraft", "experience_bottle");
        }
        return null;
    }

    static void renderFailure(GuiGraphics g, ResourceLocation entityId, int x, int y, int size) {
        Optional<ItemStack> fallback = fallbackItem(entityId);
        if (fallback.isPresent()) {
            renderItem(g, fallback.get(), x, y, size);
            return;
        }
        renderErrorIcon(g, x, y, size);
    }

    static void clear() {
        FALLBACK_ITEM_CACHE.clear();
    }

    private static Optional<ItemStack> fallbackItem(ResourceLocation entityId) {
        return FALLBACK_ITEM_CACHE.computeIfAbsent(entityId, EntityIconFallbacks::resolveFallbackItem);
    }

    private static Optional<ItemStack> resolveFallbackItem(ResourceLocation entityId) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof SpawnEggItem egg)) {
                continue;
            }
            ItemStack stack = new ItemStack(egg);
            ResourceLocation eggEntityId = Services.PLATFORM.getSpawnEggEntityTypeId(egg, stack);
            if (entityId.equals(eggEntityId)) {
                return Optional.of(stack);
            }
        }
        ResourceLocation proxyId = proxyItemId(entityId);
        if (proxyId != null) {
            ItemStack proxy = BuiltInRegistries.ITEM.getOptional(proxyId).map(ItemStack::new).orElse(ItemStack.EMPTY);
            if (!proxy.isEmpty()) {
                return Optional.of(proxy);
            }
        }
        return Optional.empty();
    }

    private static void renderItem(GuiGraphics g, ItemStack stack, int x, int y, int size) {
        IconRenderState.render3dIcon(g, () -> {
            g.pose().pushPose();
            try {
                g.pose().translate(x + size / 2.0, y + size / 2.0, OverlayLayers.SCREEN);
                float scale = size / 16.0f;
                g.pose().scale(scale, scale, 1f);
                g.renderItem(stack, -8, -8);
            } finally {
                g.pose().popPose();
            }
        });
    }

    private static void renderErrorIcon(GuiGraphics g, int x, int y, int size) {
        g.fill(x, y, x + size, y + size, 0xFF4A0000);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF8B0000);
        var font = Minecraft.getInstance().font;
        String marker = "!";
        int textX = x + (size - font.width(marker)) / 2;
        int textY = y + (size - font.lineHeight) / 2;
        g.drawString(font, marker, textX, textY, 0xFFFFDDDD, false);
    }
}
