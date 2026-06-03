package com.sanhiruzu.ami.client.results;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates the complete search and filter state for the AMI results panel.
 * Widgets can subscribe to changes to update their internal display state.
 */
public class SearchState {

    private final Set<String> activeFacets = new HashSet<>();
    private final Set<String> selectedMods = new HashSet<>();
    private final List<Listener> listeners = new ArrayList<>();
    private String query = "";
    private ResultsProcessor.SortField sortField = ResultsProcessor.SortField.REGISTRY;
    private boolean ascending = true;
    private ResultsProcessor.GroupBy groupBy = ResultsProcessor.GroupBy.CATEGORY;
    private ResultsToolbar.ViewMode viewMode = ResultsToolbar.ViewMode.LIST;
    private ListLens listLens = ListLens.ALL;
    private List<ListLens> availableListLenses = List.of(ListLens.values());

    public SearchState() {
        RowFieldConfig.setSubtitleFields(listLens.subtitleFields());
    }

    public void reset() {
        this.query = "";
        this.activeFacets.clear();
        this.selectedMods.clear();
        this.viewMode = ResultsToolbar.ViewMode.LIST;
        this.availableListLenses = List.of(ListLens.values());
        resetPresentationDefaults();
        notifyListeners();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void refresh() {
        notifyListeners();
    }

    private void notifyListeners() {
        for (Listener listener : listeners) {
            listener.onSearchStateChanged(this);
        }
    }

    public String getQuery() {
        return query;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public void setQuery(String query) {
        query = query == null ? "" : query;
        if (this.query.equals(query)) {
            return;
        }
        this.query = query;
        notifyListeners();
    }

    public ResultsProcessor.SortField getSortField() {
        return sortField;
    }

    public void setSortField(ResultsProcessor.SortField sortField) {
        if (viewMode == ResultsToolbar.ViewMode.LIST && !listLens.sortFields().contains(sortField)) {
            return;
        }
        this.sortField = sortField;
        notifyListeners();
    }

    public boolean isAscending() {
        return ascending;
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
        notifyListeners();
    }

    public ResultsProcessor.GroupBy getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(ResultsProcessor.GroupBy groupBy) {
        this.groupBy = groupBy;
        notifyListeners();
    }

    public Set<String> getActiveFacets() {
        return new HashSet<>(activeFacets);
    }

    public void toggleFacet(String facetId) {
        if (activeFacets.contains(facetId)) activeFacets.remove(facetId);
        else activeFacets.add(facetId);
        notifyListeners();
    }

    /**
     * Selects only this facet, deselecting all others. If already the sole active facet, clears it.
     */
    public void selectOnlyFacet(String facetId) {
        if (activeFacets.size() == 1 && activeFacets.contains(facetId)) {
            activeFacets.clear();
        } else {
            activeFacets.clear();
            activeFacets.add(facetId);
        }
        notifyListeners();
    }

    public void clearFacets() {
        activeFacets.clear();
        notifyListeners();
    }

    public Set<String> getSelectedMods() {
        return new HashSet<>(selectedMods);
    }

    public void setSelectedMods(Set<String> mods) {
        this.selectedMods.clear();
        this.selectedMods.addAll(mods);
        notifyListeners();
    }

    public void toggleMod(String modId) {
        if (selectedMods.contains(modId)) selectedMods.remove(modId);
        else selectedMods.add(modId);
        notifyListeners();
    }

    public ResultsToolbar.ViewMode getViewMode() {
        return viewMode;
    }

    public void setViewMode(ResultsToolbar.ViewMode viewMode) {
        if (this.viewMode == viewMode) return;
        this.viewMode = viewMode;
        resetPresentationDefaults();
        notifyListeners();
    }

    public ListLens getListLens() {
        return listLens;
    }

    public void setListLens(ListLens listLens) {
        if (listLens == null) {
            listLens = ListLens.ALL;
        }
        if (!availableListLenses.contains(listLens)) {
            listLens = ListLens.ALL;
        }
        this.listLens = listLens;
        this.sortField = listLens.sortField();
        this.ascending = listLens.ascending();
        this.groupBy = listLens.groupBy();
        RowFieldConfig.setSubtitleFields(listLens.subtitleFields());
        notifyListeners();
    }

    public List<ListLens> getAvailableListLenses() {
        return availableListLenses;
    }

    public void setAvailableListLenses(List<ListLens> availableListLenses) {
        if (availableListLenses == null || availableListLenses.isEmpty()) {
            availableListLenses = List.of(ListLens.ALL);
        }
        this.availableListLenses = List.copyOf(availableListLenses);
        if (!this.availableListLenses.contains(listLens)) {
            resetPresentationDefaults();
        }
    }

    private void resetPresentationDefaults() {
        this.listLens = ListLens.ALL;
        this.sortField = ListLens.ALL.sortField();
        this.ascending = ListLens.ALL.ascending();
        this.groupBy = ListLens.ALL.groupBy();
        RowFieldConfig.setSubtitleFields(ListLens.ALL.subtitleFields());
    }

    /**
     * Helper to create a processor with current state.
     */
    public ResultsProcessor createProcessor() {
        return new ResultsProcessor(sortField, ascending, groupBy, selectedMods, activeFacets);
    }

    public interface Listener {
        void onSearchStateChanged(SearchState state);
    }
}
