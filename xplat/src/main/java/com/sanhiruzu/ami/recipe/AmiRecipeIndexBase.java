package com.sanhiruzu.ami.recipe;

import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AmiRecipeIndexBase {
    private final ConcurrentMap<Item, List<AmiRecipeHolder<?>>> recipesByOutput = new ConcurrentHashMap<>();
    private final ConcurrentMap<Item, List<AmiRecipeHolder<?>>> recipesByInput = new ConcurrentHashMap<>();
    private volatile boolean built;

    public boolean isBuilt() {
        return built;
    }

    protected void beginRebuild() {
        recipesByOutput.clear();
        recipesByInput.clear();
        built = false;
    }

    protected void finishRebuild() {
        built = true;
    }

    protected void addOutput(Item item, AmiRecipeHolder<?> holder) {
        recipesByOutput.computeIfAbsent(item, k -> new ArrayList<>()).add(holder);
    }

    protected void addInput(Item item, AmiRecipeHolder<?> holder) {
        recipesByInput.computeIfAbsent(item, k -> new ArrayList<>()).add(holder);
    }

    protected void addOutput(ItemStack stack, AmiRecipeHolder<?> holder) {
        if (!stack.isEmpty()) addOutput(stack.getItem(), holder);
    }

    protected void addInput(ItemStack stack, AmiRecipeHolder<?> holder) {
        if (!stack.isEmpty()) addInput(stack.getItem(), holder);
    }

    protected void addIngredientInputs(Ingredient ingredient, AmiRecipeHolder<?> holder) {
        for (ItemStack stack : ingredient.getItems()) {
            addInput(stack, holder);
        }
    }

    public List<AmiRecipeHolder<?>> getRecipesFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        List<AmiRecipeHolder<?>> list = recipesByOutput.get(stack.getItem());
        return list == null ? List.of() : List.copyOf(list);
    }

    public List<AmiRecipeHolder<?>> getUsesFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        List<AmiRecipeHolder<?>> list = recipesByInput.get(stack.getItem());
        return list == null ? List.of() : List.copyOf(list);
    }

    public int recipeCount() {
        return recipesByOutput.values().stream().mapToInt(List::size).sum();
    }

    public boolean hasRecipe(Item item) {
        return recipesByOutput.containsKey(item);
    }

    public Set<Item> getAllOutputItems() {
        return Set.copyOf(recipesByOutput.keySet());
    }

    @SuppressWarnings("unchecked")
    public <T extends Recipe<?>> List<AmiRecipeHolder<T>> getAllRecipesOfType(RecipeType<T> type) {
        List<AmiRecipeHolder<T>> result = new ArrayList<>();
        Set<ResourceLocation> seen = new HashSet<>();
        for (List<AmiRecipeHolder<?>> holders : recipesByOutput.values()) {
            for (AmiRecipeHolder<?> holder : holders) {
                if (holder.value().getType() == type && seen.add(holder.id())) {
                    result.add((AmiRecipeHolder<T>) holder);
                }
            }
        }
        return result;
    }

    public List<AmiRecipeHolder<?>> getRecipesByType(ItemStack stack) {
        return deduplicateByType(getRecipesFor(stack));
    }

    public List<AmiRecipeHolder<?>> getUsesByType(ItemStack stack) {
        return deduplicateByType(getUsesFor(stack));
    }

    private <T extends AmiRecipeHolder<?>> List<T> deduplicateByType(List<T> recipes) {
        if (recipes.size() <= 1) return recipes;
        Map<RecipeType<?>, T> unique = new LinkedHashMap<>();
        for (T holder : recipes) {
            unique.putIfAbsent(holder.value().getType(), holder);
        }
        return new ArrayList<>(unique.values());
    }
}
