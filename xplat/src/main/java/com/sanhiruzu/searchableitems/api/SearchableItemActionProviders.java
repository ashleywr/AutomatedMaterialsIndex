package com.sanhiruzu.searchableitems.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared item-action provider registry.
 */
public final class SearchableItemActionProviders {
    private static final Logger LOGGER = Logger.getLogger(SearchableItemActionProviders.class.getName());
    private static final List<SearchableItemActionProvider> PROVIDERS = new ArrayList<>();
    private static boolean serviceProvidersLoaded;

    private SearchableItemActionProviders() {
    }

    public static synchronized void register(SearchableItemActionProvider provider) {
        if (provider == null) {
            return;
        }
        String className = provider.getClass().getName();
        for (SearchableItemActionProvider existing : PROVIDERS) {
            if (existing == provider || existing.getClass().getName().equals(className)) {
                return;
            }
        }
        PROVIDERS.add(provider);
    }

    public static synchronized List<SearchableItemActionProvider> getProviders() {
        loadServiceProviders();
        return Collections.unmodifiableList(PROVIDERS);
    }

    public static synchronized void loadServiceProviders() {
        if (serviceProvidersLoaded) {
            return;
        }
        serviceProvidersLoaded = true;

        try {
            ServiceLoader<SearchableItemActionProvider> loader = ServiceLoader.load(
                    SearchableItemActionProvider.class,
                    SearchableItemActionProviders.class.getClassLoader()
            );
            for (SearchableItemActionProvider provider : loader) {
                register(provider);
            }
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "Searchable item action provider discovery failed", e);
        }
    }

    public static synchronized void clearForTests() {
        PROVIDERS.clear();
        serviceProvidersLoaded = false;
    }
}
