package com.sanhiruzu.ami.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeViewerScreenGeometryTest {

    @Test
    void tallScreensCapRecipeViewerHeightToJeiDefault() {
        RecipeViewerScreenGeometry.Geometry geometry = RecipeViewerScreenGeometry.compute(720, 175);

        assertEquals(350, geometry.guiHeight());
        assertEquals(192, geometry.guiTop());
    }

    @Test
    void veryShortScreensClampTopSoPanelStaysVisible() {
        RecipeViewerScreenGeometry.Geometry geometry = RecipeViewerScreenGeometry.compute(200, 175);

        assertEquals(175, geometry.guiHeight());
        assertEquals(25, geometry.guiTop());
    }
}
