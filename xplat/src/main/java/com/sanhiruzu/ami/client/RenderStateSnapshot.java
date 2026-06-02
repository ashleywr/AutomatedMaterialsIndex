package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/**
 * Small GL-state snapshot for AMI render paths that temporarily switch framebuffers
 * or render 3D models inside GUI overlays.
 */
public final class RenderStateSnapshot {
    private final boolean blendEnabled;
    private final int blendSrcRgb;
    private final int blendDstRgb;
    private final int blendSrcAlpha;
    private final int blendDstAlpha;
    private final boolean depthEnabled;
    private final boolean depthMask;
    private final int depthFunc;
    private final boolean cullEnabled;
    private final boolean scissorEnabled;
    private final int[] scissorBox = new int[4];
    private final int[] viewport = new int[4];
    private final float shaderRed;
    private final float shaderGreen;
    private final float shaderBlue;
    private final float shaderAlpha;

    private RenderStateSnapshot() {
        this.blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        this.blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        this.blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        this.blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        this.blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        this.depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        this.depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        this.depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        this.cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        this.scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (this.scissorEnabled) {
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, this.scissorBox);
        }
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, this.viewport);

        float[] shaderColor = RenderSystem.getShaderColor();
        this.shaderRed = shaderColor[0];
        this.shaderGreen = shaderColor[1];
        this.shaderBlue = shaderColor[2];
        this.shaderAlpha = shaderColor[3];
    }

    public static RenderStateSnapshot capture() {
        return new RenderStateSnapshot();
    }

    public void restore() {
        GlStateManager._viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        RenderSystem.setShaderColor(shaderRed, shaderGreen, shaderBlue, shaderAlpha);

        if (blendEnabled) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        } else {
            RenderSystem.disableBlend();
        }

        if (depthEnabled) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }
        RenderSystem.depthMask(depthMask);
        RenderSystem.depthFunc(depthFunc);

        if (cullEnabled) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }

        if (scissorEnabled) {
            RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
        } else {
            RenderSystem.disableScissor();
        }
    }
}
