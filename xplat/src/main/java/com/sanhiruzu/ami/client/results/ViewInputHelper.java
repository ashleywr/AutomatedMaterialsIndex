package com.sanhiruzu.ami.client.results;

import net.minecraft.client.gui.screens.Screen;

final class ViewInputHelper {
    private ViewInputHelper() {}

    static boolean isTokenInjectClick(int button) {
        return button == 1 && Screen.hasControlDown();
    }
}
