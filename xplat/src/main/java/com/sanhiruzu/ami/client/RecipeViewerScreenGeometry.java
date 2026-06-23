package com.sanhiruzu.ami.client;

final class RecipeViewerScreenGeometry {
    static final int VIEWPORT_MARGIN_HEIGHT = 76;
    static final int MIN_GUI_TOP = 0;
    static final int TAB_HEIGHT = 24;
    static final int HEADER_TOP_PADDING = 21;
    static final int DEFAULT_MAX_GUI_HEIGHT = 350;

    private RecipeViewerScreenGeometry() {
    }

    static Geometry compute(int screenHeight, int minimumPanelHeight) {
        int availableHeight = Math.max(0, screenHeight - VIEWPORT_MARGIN_HEIGHT);
        int maxGuiHeight = Math.max(DEFAULT_MAX_GUI_HEIGHT, minimumPanelHeight);
        int guiHeight = Math.max(minimumPanelHeight, Math.min(availableHeight, maxGuiHeight));
        int extraSpace = Math.max(0, availableHeight - guiHeight);
        int preferredTop = TAB_HEIGHT + HEADER_TOP_PADDING + extraSpace / 2;
        int guiTop = Math.max(MIN_GUI_TOP, Math.min(preferredTop, Math.max(0, screenHeight - guiHeight)));
        return new Geometry(guiHeight, guiTop);
    }

    record Geometry(int guiHeight, int guiTop) {
    }
}
