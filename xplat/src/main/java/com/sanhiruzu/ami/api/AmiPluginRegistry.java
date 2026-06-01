package com.sanhiruzu.ami.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AmiPluginRegistry {
    private static final Logger LOGGER = Logger.getLogger(AmiPluginRegistry.class.getName());
    private static final List<IAmiPlugin> plugins = new ArrayList<>();
    private static boolean servicePluginsLoaded;

    private AmiPluginRegistry() {
    }

    public static synchronized void register(IAmiPlugin plugin) {
        if (plugin == null) return;
        String className = plugin.getClass().getName();
        for (IAmiPlugin existing : plugins) {
            if (existing == plugin || existing.getClass().getName().equals(className)) {
                return;
            }
        }
        plugins.add(plugin);
    }

    public static synchronized List<IAmiPlugin> getPlugins() {
        loadServicePlugins();
        return Collections.unmodifiableList(plugins);
    }

    /**
     * Loads AMI plugins declared through
     * {@code META-INF/services/com.sanhiruzu.ami.api.IAmiPlugin}.
     */
    public static synchronized void loadServicePlugins() {
        if (servicePluginsLoaded) return;
        servicePluginsLoaded = true;

        try {
            ServiceLoader<IAmiPlugin> loader = ServiceLoader.load(IAmiPlugin.class, AmiPluginRegistry.class.getClassLoader());
            for (IAmiPlugin plugin : loader) {
                register(plugin);
            }
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to load service plugins", e);
        }
    }

    static synchronized void clearForTests() {
        plugins.clear();
        servicePluginsLoaded = false;
    }
}
