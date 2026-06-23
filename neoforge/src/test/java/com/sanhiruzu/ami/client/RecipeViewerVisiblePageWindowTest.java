package com.sanhiruzu.ami.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeViewerVisiblePageWindowTest {

    @Test
    void computeUsesVisibleEntryCountForTheFirstPage() {
        assertEquals(
                new RecipeViewerVisiblePageWindow.Window(0, 0, 2, 2),
                RecipeViewerVisiblePageWindow.compute(0, 2, 3));
    }

    @Test
    void computeClampsRequestedPageIntoTheVisibleRange() {
        assertEquals(
                new RecipeViewerVisiblePageWindow.Window(1, 2, 3, 2),
                RecipeViewerVisiblePageWindow.compute(9, 2, 3));
    }

    @Test
    void computeReturnsASingleEmptyPageWhenNoVisibleEntriesRemain() {
        assertEquals(
                new RecipeViewerVisiblePageWindow.Window(0, 0, 0, 1),
                RecipeViewerVisiblePageWindow.compute(4, 2, 0));
    }
}
