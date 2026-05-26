package com.sanhiruzu.ami.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.sanhiruzu.ami.forge.AMI;
/**
 * Dynamically loads EMI integration using reflection to avoid module export conflicts.
 * This allows AMI to support EMI without declaring EMI packages as exports to NeoForge.
 */
public class EmiIntegrationLoader {
    private static final String EMI_BRIDGE_CLASS = "com.sanhiruzu.ami.emi.AmiEmiBridge";
    private static final String CAPTURED_RECIPE_CLASS = "com.sanhiruzu.ami.emi.CapturedRecipe";

    public static void indexEmiRecipes(
            ConcurrentMap<Item, List<com.sanhiruzu.ami.util.AmiRecipeHolder<?>>> recipesByOutput,
            ConcurrentMap<Item, List<com.sanhiruzu.ami.util.AmiRecipeHolder<?>>> recipesByInput,
            java.util.function.BiFunction<ResourceLocation, String, RecipeType<?>> getEmiCategoryType,
            java.util.function.BiConsumer<RecipeType<?>, ItemStack> setEmiCategoryIcon) {
        try {
            Class<?> emiClass = Class.forName(EMI_BRIDGE_CLASS);

            // Check if real EMI is loaded (skip if so)
            boolean realEmiLoaded = (boolean) emiClass.getMethod("isRealEmiLoaded").invoke(null);
            if (realEmiLoaded) return;

            // Load EMI plugins
            emiClass.getMethod("loadPlugins").invoke(null);

            // Get categories and recipes
            @SuppressWarnings("unchecked")
            Map<ResourceLocation, ?> categories = (Map<ResourceLocation, ?>) emiClass.getMethod("getCategories").invoke(null);
            @SuppressWarnings("unchecked")
            Map<ResourceLocation, List<?>> recipesByCat = (Map<ResourceLocation, List<?>>) emiClass.getMethod("getRecipesByCategory").invoke(null);

            if (recipesByCat.isEmpty()) return;

            // Process each category and recipe
            int idCounter = 0;
            for (var entry : recipesByCat.entrySet()) {
                ResourceLocation catId = entry.getKey();
                Object cat = categories.get(catId);
                String catName = catId.getPath();

                if (cat != null) {
                    try {
                        Object name = cat.getClass().getMethod("getName").invoke(cat);
                        catName = (String) name.getClass().getMethod("getString").invoke(name);
                    } catch (Exception ignored) {
                    }
                }

                RecipeType<?> type = getEmiCategoryType.apply(catId, catName);

                // Try to set category icon if available
                if (cat != null) {
                    try {
                        Object icon = cat.getClass().getMethod("icon").invoke(cat);
                        if (icon != null && icon.getClass().getSimpleName().equals("EmiStack")) {
                            ItemStack iconStack = (ItemStack) icon.getClass().getMethod("getItemStack").invoke(icon);
                            setEmiCategoryIcon.accept(type, iconStack);
                        }
                    } catch (Exception ignored) {
                    }
                }

                // Index recipes
                for (Object capturedObj : entry.getValue()) {
                    try {
                        // Create CapturedRecipe wrapper
                        Class<?> capturedClass = Class.forName(CAPTURED_RECIPE_CLASS);
                        Object wrapper = capturedClass.getConstructor(Object.class, RecipeType.class)
                                .newInstance(capturedObj, type);

                        // Get recipe ID
                        ResourceLocation recipeId = (ResourceLocation) capturedObj.getClass()
                                .getMethod("recipeId").invoke(capturedObj);

                        // Create recipe holder
                        @SuppressWarnings("unchecked")
                        com.sanhiruzu.ami.util.AmiRecipeHolder<?> holder = (com.sanhiruzu.ami.util.AmiRecipeHolder<?>) com.sanhiruzu.ami.util.AmiRecipeHolder.class
                                .getConstructor(ResourceLocation.class, net.minecraft.world.item.crafting.Recipe.class)
                                .newInstance(recipeId, wrapper);

                        // Get slots
                        @SuppressWarnings("unchecked")
                        List<?> slots = (List<?>) capturedObj.getClass()
                                .getMethod("slots").invoke(capturedObj);

                        // Process each slot
                        for (Object slotObj : slots) {
                            boolean isOutput = (boolean) slotObj.getClass()
                                    .getMethod("isOutput").invoke(slotObj);

                            @SuppressWarnings("unchecked")
                            List<ItemStack> alternatives = (List<ItemStack>) slotObj.getClass()
                                    .getMethod("alternatives").invoke(slotObj);

                            for (ItemStack stack : alternatives) {
                                if (!stack.isEmpty()) {
                                    Item key = stack.getItem();
                                    if (isOutput) {
                                        recipesByOutput.computeIfAbsent(key, k -> new ArrayList<>()).add(holder);
                                    } else {
                                        recipesByInput.computeIfAbsent(key, k -> new ArrayList<>()).add(holder);
                                    }
                                }
                            }
                        }
                        idCounter++;
                    } catch (Exception e) {
                        com.sanhiruzu.ami.forge.AMI.LOGGER.debug("Failed to index EMI recipe", e);
                    }
                }
            }

            com.sanhiruzu.ami.forge.AMI.LOGGER.info("AMI/EMI bridge: indexed {} captured recipes across {} categories",
                    idCounter, recipesByCat.size());

        } catch (ClassNotFoundException ignored) {
            // EMI integration not available
        } catch (Exception e) {
            com.sanhiruzu.ami.forge.AMI.LOGGER.debug("AMI/EMI bridge loading failed", e);
        }
    }
}

