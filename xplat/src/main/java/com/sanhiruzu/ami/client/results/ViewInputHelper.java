package com.sanhiruzu.ami.client.results;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

final class ViewInputHelper {
    private ViewInputHelper() {}

    static boolean isTokenInjectClick(int button) {
        if (button != 1) return false;
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }
}
