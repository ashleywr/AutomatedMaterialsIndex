package com.sanhiruzu.ami.client.results;

import com.mojang.blaze3d.platform.NativeImage;
import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.TexturedQuadBatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

final class GridCellSpriteBatchRenderer {
    static final int CELL_SIZE = 20;
    private static final int ATLAS_W = 96;
    private static final int ATLAS_H = 32;
    private static final int SLOT_U = 2;
    private static final int HOVER_U = 34;
    private static final int GOLD_U = 66;
    private static final float UV_INSET = 0.25f;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ami", "generated/grid_cell_atlas");
    private final TexturedQuadBatch batch = new TexturedQuadBatch();
    private int registeredSignature;

    void clear() {
        batch.clear();
    }

    void slot(int x, int y) {
        add(x, y, SLOT_U);
    }

    void hover(int x, int y) {
        add(x, y, HOVER_U);
    }

    void goldBorder(int x, int y) {
        add(x, y, GOLD_U);
    }

    void flush(GuiGraphics g) {
        ensureRegistered();
        batch.setTexture(TEXTURE);
        batch.flush(g);
    }

    private void add(int x, int y, int u) {
        ensureRegistered();
        batch.setTexture(TEXTURE);
        batch.add(x, y, CELL_SIZE, CELL_SIZE,
                (u + UV_INSET) / (float) ATLAS_W,
                UV_INSET / (float) ATLAS_H,
                (u + CELL_SIZE - UV_INSET) / (float) ATLAS_W,
                (CELL_SIZE - UV_INSET) / (float) ATLAS_H,
                0xFFFFFFFF);
    }

    private void ensureRegistered() {
        int signature = signature();
        if (registeredSignature == signature) {
            return;
        }

        NativeImage image = new NativeImage(ATLAS_W, ATLAS_H, true);
        paintSlot(image, SLOT_U, 0);
        paintHover(image, HOVER_U, 0);
        paintGoldBorder(image, GOLD_U, 0);
        Minecraft.getInstance().getTextureManager().register(TEXTURE, new DynamicTexture(image));
        registeredSignature = signature;
    }

    private static void paintSlot(NativeImage image, int x, int y) {
        fill(image, x, y, x + CELL_SIZE, y + CELL_SIZE, AMITheme.SLOT_BG);
        fill(image, x, y, x + CELL_SIZE, y + 1, slotEdgeDark());
        fill(image, x, y, x + 1, y + CELL_SIZE, slotEdgeDark());
        fill(image, x + 1, y + CELL_SIZE - 1, x + CELL_SIZE, y + CELL_SIZE, slotEdgeLight());
        fill(image, x + CELL_SIZE - 1, y + 1, x + CELL_SIZE, y + CELL_SIZE, slotEdgeLight());
    }

    private static void paintHover(NativeImage image, int x, int y) {
        fill(image, x, y, x + CELL_SIZE, y + CELL_SIZE, AMITheme.ENTRY_HOVER);
    }

    private static void paintGoldBorder(NativeImage image, int x, int y) {
        border(image, x, y, CELL_SIZE, CELL_SIZE, familyBorder());
    }

    private static void border(NativeImage image, int x, int y, int w, int h, int color) {
        fill(image, x, y, x + w, y + 1, color);
        fill(image, x, y + h - 1, x + w, y + h, color);
        fill(image, x, y + 1, x + 1, y + h - 1, color);
        fill(image, x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static void fill(NativeImage image, int x1, int y1, int x2, int y2, int argb) {
        if ((argb >>> 24) == 0) {
            return;
        }
        int rgba = argbToNativeRgba(argb);
        for (int y = Math.max(0, y1); y < Math.min(ATLAS_H, y2); y++) {
            for (int x = Math.max(0, x1); x < Math.min(ATLAS_W, x2); x++) {
                image.setPixelRGBA(x, y, rgba);
            }
        }
    }

    private static int signature() {
        int result = AMITheme.SLOT_BG;
        result = 31 * result + AMITheme.SLOT_EDGE_DARK;
        result = 31 * result + AMITheme.SLOT_EDGE_LIGHT;
        result = 31 * result + AMITheme.ENTRY_HOVER;
        result = 31 * result + AMITheme.GRID_GOLD_BORDER;
        return result;
    }

    private static int slotEdgeDark() {
        return translucentSlotTheme() ? capAlpha(AMITheme.SLOT_EDGE_DARK, 0x66) : AMITheme.SLOT_EDGE_DARK;
    }

    private static int slotEdgeLight() {
        return translucentSlotTheme() ? capAlpha(AMITheme.SLOT_EDGE_LIGHT, 0x28) : AMITheme.SLOT_EDGE_LIGHT;
    }

    private static int familyBorder() {
        return translucentSlotTheme() ? capAlpha(AMITheme.GRID_GOLD_BORDER, 0xAA) : AMITheme.GRID_GOLD_BORDER;
    }

    private static boolean translucentSlotTheme() {
        int alpha = (AMITheme.SLOT_BG >>> 24) & 0xFF;
        return alpha > 0 && alpha < 0x80;
    }

    private static int capAlpha(int argb, int maxAlpha) {
        int alpha = Math.min((argb >>> 24) & 0xFF, maxAlpha);
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    private static int argbToNativeRgba(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }
}
