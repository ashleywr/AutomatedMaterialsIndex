package com.sanhiruzu.ami.client.results;

import java.util.Set;

/**
 * Immutable presentation settings used by result filtering, sorting, and tree building.
 */
public record ResultsPresentationOptions(
        ResultsProcessor.SortField sortField,
        boolean ascending,
        ResultsProcessor.GroupBy groupBy,
        Set<String> selectedMods,
        Set<String> activeFacets
) {
    public ResultsPresentationOptions {
        selectedMods = selectedMods == null ? Set.of() : Set.copyOf(selectedMods);
        activeFacets = activeFacets == null ? Set.of() : Set.copyOf(activeFacets);
    }
}
