package com.sanhiruzu.ami.client.icon;

final class EntityIconRenderPolicy {
    private EntityIconRenderPolicy() {
    }

    static boolean showFallbackOnAtlasMiss(boolean hovered) {
        return !hovered;
    }
}
