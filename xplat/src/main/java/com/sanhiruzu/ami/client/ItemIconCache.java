package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Off-screen framebuffer icon baking is disabled in MC 26.x (Matrix3x2fStack has no Z axis,
 * RenderTarget/bindWrite APIs changed). This class is retained as a stub so call sites compile.
 * All prime/blit calls are no-ops; callers fall back to direct g.item() rendering.
 */
public class ItemIconCache {

    private static final int ICON_SIZE = 16;

    private static final Map<Identifier, Identifier> textureKeys = new HashMap<>();

    private ItemIconCache() {
    }

    public static boolean isCached(Identifier id) {
        return textureKeys.containsKey(id);
    }

    public static void primeVisible(GuiGraphicsExtractor g, Iterable<Map.Entry<Identifier, ItemStack>> visible) {
    }

    public static void primeVisible(GuiGraphicsExtractor g, Iterable<Map.Entry<Identifier, ItemStack>> visible, int maxNewEntries) {
    }

    public static void blit(GuiGraphicsExtractor g, Identifier itemId, int x, int y) {
        Identifier texKey = textureKeys.get(itemId);
        if (texKey == null) return;
        g.blit(texKey, x, y, x + ICON_SIZE, y + ICON_SIZE, 0f, 0f, 1f, 1f);
    }

    public static void invalidate() {
        Minecraft mc = Minecraft.getInstance();
        textureKeys.values().forEach(mc.getTextureManager()::release);
        textureKeys.clear();
    }
}
