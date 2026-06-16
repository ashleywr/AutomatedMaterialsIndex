package com.sanhiruzu.ami.index.providers;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

/**
 * Plugin interface for mods to register custom ingredient types with any recipe viewer.
 *
 * Annotate your implementation with @{@link RecipeViewerPlugin} for automatic discovery.
 * Mirrors the shape of JEI's IModPlugin so existing JEI plugin code translates directly.
 *
 * Example:
 * <pre>{@code
 * @RecipeViewerPlugin
 * public class MyModPlugin implements IRecipeViewerPlugin {
 *     @Override
 *     public void registerIngredients(IIngredientRegistration registration) {
 *         registration.register(MY_TYPE, MyMod.getRegistry().values(), new MyIngredientHelper());
 *     }
 * }
 * }</pre>
 */
public interface IRecipeViewerPlugin {

    default void registerIngredients(IIngredientRegistration registration) {}

    default void registerExtraIngredients(IExtraIngredientRegistration registration) {}

    /**
     * Describes an ingredient type — what class it uses and a stable type ID.
     * Mirrors JEI's IIngredientType.
     */
    interface IIngredientType<V> {
        /** Stable unique ID for this type, e.g. "mekanism:gas" */
        Identifier getTypeId();

        /** The Java class of the ingredient */
        Class<? extends V> getIngredientClass();
    }

    /**
     * Provides the viewer with the information it needs to index an ingredient.
     * Mirrors JEI's IIngredientHelper.
     */
    interface IIngredientHelper<V> {
        String getDisplayName(V ingredient);

        Identifier getResourceLocation(V ingredient);

        default String getDisplayModId(V ingredient) {
            return getResourceLocation(ingredient).getNamespace();
        }
    }

    interface IIngredientRegistration {
        <V> void register(IIngredientType<V> type, Collection<? extends V> ingredients, IIngredientHelper<V> helper);
    }

    interface IExtraIngredientRegistration {
        void addExtraItemStacks(Collection<ItemStack> stacks);

        <V> void addExtraIngredients(IIngredientType<V> type, Collection<? extends V> ingredients);
    }
}
