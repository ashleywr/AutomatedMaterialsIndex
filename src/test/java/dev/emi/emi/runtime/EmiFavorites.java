package dev.emi.emi.runtime;

import dev.emi.emi.api.stack.EmiIngredient;
import java.util.List;
import java.util.ArrayList;

public final class EmiFavorites {
    public static final List<EmiFavorite> favorites = new ArrayList<>();
    public static void addFavorite(EmiIngredient stack) {}
    public static void addFavoriteAt(EmiIngredient stack, int index) {}
    public static void removeFavorite(EmiIngredient stack) {}
}
