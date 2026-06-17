package com.sanhiruzu.ami.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayInputPolicyTest {
    @Test
    void hiddenAmiPanelRejectsNonToggleGlobalKeys() {
        assertFalse(OverlayInputPolicy.shouldDispatchGlobalAmiKeybinds(false, false));
    }

    @Test
    void hiddenAmiPanelStillAllowsToggleKey() {
        assertTrue(OverlayInputPolicy.shouldDispatchGlobalAmiKeybinds(false, true));
    }

    @Test
    void visibleAmiPanelAllowsGlobalKeys() {
        assertTrue(OverlayInputPolicy.shouldDispatchGlobalAmiKeybinds(true, false));
    }
}
