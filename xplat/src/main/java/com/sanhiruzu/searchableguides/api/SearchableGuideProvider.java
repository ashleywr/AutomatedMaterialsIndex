package com.sanhiruzu.searchableguides.api;

import java.util.function.Consumer;

/**
 * Service-provider entry point for mods that want any compatible viewer to index
 * their guide pages.
 * <p>
 * Implementations can be discovered through:
 * {@code META-INF/services/com.sanhiruzu.searchableguides.api.SearchableGuideProvider}
 */
public interface SearchableGuideProvider {
    String id();

    void addGuideDocuments(Consumer<SearchableGuideDocument> documents);
}
