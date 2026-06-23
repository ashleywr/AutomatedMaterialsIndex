package com.sanhiruzu.ami.client;

final class RecipeViewerChromeLayout {
    private static final int SLOT_SIZE = 18;

    private RecipeViewerChromeLayout() {
    }

    static int centeredSlotX(int panelX, int panelWidth) {
        return centeredSpriteOrigin(panelX, panelWidth, SLOT_SIZE);
    }

    static int centeredSpriteOrigin(int areaOrigin, int areaSize, int spriteSize) {
        return areaOrigin + (areaSize - spriteSize) / 2;
    }
}
