package com.sanhiruzu.ami.client.icon;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class IconRenderState {
    private IconRenderState() {
    }

    public static void render3dIcon(GuiGraphicsExtractor g, Runnable renderer) {
        if (renderer != null) renderer.run();
    }
}
