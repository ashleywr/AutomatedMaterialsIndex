package com.sanhiruzu.ami.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class AmiRecipeCategoryRegistry {
    private static final Map<ResourceLocation, RecipeType<?>> EMI_CATEGORY_TYPES = new LinkedHashMap<>();
    private static final Map<RecipeType<?>, String> EMI_CATEGORY_NAMES = new HashMap<>();
    private static final Map<RecipeType<?>, ItemStack> EMI_CATEGORY_ICONS = new HashMap<>();

    private AmiRecipeCategoryRegistry() {
    }

    public static RecipeType<?> getEmiCategoryType(
            ResourceLocation categoryId,
            String categoryName,
            Function<ResourceLocation, String> typeName
    ) {
        return EMI_CATEGORY_TYPES.computeIfAbsent(categoryId, id -> {
            var type = new RecipeType<>() {
                @Override
                public String toString() {
                    return typeName.apply(id);
                }
            };
            EMI_CATEGORY_NAMES.put(type, categoryName);
            return type;
        });
    }

    public static void setEmiCategoryIcon(RecipeType<?> type, ItemStack icon) {
        if (!icon.isEmpty()) EMI_CATEGORY_ICONS.put(type, icon.copy());
    }

    public static boolean isEmiCategoryType(RecipeType<?> type) {
        return EMI_CATEGORY_NAMES.containsKey(type);
    }

    public static String getEmiCategoryName(RecipeType<?> type) {
        return EMI_CATEGORY_NAMES.get(type);
    }

    public static ItemStack getEmiCategoryIcon(RecipeType<?> type) {
        return EMI_CATEGORY_ICONS.getOrDefault(type, ItemStack.EMPTY);
    }
}
