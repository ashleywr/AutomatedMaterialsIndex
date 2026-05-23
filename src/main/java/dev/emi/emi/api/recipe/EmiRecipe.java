package dev.emi.emi.api.recipe;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;

public interface EmiRecipe {
    EmiRecipeCategory getCategory();

    @Nullable ResourceLocation getId();

    List<EmiIngredient> getInputs();

    default List<EmiIngredient> getCatalysts() {
        return List.of();
    }

    List<EmiStack> getOutputs();

    int getDisplayWidth();
    int getDisplayHeight();

    void addWidgets(WidgetHolder widgets);

    default boolean supportsRecipeTree() {
        return !getInputs().isEmpty() && !getOutputs().isEmpty();
    }

    default boolean hideCraftable() {
        return false;
    }
}
