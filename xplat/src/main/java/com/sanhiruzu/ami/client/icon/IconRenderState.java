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
 */
public final class IconRenderState {
    private IconRenderState() {
    }

    public static void render3dIcon(GuiGraphics g, Runnable renderer) {
        if (g == null || renderer == null) return;

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
