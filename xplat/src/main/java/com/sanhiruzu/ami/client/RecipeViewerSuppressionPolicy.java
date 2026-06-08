package com.sanhiruzu.ami.client;

/**
 * Pure stateless policy for the recipe-viewer visibility model.
 *
 * <p>Three mutually exclusive display states:
 * <ul>
 *   <li>{@link VisibleLayer#AMI} — AMI overlay renders; external viewers are suppressed.</li>
 *   <li>{@link VisibleLayer#EXTERNAL_RECIPE_VIEWER} — AMI is hidden; external viewers (EMI, JEI)
 *       render normally. Reached by Alt-V or the recipe-book button in TOGGLE_EXTERNAL_VIEWER mode.</li>
 *   <li>{@link VisibleLayer#NONE} — Both AMI and external viewers are hidden. Reached by the
 *       recipe-book button in TOGGLE_AMI mode, or while the vanilla recipe book is open in
 *       OPEN_VANILLA_BOOK mode.</li>
 * </ul>
 *
 * <p>{@code InventoryOverlayHandler} holds the live {@link VisibleLayer} as {@code currentLayer}
 * and transitions it via its private {@code setLayer()} method. This class provides the pure
 * decision functions used by tests and the suppression query called from mixins.
 */
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
            boolean recipeBookHidesRecipeViewers,
            boolean externalRecipeViewerAvailable
    ) {
    }

    public static VisibleLayer visibleLayer(ScreenState state) {
        if (state == null || !state.amiCapableScreen()) {
            return VisibleLayer.NONE;
        }
        if (state.amiEnabled()) {
            return VisibleLayer.AMI;
        }
        if (state.recipeBookHidesRecipeViewers()) {
            return VisibleLayer.NONE;
        }
        if (state.externalRecipeViewerAvailable()) {
            return VisibleLayer.EXTERNAL_RECIPE_VIEWER;
        }
        return VisibleLayer.NONE;
    }

    public static boolean shouldSuppressRecipeViewerChrome(boolean amiEnabled, boolean currentScreenSupportsAmi) {
        return shouldSuppressRecipeViewerChrome(amiEnabled, false, currentScreenSupportsAmi);
    }

    public static boolean shouldSuppressRecipeViewerChrome(
            boolean amiEnabled,
            boolean recipeBookHidesRecipeViewers,
            boolean currentScreenSupportsAmi
    ) {
        if (!currentScreenSupportsAmi) {
            return false;
        }
        return amiEnabled || recipeBookHidesRecipeViewers;
    }
}
