package com.sanhiruzu.ami.client.icon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class EntityIconRenderPolicyTest {
    @Test
    void fabricHoveredEntityIconsDoNotSpin() {
        assertFalse(EntityIconRenderer.spinHoveredEntityIcons());
    }
}
