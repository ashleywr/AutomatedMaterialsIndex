package com.sanhiruzu.ami.client;

import java.util.ArrayList;
import java.util.List;

final class RecipeViewerVisiblePageWindow {
    private RecipeViewerVisiblePageWindow() {
    }

    static Window compute(int requestedPageIndex, int recipesPerPage, int visibleCount) {
        int clampedRecipesPerPage = Math.max(1, recipesPerPage);
        int totalPages = Math.max(1, (int) Math.ceil((double) Math.max(0, visibleCount) / clampedRecipesPerPage));
        int pageIndex = Math.max(0, Math.min(requestedPageIndex, totalPages - 1));
        int startIndex = Math.min(pageIndex * clampedRecipesPerPage, Math.max(0, visibleCount));
        int endIndexExclusive = Math.min(startIndex + clampedRecipesPerPage, Math.max(0, visibleCount));
        return new Window(pageIndex, startIndex, endIndexExclusive, totalPages);
    }

    record Window(int pageIndex, int startIndex, int endIndexExclusive, int totalPages) {
    }

    static List<Integer> resolveVisibleCandidateIndexes(
            List<RecipeViewerDisplayEntryPolicy.Candidate> candidates,
            List<RecipeViewerDisplayEntryPolicy.DisplayEntry> visibleEntries
    ) {
        if (candidates == null || candidates.isEmpty() || visibleEntries == null || visibleEntries.isEmpty()) {
            return List.of();
        }

        List<RecipeViewerDisplayEntryPolicy.VisibleCandidate> visibleCandidates =
                RecipeViewerDisplayEntryPolicy.visibleCandidates(candidates);
        List<Integer> resolvedIndexes = new ArrayList<>(Math.min(visibleEntries.size(), visibleCandidates.size()));
        for (int i = 0; i < visibleEntries.size() && i < visibleCandidates.size(); i++) {
            resolvedIndexes.add(visibleCandidates.get(i).candidateIndex());
        }
        return List.copyOf(resolvedIndexes);
    }
}
