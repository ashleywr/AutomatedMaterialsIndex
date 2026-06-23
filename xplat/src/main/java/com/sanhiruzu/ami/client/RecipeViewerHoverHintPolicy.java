package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.SlotPosition;

import java.util.List;

final class RecipeViewerHoverHintPolicy {
    enum Hint {
        INGREDIENT_SCROLL,
        TRANSFER_BUTTON,
        OUTPUT_SHIFT_TRANSFER,
        WORKSTATION_LEFT_CLICK,
        WORKSTATION_RIGHT_CLICK
    }

    private RecipeViewerHoverHintPolicy() {
    }

    static List<Hint> ingredientHints(SlotPosition slot) {
        return slot.alternatives().size() > 1 ? List.of(Hint.INGREDIENT_SCROLL) : List.of();
    }

    static List<Hint> transferButtonHints() {
        return List.of(Hint.TRANSFER_BUTTON);
    }

    static List<Hint> outputHints(boolean recipeCanTransfer) {
        return recipeCanTransfer ? List.of(Hint.OUTPUT_SHIFT_TRANSFER) : List.of();
    }

    static List<Hint> workstationHints() {
        return List.of(Hint.WORKSTATION_LEFT_CLICK, Hint.WORKSTATION_RIGHT_CLICK);
    }
}
