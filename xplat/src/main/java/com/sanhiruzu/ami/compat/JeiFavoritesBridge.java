package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.client.favorites.FavoriteEntry;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JEI does not expose bookmark mutation/listing through the public runtime API.
 * This bridge keeps AMI favorites in sync with JEI's item and recipe bookmarks
 * while staying tolerant of JEI 15/19 internal differences.
 */
public final class JeiFavoritesBridge {
    private JeiFavoritesBridge() {
    }

    public static boolean isFavorite(ResourceLocation id) {
        return getFavoriteIds().contains(id);
    }

    public static void addFavorite(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        JeiRuntimeAccessor.withRuntime(runtime -> {
            Object bookmarkList = getBookmarkList(runtime);
            Object bookmark = createIngredientBookmark(runtime, bookmarkList, stack);
            if (bookmark != null) {
                invokeBoolean(bookmarkList, "add", bookmark);
            }
        });
    }

    public static void addFavoriteAt(ItemStack stack, int index) {
        addFavorite(stack);
    }

    public static void removeFavorite(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        JeiRuntimeAccessor.withRuntime(runtime -> {
            Object bookmarkList = getBookmarkList(runtime);
            Object bookmark = createIngredientBookmark(runtime, bookmarkList, stack);
            if (bookmark != null && invokeBoolean(bookmarkList, "remove", bookmark)) {
                return;
            }
            removeMatchingBookmarks(bookmarkList, stack);
        });
    }

    public static List<ResourceLocation> getFavoriteIds() {
        return JeiRuntimeAccessor.withRuntime(runtime -> {
            Object bookmarkList = getBookmarkList(runtime);
            Object rawList = getFieldValue(bookmarkList, "bookmarksList");
            if (!(rawList instanceof Iterable<?> bookmarks)) {
                return List.<ResourceLocation>of();
            }

            List<ResourceLocation> ids = new ArrayList<>();
            for (Object bookmark : bookmarks) {
                ItemStack stack = getBookmarkStack(bookmark);
                if (!stack.isEmpty()) {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (id != null) {
                        ids.add(id);
                    }
                }
            }
            return ids;
        }, List.of());
    }

    public static List<FavoriteEntry> getFavoriteEntries() {
        return JeiRuntimeAccessor.withRuntime(runtime -> {
            Object bookmarkList = getBookmarkList(runtime);
            Object rawList = getFieldValue(bookmarkList, "bookmarksList");
            if (!(rawList instanceof Iterable<?> bookmarks)) {
                return List.<FavoriteEntry>of();
            }

            List<FavoriteEntry> entries = new ArrayList<>();
            for (Object bookmark : bookmarks) {
                ItemStack stack = getBookmarkStack(bookmark);
                if (stack.isEmpty()) continue;

                ResourceLocation recipeId = getRecipeId(bookmark);
                FavoriteEntry entry = recipeId == null
                        ? FavoriteEntry.item(stack, "jei")
                        : FavoriteEntry.recipe(stack, recipeId, "jei");
                if (entry != null) {
                    entries.add(entry);
                }
            }
            return entries;
        }, List.of());
    }

    private static Object getBookmarkList(IJeiRuntime runtime) {
        if (runtime == null) return null;
        return getFieldValue(runtime.getBookmarkOverlay(), "bookmarkList");
    }

    private static Object createIngredientBookmark(IJeiRuntime runtime, Object bookmarkList, ItemStack stack) {
        if (runtime == null || bookmarkList == null || stack == null || stack.isEmpty()) return null;

        Optional<ITypedIngredient<ItemStack>> typed = runtime.getIngredientManager()
                .createTypedIngredient(VanillaTypes.ITEM_STACK, stack.copy());
        if (typed.isEmpty()) return null;

        Object bookmarkFactory = getFieldValue(bookmarkList, "bookmarkFactory");
        if (bookmarkFactory != null) {
            Object bookmark = invokeAny(bookmarkFactory, "create", typed.get());
            if (bookmark != null) return bookmark;
        }

        try {
            Class<?> clazz = Class.forName("mezz.jei.gui.bookmarks.IngredientBookmark");
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.getName().equals("create") || method.getParameterCount() != 2
                        || !Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                method.setAccessible(true);
                return method.invoke(null, typed.get(), runtime.getIngredientManager());
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static void removeMatchingBookmarks(Object bookmarkList, ItemStack stack) {
        if (bookmarkList == null || stack == null || stack.isEmpty()) return;
        Object rawList = getFieldValue(bookmarkList, "bookmarksList");
        if (!(rawList instanceof Iterable<?> bookmarks)) return;

        String key = FavoriteEntry.stackKey(stack);
        List<Object> matches = new ArrayList<>();
        for (Object bookmark : bookmarks) {
            ItemStack bookmarkStack = getBookmarkStack(bookmark);
            if (!bookmarkStack.isEmpty() && key.equals(FavoriteEntry.stackKey(bookmarkStack))) {
                matches.add(bookmark);
            }
        }
        for (Object match : matches) {
            invokeBoolean(bookmarkList, "remove", match);
        }
    }

    private static ItemStack getBookmarkStack(Object bookmark) {
        if (bookmark == null) return ItemStack.EMPTY;

        Object ingredient = invokeAny(bookmark, "getDisplayIngredient");
        if (ingredient == null) {
            ingredient = invokeAny(bookmark, "getIngredient");
        }
        if (ingredient instanceof ITypedIngredient<?> typedIngredient) {
            return typedIngredient.getItemStack().map(ItemStack::copy).orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    private static ResourceLocation getRecipeId(Object bookmark) {
        if (bookmark == null || !bookmark.getClass().getName().endsWith("RecipeBookmark")) {
            return null;
        }

        Object recipeUid = getFieldValue(bookmark, "recipeUid");
        if (recipeUid instanceof ResourceLocation id) {
            return id;
        }

        Object category = invokeAny(bookmark, "getRecipeCategory");
        Object recipe = invokeAny(bookmark, "getRecipe");
        Object id = invokeAny(category, "getRegistryName", recipe);
        return id instanceof ResourceLocation location ? location : null;
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

    private static Object invokeAny(Object target, String methodName, Object... args) {
        if (target == null) return null;
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    return method.invoke(target, args);
                } catch (ReflectiveOperationException ignored) {
                    return null;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static boolean invokeBoolean(Object target, String methodName, Object arg) {
        Object result = invokeAny(target, methodName, arg);
        return result instanceof Boolean b && b;
    }
}
