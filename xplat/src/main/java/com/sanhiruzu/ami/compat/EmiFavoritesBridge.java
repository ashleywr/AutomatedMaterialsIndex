package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.client.favorites.FavoriteEntry;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.config.SidebarType;
import dev.emi.emi.runtime.EmiFavorite;
import dev.emi.emi.runtime.EmiFavorites;
import dev.emi.emi.runtime.EmiPersistentData;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.screen.WidgetGroup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Direct EMI favorites API calls — only referenced behind a ModList.isLoaded("emi") guard
 * so this class is never loaded when EMI is absent.
 */
public final class EmiFavoritesBridge {
    private EmiFavoritesBridge() {
    }

    // ResourceLocation is inaccessible in 26.x compile context; use reflection for all EMI ID ops.
    private static String emiGetIdStr(Object emiStackOrRecipe) {
        try {
            Object id = emiStackOrRecipe.getClass().getMethod("getId").invoke(emiStackOrRecipe);
            return id != null ? id.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static EmiRecipe emiGetRecipeById(Identifier id) {
        if (id == null) return null;
        try {
            var mgr = EmiApi.getRecipeManager();
            String idStr = id.toString();
            for (Method m : mgr.getClass().getMethods()) {
                if (m.getName().equals("getRecipe") && m.getParameterCount() == 1) {
                    Class<?> paramType = m.getParameterTypes()[0];
                    Object rl = paramType.getMethod("parse", String.class).invoke(null, idStr);
                    Object result = m.invoke(mgr, rl);
                    return result instanceof EmiRecipe r ? r : null;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static boolean isFavorite(Identifier id) {
        String idStr = id.toString();
        for (EmiFavorite favorite : EmiFavorites.favorites) {
            EmiIngredient stack = favorite.getStack();
            if (stack instanceof EmiStack emiStack) {
                String emiId = emiGetIdStr(emiStack);
                if (idStr.equals(emiId)) return true;
            }
        }
        return false;
    }

    public static boolean addFavorite(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        EmiFavorites.addFavorite(EmiStack.of(stack));
        boolean added = hasMatchingFavorite(EmiStack.of(stack), null);
        if (added) {
            syncFavoritesPanel();
        }
        return added;
    }

    public static boolean addFavoriteAt(ItemStack stack, int index) {
        if (stack == null || stack.isEmpty()) return false;
        EmiFavorites.removeFavorite(EmiStack.of(stack));
        if (index < 0 || index > EmiFavorites.favorites.size()) {
            EmiFavorites.addFavorite(EmiStack.of(stack));
        } else {
            EmiFavorites.addFavoriteAt(EmiStack.of(stack), index);
        }
        boolean added = hasMatchingFavorite(EmiStack.of(stack), null);
        if (added) {
            syncFavoritesPanel();
        }
        return added;
    }

    public static void removeFavorite(ItemStack stack) {
        boolean removed = EmiFavorites.removeFavorite(EmiStack.of(stack));
        if (!removed) {
            removed = removeMatchingFavorite(EmiStack.of(stack), null);
        }
        if (removed) {
            syncFavoritesPanel();
        }
    }

    public static boolean addRecipeFavorite(ItemStack stack, Identifier recipeId) {
        if (stack == null || stack.isEmpty()) return false;
        EmiRecipe recipe = emiGetRecipeById(recipeId);
        if (recipe != null) {
            EmiFavorites.addFavorite(EmiStack.of(stack), recipe);
        } else {
            EmiFavorites.addFavorite(EmiStack.of(stack));
        }
        boolean added = hasMatchingFavorite(EmiStack.of(stack), recipe);
        if (added) {
            syncFavoritesPanel();
        }
        return added;
    }

    public static void removeRecipeFavorite(ItemStack stack, Identifier recipeId) {
        EmiRecipe recipe = emiGetRecipeById(recipeId);
        if (recipe == null) {
            if (EmiFavorites.removeFavorite(EmiStack.of(stack))) {
                syncFavoritesPanel();
            }
            return;
        }
        if (removeMatchingFavorite(EmiStack.of(stack), recipe)) {
            syncFavoritesPanel();
        }
    }

    public static boolean removeHoveredFavorite() {
        var hovered = EmiApi.getHoveredStack(true);
        if (hovered == null || hovered.isEmpty()) return false;

        EmiIngredient ingredient = hovered.getStack();
        EmiRecipe recipe = hovered.getRecipeContext();
        if (recipe == null) {
            recipe = EmiApi.getRecipeContext(ingredient);
        }

        boolean removed = removeMatchingFavorite(ingredient, recipe);
        if (removed) {
            syncFavoritesPanel();
        }
        return removed;
    }

    public static boolean toggleRecipeScreenHoveredFavorite(Object recipeScreen) {
        SlotWidget slot = getHoveredRecipeSlot(recipeScreen);
        if (slot == null) return false;

        return toggleFavorite(slot.getStack(), slot.getRecipe());
    }

    private static boolean toggleFavorite(EmiIngredient ingredient, EmiRecipe recipe) {
        if (ingredient == null || ingredient.isEmpty()) return false;

        if (recipe == null) {
            recipe = EmiApi.getRecipeContext(ingredient);
        }

        boolean removed = removeMatchingFavorite(ingredient, recipe);
        if (!removed) {
            if (recipe != null) {
                EmiFavorites.addFavorite(ingredient, recipe);
            } else {
                EmiFavorites.addFavorite(ingredient);
            }
        }
        syncFavoritesPanel();
        return true;
    }

    public static List<Identifier> getFavoriteIds() {
        List<Identifier> result = new ArrayList<>();
        for (EmiFavorite favorite : EmiFavorites.favorites) {
            EmiIngredient stack = favorite.getStack();
            if (stack instanceof EmiStack emiStack) {
                String idStr = emiGetIdStr(emiStack);
                if (idStr != null) {
                    Identifier parsed = Identifier.tryParse(idStr);
                    if (parsed != null) result.add(parsed);
                }
            }
        }
        return result;
    }

    public static List<FavoriteEntry> getFavoriteEntries() {
        List<FavoriteEntry> result = new ArrayList<>();
        for (EmiFavorite favorite : EmiFavorites.favorites) {
            EmiIngredient ingredient = favorite.getStack();
            ItemStack stack = firstItemStack(ingredient);
            if (stack.isEmpty()) continue;

            EmiRecipe recipe = favorite.getRecipe();
            String recipeIdStr = emiGetIdStr(recipe);
            Identifier recipeId = recipeIdStr == null ? null : Identifier.tryParse(recipeIdStr);
            FavoriteEntry entry = recipeId == null
                    ? FavoriteEntry.item(stack, "emi")
                    : FavoriteEntry.recipe(stack, recipeId, "emi");
            if (entry != null) {
                result.add(entry);
            }
        }
        return result;
    }

    private static ItemStack firstItemStack(EmiIngredient ingredient) {
        if (ingredient instanceof EmiStack emiStack) {
            return emiStack.getItemStack().copy();
        }
        for (EmiStack stack : ingredient.getEmiStacks()) {
            ItemStack itemStack = stack.getItemStack();
            if (!itemStack.isEmpty()) {
                return itemStack.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private static SlotWidget getHoveredRecipeSlot(Object recipeScreen) {
        SlotWidget slot = getHoveredRecipeSlotFromPage(recipeScreen);
        if (slot != null) return slot;

        Object hovered = getFieldValue(recipeScreen, "hoveredWidget");
        return hovered instanceof SlotWidget hoveredSlot ? hoveredSlot : null;
    }

    private static SlotWidget getHoveredRecipeSlotFromPage(Object recipeScreen) {
        Object currentPage = getFieldValue(recipeScreen, "currentPage");
        if (!(currentPage instanceof Iterable<?> groups)) return null;

        int mouseX = EmiScreenManager.lastMouseX;
        int mouseY = EmiScreenManager.lastMouseY;
        for (Object groupObject : groups) {
            if (!(groupObject instanceof WidgetGroup group)) continue;

            int localX = mouseX - group.x;
            int localY = mouseY - group.y;
            for (dev.emi.emi.api.widget.Widget widget : group.widgets) {
                if (widget instanceof SlotWidget slot && slot.getBounds().contains(localX, localY)) {
                    return slot;
                }
            }
        }
        return null;
    }

    private static Object getFieldValue(Object target, String fieldName) {
        if (target == null) return null;
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private static boolean removeMatchingFavorite(EmiIngredient ingredient, EmiRecipe recipe) {
        ItemStack targetStack = firstItemStack(ingredient);
        if (targetStack.isEmpty()) return false;

        String targetRecipeIdStr = emiGetIdStr(recipe);
        Identifier targetRecipeId = targetRecipeIdStr == null ? null : Identifier.tryParse(targetRecipeIdStr);
        String targetStackKey = FavoriteEntry.stackKey(targetStack);
        return EmiFavorites.favorites.removeIf(favorite -> favoriteMatches(favorite, targetRecipeId, targetStackKey));
    }

    private static boolean hasMatchingFavorite(EmiIngredient ingredient, EmiRecipe recipe) {
        ItemStack targetStack = firstItemStack(ingredient);
        if (targetStack.isEmpty()) return false;

        String targetRecipeIdStr2 = emiGetIdStr(recipe);
        Identifier targetRecipeId = targetRecipeIdStr2 == null ? null : Identifier.tryParse(targetRecipeIdStr2);
        String targetStackKey = FavoriteEntry.stackKey(targetStack);
        for (EmiFavorite favorite : EmiFavorites.favorites) {
            if (favoriteMatches(favorite, targetRecipeId, targetStackKey)) {
                return true;
            }
        }
        return false;
    }

    private static void syncFavoritesPanel() {
        EmiPersistentData.save();
        EmiScreenManager.repopulatePanels(SidebarType.FAVORITES);
        RecipeViewerStateSync.favoritesChanged();
    }

    private static boolean favoriteMatches(EmiFavorite favorite, Identifier targetRecipeId, String targetStackKey) {
            EmiRecipe favoriteRecipe = favorite.getRecipe();
            String favRecipeIdStr = emiGetIdStr(favoriteRecipe);
            Identifier favoriteRecipeId = favRecipeIdStr == null ? null : Identifier.tryParse(favRecipeIdStr);
            if (targetRecipeId == null != (favoriteRecipeId == null)) {
                return false;
            }
            if (targetRecipeId != null && !targetRecipeId.equals(favoriteRecipeId)) {
                return false;
            }
            return targetStackKey.equals(FavoriteEntry.stackKey(firstItemStack(favorite.getStack())));
    }
}
