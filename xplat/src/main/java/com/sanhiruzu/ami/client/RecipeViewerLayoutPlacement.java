package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.RecipeLayout;

final class RecipeViewerLayoutPlacement {
    private static final int BASE_LAYOUT_X = 24;
    private static final int OUTPUT_SLOT_SPRITE_WIDTH = 26;
    private static final int ARROW_WIDTH = 22;
    private static final int GENERIC_LAYOUT_REFERENCE_WIDTH = 108;
    private static final int MAX_GENERIC_LAYOUT_NUDGE = 8;

    private RecipeViewerLayoutPlacement() {
    }

    static int layoutOriginX(int cardX, RecipeLayout layout) {
        if (layout.backgroundTexture() != null) {
            return cardX + BASE_LAYOUT_X;
        }

        int inputRight = layout.inputs().stream()
                .mapToInt(slot -> slot.x() + 18)
                .max()
                .orElse(0);
        int clusterWidth = Math.max(
                inputRight,
                Math.max(layout.outputX() + OUTPUT_SLOT_SPRITE_WIDTH, layout.arrowX() + ARROW_WIDTH));
        int nudge = Math.min(
                MAX_GENERIC_LAYOUT_NUDGE,
                Math.max(0, (GENERIC_LAYOUT_REFERENCE_WIDTH - clusterWidth) / 2));
        return cardX + BASE_LAYOUT_X + nudge;
    }
}
