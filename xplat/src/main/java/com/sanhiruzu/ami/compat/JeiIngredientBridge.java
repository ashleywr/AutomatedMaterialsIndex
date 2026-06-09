package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.providers.IRecipeViewerPlugin;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reads all ingredient types from JEI's runtime and exposes them as an IRecipeViewerPlugin.
 * Registered into IngredientPluginRegistry when JEI runtime becomes available.
 * Gives AMI full parity with JEI's ingredient database without duplicating discovery logic.
 */
public class JeiIngredientBridge implements IRecipeViewerPlugin {

    private static final AtomicReference<IIngredientManager> MANAGER = new AtomicReference<>();

    static void setManager(IIngredientManager manager) {
        MANAGER.set(manager);
        AmiCore.LOGGER.info("AMI: JEI ingredient manager available — parity bridge active");
    }

    static void clearManager() {
        MANAGER.set(null);
    }

    @Override
    public void registerIngredients(IIngredientRegistration registration) {
        IIngredientManager manager = MANAGER.get();
        if (manager == null) return;

        for (mezz.jei.api.ingredients.IIngredientType<?> rawType : manager.getRegisteredIngredientTypes()) {
            @SuppressWarnings("unchecked")
            mezz.jei.api.ingredients.IIngredientType<Object> type =
                    (mezz.jei.api.ingredients.IIngredientType<Object>) rawType;
            indexType(manager, type, registration);
        }
    }

    private static <V> void indexType(IIngredientManager manager,
                                      mezz.jei.api.ingredients.IIngredientType<V> jeiType,
                                      IRecipeViewerPlugin.IIngredientRegistration registration) {
        Class<? extends V> cls = jeiType.getIngredientClass();
        // ItemStacks are indexed as ITEM nodes by ItemProvider; FluidStacks as FLUID nodes.
        // Skip both to avoid orphaned INGREDIENT duplicates with no category or display data.
        if (ItemStack.class.isAssignableFrom(cls)) return;
        if (cls.getName().endsWith(".FluidStack")) return;

        String typeUid = cls.getName();

        mezz.jei.api.ingredients.IIngredientHelper<V> helper;
        try {
            helper = manager.getIngredientHelper(jeiType);
        } catch (Throwable t) {
            AmiCore.LOGGER.warn("AMI: failed to get JEI ingredient helper for type {}", typeUid, t);
            return;
        }

        Collection<V> ingredients;
        try {
            ingredients = manager.getAllIngredients(jeiType);
        } catch (Throwable t) {
            AmiCore.LOGGER.warn("AMI: failed to get JEI ingredients for type {}", typeUid, t);
            return;
        }

        ResourceLocation typeId = classNameToTypeId(typeUid);
        registration.register(
                new TypeAdapter<>(jeiType, typeId),
                ingredients,
                new HelperAdapter<>(helper)
        );
    }

    // "net.minecraft.world.item.ItemStack" → jei:net/minecraft/world/item/itemstack
    private static ResourceLocation classNameToTypeId(String className) {
        String path = className.replace('.', '/').replace('$', '_').toLowerCase(Locale.ROOT);
        return ResourceLocation.fromNamespaceAndPath("jei", path);
    }

    private record TypeAdapter<V>(
            mezz.jei.api.ingredients.IIngredientType<V> jeiType,
            ResourceLocation typeId
    ) implements IRecipeViewerPlugin.IIngredientType<V> {

        @Override
        public ResourceLocation getTypeId() {
            return typeId;
        }

        @Override
        public Class<? extends V> getIngredientClass() {
            return jeiType.getIngredientClass();
        }
    }

    private record HelperAdapter<V>(
            mezz.jei.api.ingredients.IIngredientHelper<V> jeiHelper
    ) implements IRecipeViewerPlugin.IIngredientHelper<V> {

        @Override
        public String getDisplayName(V ingredient) {
            try {
                return jeiHelper.getDisplayName(ingredient);
            } catch (Throwable t) {
                return "";
            }
        }

        @Override
        public ResourceLocation getResourceLocation(V ingredient) {
            try {
                return jeiHelper.getResourceLocation(ingredient);
            } catch (Throwable t) {
                return ResourceLocation.fromNamespaceAndPath("unknown", "unknown");
            }
        }

        @Override
        public String getDisplayModId(V ingredient) {
            try {
                return jeiHelper.getDisplayModId(ingredient);
            } catch (Throwable t) {
                return getResourceLocation(ingredient).getNamespace();
            }
        }
    }
}
