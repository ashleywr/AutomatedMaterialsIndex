package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI-style same-frame item batching. This avoids the framebuffer thumbnail
 * cache while still reducing per-icon lighting switches and buffer flushes.
 */
public final class ItemIconBatchRenderer {
    private final List<Entry> useBlockLight = new ArrayList<>();
    private final List<Entry> noBlockLight = new ArrayList<>();
    private final List<CustomEntry> customRender = new ArrayList<>();
    private int useBlockLightCount;
    private int noBlockLightCount;
    private int customRenderCount;

    public void clear() {
        useBlockLightCount = 0;
        noBlockLightCount = 0;
        customRenderCount = 0;
    }

    public boolean isEmpty() {
        return useBlockLightCount == 0 && noBlockLightCount == 0 && customRenderCount == 0;
    }

    public void add(ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, minecraft.level, null, 0);

        if (model.isCustomRenderer()) {
            addCustomEntry(stack, x, y);
        } else if (model.usesBlockLight()) {
            addEntry(useBlockLight, useBlockLightCount++, model, stack, x, y);
        } else {
            addEntry(noBlockLight, noBlockLightCount++, model, stack, x, y);
        }
    }

    private static void addEntry(List<Entry> entries, int index, BakedModel model, ItemStack stack, int x, int y) {
        Entry entry;
        if (index < entries.size()) {
            entry = entries.get(index);
        } else {
            entry = new Entry();
            entries.add(entry);
        }
        entry.set(model, stack, x, y);
    }

    private void addCustomEntry(ItemStack stack, int x, int y) {
        CustomEntry entry;
        if (customRenderCount < customRender.size()) {
            entry = customRender.get(customRenderCount);
        } else {
            entry = new CustomEntry();
            customRender.add(entry);
        }
        customRenderCount++;
        entry.set(stack, x, y);
    }

    public void render(GuiGraphics g) {
        if (isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        Font font = minecraft.font;
        RenderStateSnapshot state = RenderStateSnapshot.capture();
        try {
            if (noBlockLightCount > 0) {
                Lighting.setupForFlatItems();
                for (int i = 0; i < noBlockLightCount; i++) {
                    Entry entry = noBlockLight.get(i);
                    renderBakedItem(g, itemRenderer, entry);
                }
                g.flush();
                Lighting.setupFor3DItems();
            }

            if (useBlockLightCount > 0) {
                Lighting.setupFor3DItems();
                for (int i = 0; i < useBlockLightCount; i++) {
                    Entry entry = useBlockLight.get(i);
                    renderBakedItem(g, itemRenderer, entry);
                }
                g.flush();
            }

            for (int i = 0; i < useBlockLightCount; i++) {
                Entry entry = useBlockLight.get(i);
                g.renderItemDecorations(font, entry.stack, entry.x, entry.y);
            }
            for (int i = 0; i < noBlockLightCount; i++) {
                Entry entry = noBlockLight.get(i);
                g.renderItemDecorations(font, entry.stack, entry.x, entry.y);
            }

            RenderSystem.disableBlend();
            for (int i = 0; i < customRenderCount; i++) {
                CustomEntry entry = customRender.get(i);
                g.renderItem(entry.stack, entry.x, entry.y);
                g.renderItemDecorations(font, entry.stack, entry.x, entry.y);
                RenderSystem.disableBlend();
            }
        } finally {
            Lighting.setupFor3DItems();
            state.restore();
            clear();
        }
    }

    private static void renderBakedItem(GuiGraphics g, ItemRenderer itemRenderer, Entry entry) {
        PoseStack poseStack = g.pose();
        poseStack.pushPose();
        poseStack.translate(entry.x + 8.0F, entry.y + 8.0F, 150.0F);
        poseStack.scale(16.0F, -16.0F, 16.0F);
        try {
            itemRenderer.render(
                    entry.stack,
                    ItemDisplayContext.GUI,
                    false,
                    poseStack,
                    g.bufferSource(),
                    0xf000f0,
                    OverlayTexture.NO_OVERLAY,
                    entry.model
            );
        } finally {
            poseStack.popPose();
        }
    }

    private static final class Entry {
        private BakedModel model;
        private ItemStack stack = ItemStack.EMPTY;
        private int x;
        private int y;

        private void set(BakedModel model, ItemStack stack, int x, int y) {
            this.model = model;
            this.stack = stack;
            this.x = x;
            this.y = y;
        }
    }

    private static final class CustomEntry {
        private ItemStack stack = ItemStack.EMPTY;
        private int x;
        private int y;

        private void set(ItemStack stack, int x, int y) {
            this.stack = stack;
            this.x = x;
            this.y = y;
        }
    }
}
