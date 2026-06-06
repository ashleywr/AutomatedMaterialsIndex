package com.sanhiruzu.searchableguides.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared guide-provider registry. Viewers may use this directly or maintain
 * their own adapter around {@link SearchableGuideProvider}.
 */
public final class SearchableGuideProviders {
    private static final Logger LOGGER = Logger.getLogger(SearchableGuideProviders.class.getName());
    private static final List<SearchableGuideProvider> PROVIDERS = new ArrayList<>();
    private static boolean serviceProvidersLoaded;

    private SearchableGuideProviders() {
    }

    public static synchronized void register(SearchableGuideProvider provider) {
        if (provider == null) {
            return;
        }
        String className = provider.getClass().getName();
        for (SearchableGuideProvider existing : PROVIDERS) {
            if (existing == provider || existing.getClass().getName().equals(className)) {
                return;
            }
        }
        PROVIDERS.add(provider);
    }

    public static synchronized List<SearchableGuideProvider> getProviders() {
        loadServiceProviders();
        return Collections.unmodifiableList(PROVIDERS);
    }

    public static synchronized void loadServiceProviders() {
        if (serviceProvidersLoaded) {
            return;
        }
        serviceProvidersLoaded = true;

        try {
            ServiceLoader<SearchableGuideProvider> loader = ServiceLoader.load(
                    SearchableGuideProvider.class,
                    SearchableGuideProviders.class.getClassLoader()
            );
            for (SearchableGuideProvider provider : loader) {
                register(provider);
            }
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "Searchable guide provider discovery failed", e);
        }
    }

    public static synchronized void clearForTests() {
        PROVIDERS.clear();
        serviceProvidersLoaded = false;
    }
}
