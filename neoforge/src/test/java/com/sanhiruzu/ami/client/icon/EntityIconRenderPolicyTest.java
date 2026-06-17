package com.sanhiruzu.ami.client.icon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityIconRenderPolicyTest {
    @Test
    void nonHoveredAtlasMissUsesFallbackInsteadOfLiveEntityRender() {
        assertTrue(EntityIconRenderPolicy.showFallbackOnAtlasMiss(false));
    }

    @Test
    void hoveredEntityIconsRemainLiveRendered() {
        assertFalse(EntityIconRenderPolicy.showFallbackOnAtlasMiss(true));
    }
}
