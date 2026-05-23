package com.sanhiruzu.ami.emi;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.EmiExclusionArea;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiStackProvider;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * EmiRegistry implementation that captures recipe layouts instead of passing them to EMI.
 */
public class AmiCaptureRegistry implements EmiRegistry {
    private final Map<ResourceLocation, EmiRecipeCategory> categories = new LinkedHashMap<>();
    private final Map<ResourceLocation, List<AmiCapturedRecipe>> recipesByCategory = new LinkedHashMap<>();
    private final Map<ResourceLocation, List<ItemStack>> workstations = new LinkedHashMap<>();
    private final List<EmiRecipe> deferred = new ArrayList<>();
    private Consumer<Consumer<EmiRecipe>> deferredConsumer;

    @Override
    public void addCategory(EmiRecipeCategory category) {
        categories.put(category.getId(), category);
    }

    @Override
    public void addWorkstation(EmiRecipeCategory category, EmiIngredient workstation) {
        List<ItemStack> items = new ArrayList<>();
        for (EmiStack s : workstation.getEmiStacks()) {
            ItemStack is = s.getItemStack();
            if (!is.isEmpty()) items.add(is);
        }
        if (!items.isEmpty()) {
            workstations.computeIfAbsent(category.getId(), k -> new ArrayList<>()).addAll(items);
        }
    }

    @Override
    public void addRecipe(EmiRecipe recipe) {
        captureRecipe(recipe);
    }

    private void captureRecipe(EmiRecipe recipe) {
        ResourceLocation catId = recipe.getCategory().getId();
        EmiRecipeCategory cat = categories.get(catId);
        if (cat == null) cat = recipe.getCategory();

        String catName = cat.getName().getString();
        ItemStack catIcon = ItemStack.EMPTY;
        if (cat.icon instanceof EmiStack emiStack) {
            catIcon = emiStack.getItemStack();
        }

        ResourceLocation recipeId = recipe.getId();
        if (recipeId == null) {
            recipeId = ResourceLocation.fromNamespaceAndPath("ami", "captured_" + System.identityHashCode(recipe));
        }

        AmiCapturedRecipe captured = new AmiCapturedRecipe(recipeId, catId, catName, catIcon);
        captured.setDisplayDimensions(recipe.getDisplayWidth(), recipe.getDisplayHeight());

        AmiCaptureWidgetHolder holder = new AmiCaptureWidgetHolder(captured,
            recipe.getDisplayWidth(), recipe.getDisplayHeight());
        recipe.addWidgets(holder);

        if (!captured.slots().isEmpty()) {
            recipesByCategory.computeIfAbsent(catId, k -> new ArrayList<>()).add(captured);
        }

        // Index by output items for lookup
        for (EmiStack output : recipe.getOutputs()) {
            ItemStack os = output.getItemStack();
            if (!os.isEmpty()) {
                workstations.computeIfAbsent(catId, k -> new ArrayList<>()).add(os);
            }
        }
    }

    @Override
    public void removeRecipes(Predicate<EmiRecipe> predicate) {
        // not needed for capture
    }

    @Override
    public void addDeferredRecipes(Consumer<Consumer<EmiRecipe>> consumer) {
        this.deferredConsumer = consumer;
    }

    @Override
    public void addEmiStack(EmiStack stack) {
        // sidebar — not capturing for recipes
    }

    @Override
    public void addEmiStackAfter(EmiStack stack, Predicate<EmiStack> predicate) {
    }

    @Override
    public void removeEmiStacks(Predicate<EmiStack> predicate) {
    }

    @Override
    public <T extends Screen> void addExclusionArea(Class<T> clazz, EmiExclusionArea<T> area) {
    }

    @Override
    public void addGenericExclusionArea(EmiExclusionArea<Screen> area) {
    }

    @Override
    public <T extends Screen> void addDragDropHandler(Class<T> clazz, EmiDragDropHandler<T> handler) {
    }

    @Override
    public void addGenericDragDropHandler(EmiDragDropHandler<Screen> handler) {
    }

    @Override
    public <T extends Screen> void addStackProvider(Class<T> clazz, EmiStackProvider<T> provider) {
    }

    @Override
    public void addGenericStackProvider(EmiStackProvider<Screen> provider) {
    }

    @Override
    public void addRecipeHandler(net.minecraft.world.inventory.MenuType<?> type, Object handler) {
    }

    @Override
    public void addGenericRecipeHandler(Object handler) {
    }

    public void processDeferred() {
        if (deferredConsumer != null) {
            deferredConsumer.accept(this::addRecipe);
        }
    }

    public Map<ResourceLocation, EmiRecipeCategory> getCategories() { return categories; }
    public Map<ResourceLocation, List<AmiCapturedRecipe>> getRecipesByCategory() { return recipesByCategory; }
    public Map<ResourceLocation, List<ItemStack>> getWorkstations() { return workstations; }
}
