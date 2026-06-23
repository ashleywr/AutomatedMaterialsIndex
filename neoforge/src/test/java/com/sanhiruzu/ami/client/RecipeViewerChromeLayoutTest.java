package com.sanhiruzu.ami.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeViewerChromeLayoutTest {

    @Test
    void centeredSlotXCentersAn18PixelSlotInsideTheOuterPanel() {
        assertEquals(9, RecipeViewerChromeLayout.centeredSlotX(0, 36));
        assertEquals(278, RecipeViewerChromeLayout.centeredSlotX(270, 34));
    }

    @Test
    void centeredSpriteOriginCentersASmallerSpriteInsideItsBadgeArea() {
        assertEquals(2, RecipeViewerChromeLayout.centeredSpriteOrigin(0, 14, 9));
        assertEquals(12, RecipeViewerChromeLayout.centeredSpriteOrigin(10, 14, 9));
    }
}
