package com.sanhiruzu.ami.client.results;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sanhiruzu.ami.forge.AMI;
/**
 * Encapsulates the complete search and filter state for the AMI results panel.
 * Widgets can subscribe to changes to update their internal display state.
 */
public class SearchState {

    public interface Listener {
        void onSearchStateChanged(SearchState state);
    }

    private String query = "";
    private ResultsProcessor.SortField sortField = ResultsProcessor.SortField.ALPHABETICAL;
    private boolean ascending = true;
    private ResultsProcessor.GroupBy groupBy = ResultsProcessor.GroupBy.CATEGORY;
    private final Set<String> activeFacets = new HashSet<>();
    private final Set<String> selectedMods = new HashSet<>();
    private ResultsToolbar.ViewMode viewMode = ResultsToolbar.ViewMode.LIST;

    private final List<Listener> listeners = new ArrayList<>();

    public void reset() {
        this.query = "";
        this.sortField = ResultsProcessor.SortField.ALPHABETICAL;
        this.ascending = true;
        this.groupBy = ResultsProcessor.GroupBy.CATEGORY;
        this.activeFacets.clear();
        this.selectedMods.clear();
        this.viewMode = ResultsToolbar.ViewMode.LIST;
        notifyListeners();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Listener listener : listeners) {
            listener.onSearchStateChanged(this);
        }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
        notifyListeners();
    }

    public ResultsProcessor.SortField getSortField() {
        return sortField;
    }

    public void setSortField(ResultsProcessor.SortField sortField) {
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
        notifyListeners();
    }

    /**
     * Helper to create a processor with current state.
     */
    public ResultsProcessor createProcessor() {
        return new ResultsProcessor(sortField, ascending, groupBy, selectedMods, activeFacets);
    }
}
