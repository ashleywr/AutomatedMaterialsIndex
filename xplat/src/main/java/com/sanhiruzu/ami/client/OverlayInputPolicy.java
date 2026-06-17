package com.sanhiruzu.ami.client;

final class OverlayInputPolicy {
    private OverlayInputPolicy() {
    }

    static boolean shouldDispatchGlobalAmiKeybinds(boolean amiPanelVisible, boolean toggleViewerKey) {
        return amiPanelVisible || toggleViewerKey;
    }
}
