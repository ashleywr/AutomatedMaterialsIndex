package com.sanhiruzu.ami.client.icon;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sanhiruzu.ami.client.RenderStateSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.GL11;

/**
 * Isolates 3D icon rendering from AMI's 2D painter order.
 *
 * Promise: 3D icon renderers may use depth while drawing, but must leave no
 * depth-buffer residue that can occlude later 2D overlays such as context menus.
 *
 * When rendering many 3D icons in a single pass, wrap them with
 * {@link #begin3dBatch}/{@link #end3dBatch} so the GL setup and flush happen
 * once for the entire group instead of once per icon.
 */
public final class IconRenderState {
    private IconRenderState() {
    }

    private static boolean in3dBatch = false;
    private static RenderStateSnapshot batchState = null;

    /**
     * Enters a shared 3D render context. All {@link #render3dIcon} calls between
     * this and the matching {@link #end3dBatch} share one GL state setup and one
     * pair of flushes instead of one per icon. Must be closed with {@link #end3dBatch}.
     */
    public static void begin3dBatch(GuiGraphics g) {
        if (in3dBatch) return;
        in3dBatch = true;
        batchState = RenderStateSnapshot.capture();
        g.flush();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    /** Closes a batch opened by {@link #begin3dBatch}. */
    public static void end3dBatch(GuiGraphics g) {
        if (!in3dBatch) return;
        in3dBatch = false;
        g.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        if (batchState != null) {
            batchState.restore();
            batchState = null;
        }
    }

    public static void render3dIcon(GuiGraphics g, Runnable renderer) {
        if (g == null || renderer == null) return;

        if (in3dBatch) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            renderer.run();
            return;
        }

        RenderStateSnapshot state = RenderStateSnapshot.capture();
        g.flush();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        try {
            renderer.run();
            g.flush();
        } finally {
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            state.restore();
        }
    }
}
