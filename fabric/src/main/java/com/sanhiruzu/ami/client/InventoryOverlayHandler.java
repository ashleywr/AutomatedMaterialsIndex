package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;

/**
 * Fabric stub for InventoryOverlayHandler.
 * The full overlay lifecycle (event hooks, screen init, render) is implemented in later milestones.
 * Provides the public API surface required by xplat mixins and compat classes so the project compiles.
 */
public class InventoryOverlayHandler {

    public static final boolean RECIPE_VIEWER_PRESENT = false;

    private static final OverlayWidgetManager MANAGER = new OverlayWidgetManager();

    public enum RecipeBookIntercept { VANILLA, AMI_TOGGLE, PASS }

    // TODO(Milestone C+): wire Fabric screen/render events and implement real overlay state

    public static RecipeBookIntercept recipeBookIntercept() {
        return RecipeBookIntercept.PASS;
    }

    public static void handleRecipeBookToggle() {
    }

    public static boolean shouldSuppressRecipeViewerChrome() {
        return false;
    }

    public static boolean isAmiEnabled() {
        return false;
    }

    public static void setAmiEnabled(boolean enabled) {
    }

    public static void toggleAmi() {
    }

    public static void toggleAmiSuppressAll() {
    }

    public static OverlayWidgetManager getManager() {
        return MANAGER;
    }

    public static boolean isMouseOverAmiOverlay(double mouseX, double mouseY) {
        return false;
    }

    public static void refreshOverlayResults() {
    }

    public static void resetSessionState() {
    }

    public static void tickAutoIndexBootstrap() {
    }
}
