package com.sanhiruzu.ami.client;

public final class RecipeViewerSuppressionPolicy {
    private RecipeViewerSuppressionPolicy() {
    }

    public enum VisibleLayer {
        NONE,
        AMI,
        EXTERNAL_RECIPE_VIEWER
    }

    public record ScreenState(
            boolean amiCapableScreen,
            boolean amiEnabled,
            boolean externalRecipeViewerAvailable
    ) {
    }

    /**
     * Contract for inventory overlays:
     * - Alt-V and the recipe book button both flip amiEnabled on AMI-capable screens.
     * - "Start AMI Hidden" only selects the initial amiEnabled value for a new inventory session.
     * - Item visibility settings such as hidden mod items or strict survival mode only filter AMI results.
     */
    public static VisibleLayer visibleLayer(ScreenState state) {
        if (state == null || !state.amiCapableScreen()) {
            return VisibleLayer.NONE;
        }
        if (state.amiEnabled()) {
            return VisibleLayer.AMI;
        }
        if (state.externalRecipeViewerAvailable()) {
            return VisibleLayer.EXTERNAL_RECIPE_VIEWER;
        }
        return VisibleLayer.NONE;
    }

    public static boolean shouldSuppressRecipeViewerChrome(boolean amiEnabled, boolean currentScreenSupportsAmi) {
        return visibleLayer(new ScreenState(currentScreenSupportsAmi, amiEnabled, true)) == VisibleLayer.AMI;
    }
}
