package dev.emi.emi.api.recipe;

import java.util.Comparator;

public class EmiRecipeSorting {
    public static Comparator<EmiRecipe> none() {
        return (a, b) -> 0;
    }
}
