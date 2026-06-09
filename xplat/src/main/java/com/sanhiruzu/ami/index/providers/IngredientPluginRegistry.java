package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.platform.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class IngredientPluginRegistry {

    private static final List<IRecipeViewerPlugin> DISCOVERED = discoverPlugins();
    private static final List<IRecipeViewerPlugin> EXTERNAL = new CopyOnWriteArrayList<>();

    private IngredientPluginRegistry() {}

    public static void register(IRecipeViewerPlugin plugin) {
        if (plugin != null) {
            EXTERNAL.add(plugin);
        }
    }

    public static List<IRecipeViewerPlugin> getAll() {
        List<IRecipeViewerPlugin> all = new ArrayList<>(DISCOVERED.size() + EXTERNAL.size());
        all.addAll(DISCOVERED);
        all.addAll(EXTERNAL);
        return List.copyOf(all);
    }

    public static void registerAllIngredients(IRecipeViewerPlugin.IIngredientRegistration registration,
                                              IRecipeViewerPlugin.IExtraIngredientRegistration extraRegistration) {
        for (IRecipeViewerPlugin plugin : getAll()) {
            try {
                plugin.registerIngredients(registration);
            } catch (Throwable t) {
                AmiCore.LOGGER.warn("Plugin '{}' failed during registerIngredients", plugin.getClass().getName(), t);
            }
            try {
                plugin.registerExtraIngredients(extraRegistration);
            } catch (Throwable t) {
                AmiCore.LOGGER.warn("Plugin '{}' failed during registerExtraIngredients", plugin.getClass().getName(), t);
            }
        }
    }

    private static List<IRecipeViewerPlugin> discoverPlugins() {
        List<IRecipeViewerPlugin> result = Services.PLATFORM.discoverAnnotatedPlugins(
                RecipeViewerPlugin.class, IRecipeViewerPlugin.class);
        AmiCore.LOGGER.info("AMI: discovered {} @RecipeViewerPlugin classes", result.size());
        return result;
    }
}
