package com.sanhiruzu.ami.compat;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IIngredientVisibility;
import mezz.jei.api.runtime.IJeiRuntime;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class JeiRuntimeListenerBridge implements IIngredientVisibility.IListener, IIngredientManager.IIngredientListener {
    private static final JeiRuntimeListenerBridge INSTANCE = new JeiRuntimeListenerBridge();
    private static final Set<IJeiRuntime> REGISTERED = Collections.newSetFromMap(new WeakHashMap<>());

    private JeiRuntimeListenerBridge() {
    }

    public static void register(IJeiRuntime runtime) {
        if (runtime == null || REGISTERED.contains(runtime)) return;
        REGISTERED.add(runtime);
        runtime.getJeiHelpers().getIngredientVisibility().registerListener(INSTANCE);
        runtime.getIngredientManager().registerIngredientListener(INSTANCE);
    }

    @Override
    public <V> void onIngredientVisibilityChanged(ITypedIngredient<V> ingredient, boolean visible) {
        RecipeViewerStateSync.visibilityChanged();
    }

    @Override
    public <V> void onIngredientsAdded(IIngredientHelper<V> ingredientHelper, Collection<ITypedIngredient<V>> ingredients) {
        RecipeViewerStateSync.recipesChanged();
    }

    @Override
    public <V> void onIngredientsRemoved(IIngredientHelper<V> ingredientHelper, Collection<ITypedIngredient<V>> ingredients) {
        RecipeViewerStateSync.recipesChanged();
    }
}
