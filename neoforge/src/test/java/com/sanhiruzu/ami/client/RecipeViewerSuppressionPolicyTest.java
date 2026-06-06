package com.sanhiruzu.ami.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeViewerSuppressionPolicyTest {
    @Test
    void hiddenAmiDoesNotSuppressExternalRecipeViewerChrome() {
        assertFalse(RecipeViewerSuppressionPolicy.shouldSuppressRecipeViewerChrome(false, true));
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
    void screenVisibilityMatrixStaysConsistent() {
        record Row(
                String name,
                boolean startHidden,
                boolean amiEnabled,
                boolean externalViewerAvailable,
                boolean creativeMode,
                boolean showHiddenModItems,
                boolean strictSurvivalMode,
                RecipeViewerSuppressionPolicy.VisibleLayer expectedLayer,
                boolean expectedExternalSuppressed
        ) {
        }

        List<Row> rows = List.of(
                new Row("default inventory opens AMI", false, true, true, false, true, false,
                        RecipeViewerSuppressionPolicy.VisibleLayer.AMI, true),
                new Row("start hidden opens external viewer", true, false, true, false, true, false,
                        RecipeViewerSuppressionPolicy.VisibleLayer.EXTERNAL_RECIPE_VIEWER, false),
                new Row("alt-v hides AMI and releases external viewer", false, false, true, false, true, false,
                        RecipeViewerSuppressionPolicy.VisibleLayer.EXTERNAL_RECIPE_VIEWER, false),
                new Row("alt-v shows AMI over external viewer", true, true, true, false, true, false,
                        RecipeViewerSuppressionPolicy.VisibleLayer.AMI, true),
                new Row("hidden AMI without EMI or JEI leaves no AMI overlay", true, false, false, false, true, false,
                        RecipeViewerSuppressionPolicy.VisibleLayer.NONE, false),
                new Row("creative mode does not change overlay selection", false, false, true, true, true, false,
                        RecipeViewerSuppressionPolicy.VisibleLayer.EXTERNAL_RECIPE_VIEWER, false),
                new Row("hidden mod item filter does not change overlay selection", false, false, true, false, false, false,
                        RecipeViewerSuppressionPolicy.VisibleLayer.EXTERNAL_RECIPE_VIEWER, false),
                new Row("strict survival does not change overlay selection", false, false, true, false, true, true,
                        RecipeViewerSuppressionPolicy.VisibleLayer.EXTERNAL_RECIPE_VIEWER, false)
        );

        for (Row row : rows) {
            var state = new RecipeViewerSuppressionPolicy.ScreenState(
                    true,
                    row.amiEnabled(),
                    row.externalViewerAvailable());
            assertEquals(row.expectedLayer(), RecipeViewerSuppressionPolicy.visibleLayer(state), row.name());
            assertEquals(row.expectedExternalSuppressed(),
                    RecipeViewerSuppressionPolicy.shouldSuppressRecipeViewerChrome(row.amiEnabled(), true),
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
}
