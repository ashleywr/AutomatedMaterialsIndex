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

    public void clear() {
        useBlockLight.clear();
        noBlockLight.clear();
        customRender.clear();
    }

    public boolean isEmpty() {
        return useBlockLight.isEmpty() && noBlockLight.isEmpty() && customRender.isEmpty();
    }

    public void add(ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, minecraft.level, null, 0);

        if (model.isCustomRenderer()) {
            customRender.add(new CustomEntry(stack, x, y));
        } else if (model.usesBlockLight()) {
            useBlockLight.add(new Entry(model, stack, x, y));
        } else {
            noBlockLight.add(new Entry(model, stack, x, y));
        }
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
            if (!noBlockLight.isEmpty()) {
                Lighting.setupForFlatItems();
                for (Entry entry : noBlockLight) {
                    renderBakedItem(g, itemRenderer, entry);
                }
                g.flush();
                Lighting.setupFor3DItems();
            }

            if (!useBlockLight.isEmpty()) {
                Lighting.setupFor3DItems();
                for (Entry entry : useBlockLight) {
                    renderBakedItem(g, itemRenderer, entry);
                }
                g.flush();
            }

            for (Entry entry : useBlockLight) {
                g.renderItemDecorations(font, entry.stack(), entry.x(), entry.y());
            }
            for (Entry entry : noBlockLight) {
                g.renderItemDecorations(font, entry.stack(), entry.x(), entry.y());
            }

            RenderSystem.disableBlend();
            for (CustomEntry entry : customRender) {
                g.renderItem(entry.stack(), entry.x(), entry.y());
                g.renderItemDecorations(font, entry.stack(), entry.x(), entry.y());
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
        poseStack.translate(entry.x() + 8.0F, entry.y() + 8.0F, 150.0F);
        poseStack.scale(16.0F, -16.0F, 16.0F);
        try {
            itemRenderer.render(
                    entry.stack(),
                    ItemDisplayContext.GUI,
                    false,
                    poseStack,
                    g.bufferSource(),
                    0xf000f0,
                    OverlayTexture.NO_OVERLAY,
                    entry.model()
            );
        } finally {
            poseStack.popPose();
        }
    }

    private record Entry(BakedModel model, ItemStack stack, int x, int y) {
    }

    private record CustomEntry(ItemStack stack, int x, int y) {
    }
}
