package com.sanhiruzu.searchableitems.api;

import java.util.function.Consumer;

/**
 * Service-provider entry point for viewer-neutral item result actions.
 * <p>
 * Implementations can be discovered through:
 * {@code META-INF/services/com.sanhiruzu.searchableitems.api.SearchableItemActionProvider}
 */
public interface SearchableItemActionProvider {
    String id();

    void addItemActions(SearchableItemActionContext context, Consumer<SearchableItemAction> actions);
}
