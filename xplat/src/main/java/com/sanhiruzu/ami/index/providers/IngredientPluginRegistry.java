package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for IAmiIngredientPlugin implementations.
 * Mods can register custom ingredient providers to contribute non-item ingredients
 * (gases, chemicals, fluids, etc.) to AMI's index without depending on recipe viewers.
 */
public final class IngredientPluginRegistry {
    private static final List<IAmiIngredientPlugin> BUILT_IN = List.of();
    private static final List<IAmiIngredientPlugin> EXTERNAL = new CopyOnWriteArrayList<>();

    private IngredientPluginRegistry() {
    }

    /**
     * Register an external ingredient plugin.
     */
    public static void register(IAmiIngredientPlugin plugin) {
        if (plugin != null) {
            EXTERNAL.add(plugin);
        }
    }

    /**
     * Get all registered ingredient plugins.
     */
    public static List<IAmiIngredientPlugin> getAll() {
        if (EXTERNAL.isEmpty()) {
            return BUILT_IN;
        }
        List<IAmiIngredientPlugin> all = new ArrayList<>(BUILT_IN);
        all.addAll(EXTERNAL);
        return List.copyOf(all);
    }

    /**
     * Register all ingredients from all plugins.
     */
    public static void registerAllIngredients(IAmiIngredientPlugin.IngredientRegistry registry) {
        for (IAmiIngredientPlugin plugin : getAll()) {
            try {
                plugin.registerIngredients(registry);
            } catch (Throwable t) {
                AmiCore.LOGGER.warn("AMI ingredient plugin '{}' failed during registration", plugin.modId(), t);
            }
        }
    }
}
