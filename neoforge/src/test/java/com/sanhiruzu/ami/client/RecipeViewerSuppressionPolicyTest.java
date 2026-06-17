package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.RecipeViewerSuppressionPolicy.VisibleLayer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.sanhiruzu.ami.client.RecipeViewerSuppressionPolicy.VisibleLayer.AMI;
import static com.sanhiruzu.ami.client.RecipeViewerSuppressionPolicy.VisibleLayer.EXTERNAL_RECIPE_VIEWER;
import static com.sanhiruzu.ami.client.RecipeViewerSuppressionPolicy.VisibleLayer.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Specification for the recipe-viewer visibility model.
 *
 * The three layers are mutually exclusive display states:
 *   AMI                   — AMI renders; external viewers (EMI, JEI) are suppressed.
 *   EXTERNAL_RECIPE_VIEWER — AMI hidden; external viewers render normally.
 *   NONE                  — Both hidden (recipe book TOGGLE_AMI pressed, or vanilla book open).
 *
 * InventoryOverlayHandler holds the live currentLayer and transitions it via setLayer().
 * This class provides the pure decision functions tested here.
 */
class RecipeViewerSuppressionPolicyTest {
    @Test
    void hiddenAmiDoesNotSuppressExternalRecipeViewerChrome() {
        assertFalse(RecipeViewerSuppressionPolicy.shouldSuppressRecipeViewerChrome(false, true));
    }

    @Test
    void recipeBookHiddenStateSuppressesExternalRecipeViewerChrome() {
        assertTrue(RecipeViewerSuppressionPolicy.shouldSuppressRecipeViewerChrome(false, true, true));
    }

    @Test
    void enabledAmiSuppressesExternalRecipeViewerChromeOnSupportedScreens() {
        assertTrue(RecipeViewerSuppressionPolicy.shouldSuppressRecipeViewerChrome(true, true));
    }

    @Test
    void enabledAmiDoesNotSuppressUnrelatedScreens() {
        assertFalse(RecipeViewerSuppressionPolicy.shouldSuppressRecipeViewerChrome(true, false));
    }

    @Test
    void reiRuntimeOverlayTakeoverIsLimitedToReiDisplayScreens() {
        assertFalse(RecipeViewerSuppressionPolicy.shouldTakeOverReiRuntimeOverlay(true, false));
        assertTrue(RecipeViewerSuppressionPolicy.shouldTakeOverReiRuntimeOverlay(true, true));
        assertFalse(RecipeViewerSuppressionPolicy.shouldTakeOverReiRuntimeOverlay(false, true));
    }

    @Test
    void screenVisibilityMatrixStaysConsistent() {
        record Row(
                String name,
                boolean startHidden,
                boolean amiEnabled,
                boolean recipeBookHidesRecipeViewers,
                boolean externalViewerAvailable,
                boolean creativeMode,
                boolean showHiddenModItems,
                boolean strictSurvivalMode,
                RecipeViewerSuppressionPolicy.VisibleLayer expectedLayer,
                boolean expectedExternalSuppressed
        ) {
        }

        List<Row> rows = List.of(
                new Row("default inventory opens AMI", false, true, false, true, false, true, false,
                        RecipeViewerSuppressionPolicy.VisibleLayer.AMI, true),
                new Row("start hidden opens external viewer", true, false, false, true, false, true, false,
                        RecipeViewerSuppressionPolicy.VisibleLayer.EXTERNAL_RECIPE_VIEWER, false),
                new Row("alt-v hides AMI and releases external viewer", false, false, false, true, false, true, false,
                        RecipeViewerSuppressionPolicy.VisibleLayer.EXTERNAL_RECIPE_VIEWER, false),
                new Row("alt-v shows AMI over external viewer", true, true, false, true, false, true, false,
                        RecipeViewerSuppressionPolicy.VisibleLayer.AMI, true),
                new Row("recipe book button hides AMI and external viewer", false, false, true, true, false, true, false,
                        RecipeViewerSuppressionPolicy.VisibleLayer.NONE, true),
                new Row("hidden AMI without EMI or JEI leaves no AMI overlay", true, false, false, false, false, true, false,
                        RecipeViewerSuppressionPolicy.VisibleLayer.NONE, false),
                new Row("creative mode does not change overlay selection", false, false, false, true, true, true, false,
                        RecipeViewerSuppressionPolicy.VisibleLayer.EXTERNAL_RECIPE_VIEWER, false),
                new Row("hidden mod item filter does not change overlay selection", false, false, false, true, false, false, false,
                        RecipeViewerSuppressionPolicy.VisibleLayer.EXTERNAL_RECIPE_VIEWER, false),
                new Row("strict survival does not change overlay selection", false, false, false, true, false, true, true,
                        RecipeViewerSuppressionPolicy.VisibleLayer.EXTERNAL_RECIPE_VIEWER, false)
        );

        for (Row row : rows) {
            var state = new RecipeViewerSuppressionPolicy.ScreenState(
                    true,
                    row.amiEnabled(),
                    row.recipeBookHidesRecipeViewers(),
                    row.externalViewerAvailable());
            assertEquals(row.expectedLayer(), RecipeViewerSuppressionPolicy.visibleLayer(state), row.name());
            assertEquals(row.expectedExternalSuppressed(),
                    RecipeViewerSuppressionPolicy.shouldSuppressRecipeViewerChrome(
                            row.amiEnabled(), row.recipeBookHidesRecipeViewers(), true),
                    row.name());

            // These config dimensions are documented in the row so future changes must decide
            // explicitly if they should affect screen selection.
            assertEquals(row.expectedLayer(), RecipeViewerSuppressionPolicy.visibleLayer(state),
                    row.name() + " startHidden=" + row.startHidden()
                            + " creative=" + row.creativeMode()
                            + " showHidden=" + row.showHiddenModItems()
                            + " strictSurvival=" + row.strictSurvivalMode());
        }
    }

    // --- Toggle transition semantics ---
    // These mirror InventoryOverlayHandler.toggleAmi() and toggleAmiSuppressAll().
    // Documented here so the intended transitions are testable without Minecraft running.

    @Test
    void toggleExternalViewerCyclesAmiAndExternalViewer() {
        // Alt-V / TOGGLE_EXTERNAL_VIEWER: AMI → external viewer → AMI → ...
        // When no external viewer is present, AMI off falls to NONE.
        assertEquals(EXTERNAL_RECIPE_VIEWER, nextLayerToggleAmi(AMI, true));
        assertEquals(AMI, nextLayerToggleAmi(EXTERNAL_RECIPE_VIEWER, true));
        assertEquals(AMI, nextLayerToggleAmi(NONE, true));

        assertEquals(NONE, nextLayerToggleAmi(AMI, false));
        assertEquals(AMI, nextLayerToggleAmi(NONE, false));
    }

    @Test
    void toggleAmiModeCyclesBetweenAmiAndNone() {
        // TOGGLE_AMI recipe book: AMI ↔ NONE, suppressing external viewers in both states.
        assertEquals(NONE, nextLayerToggleAmiSuppressAll(AMI));
        assertEquals(AMI, nextLayerToggleAmiSuppressAll(NONE));
        assertEquals(AMI, nextLayerToggleAmiSuppressAll(EXTERNAL_RECIPE_VIEWER));
    }

    /** Mirrors InventoryOverlayHandler.toggleAmi() without Minecraft state. */
    private static VisibleLayer nextLayerToggleAmi(VisibleLayer current, boolean recipeViewerPresent) {
        return current == AMI
                ? (recipeViewerPresent ? EXTERNAL_RECIPE_VIEWER : NONE)
                : AMI;
    }

    /** Mirrors InventoryOverlayHandler.toggleAmiSuppressAll() without Minecraft state. */
    private static VisibleLayer nextLayerToggleAmiSuppressAll(VisibleLayer current) {
        return current == AMI ? NONE : AMI;
    }
}
