package com.sanhiruzu.searchableitems.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared item-provider registry. Viewers may consume this directly or adapt it
 * into their own indexing model.
 */
public final class SearchableItemProviders {
    private static final Logger LOGGER = Logger.getLogger(SearchableItemProviders.class.getName());
    private static final List<SearchableItemProvider> PROVIDERS = new ArrayList<>();
    private static boolean serviceProvidersLoaded;

    private SearchableItemProviders() {
    }

    public static synchronized void register(SearchableItemProvider provider) {
        if (provider == null) {
            return;
        }
        String className = provider.getClass().getName();
        for (SearchableItemProvider existing : PROVIDERS) {
            if (existing == provider || existing.getClass().getName().equals(className)) {
                return;
            }
        }
        PROVIDERS.add(provider);
    }

    public static synchronized List<SearchableItemProvider> getProviders() {
        loadServiceProviders();
        return Collections.unmodifiableList(PROVIDERS);
    }

    public static synchronized void loadServiceProviders() {
        if (serviceProvidersLoaded) {
            return;
        }
        serviceProvidersLoaded = true;

        try {
            ServiceLoader<SearchableItemProvider> loader = ServiceLoader.load(
                    SearchableItemProvider.class,
                    SearchableItemProviders.class.getClassLoader()
            );
            for (SearchableItemProvider provider : loader) {
                register(provider);
            }
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "Searchable item provider discovery failed", e);
        }
    }

    public static synchronized void clearForTests() {
        PROVIDERS.clear();
        serviceProvidersLoaded = false;
    }
}
