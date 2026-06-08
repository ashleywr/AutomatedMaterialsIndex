package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.AmiGuideSearchIndex;
import com.sanhiruzu.ami.index.AmiQuestSearchIndex;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchService;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.Collectors;

public final class ResultsViewProjector {
    private ResultsViewProjector() {
    }

    /**
     * Apply universal filters that should work across all display contexts
     * (search results, favorites, sidebars, etc). Called first, before any
     * context-specific processing, so deletions are consistently applied everywhere.
     */
    private static List<SearchNode> applyUniversalFilters(List<SearchNode> nodes) {
        return nodes.stream()
                .filter(n -> !DeletedSearchNodesTracker.isDeleted(n.id()))
                .filter(n -> !SoftDeleteTracker.isFullyFaded(n.id()))
                .collect(Collectors.toList());
    }

    public static Projection project(List<SearchNode> source,
                                     SearchState state,
                                     SearchService searchService,
                                     boolean compactMainPanel,
                                     boolean favoritesPanel) {
        return project(source, state, searchService, null, compactMainPanel, favoritesPanel);
    }

    public static Projection project(List<SearchNode> source,
                                     SearchState state,
                                     SearchService searchService,
                                     AmiGuideSearchIndex guideSearchIndex,
                                     boolean compactMainPanel,
                                     boolean favoritesPanel) {
        return project(source, state, searchService, guideSearchIndex, null, compactMainPanel, favoritesPanel);
    }

    public static Projection project(List<SearchNode> source,
                                     SearchState state,
                                     SearchService searchService,
                                     AmiGuideSearchIndex guideSearchIndex,
                                     AmiQuestSearchIndex questSearchIndex,
                                     boolean compactMainPanel,
                                     boolean favoritesPanel) {
        List<SearchNode> effectiveSource = applyUniversalFilters(source);
        String query = state.getQuery();

        if (!query.isEmpty() && searchService != null) {
            effectiveSource = SearchScope.resolveQueriedSource(searchService, effectiveSource, query, favoritesPanel);
        }

        if (state.getViewMode() == ResultsToolbar.ViewMode.LIST && !compactMainPanel && !favoritesPanel) {
            effectiveSource = state.getListLens().filter(effectiveSource);
        }

        List<TreeNode> roots;
        if (favoritesPanel) {
            roots = effectiveSource.stream()
                    .map(n -> new TreeNode(Component.literal(n.displayName()), n))
                    .collect(Collectors.toList());
        } else {
            ResultsProcessor processor = createProcessorForProjection(state, compactMainPanel);
            if (compactMainPanel) {
                roots = processor.processFlatWithCardGrouping(effectiveSource);
            } else if (state.getViewMode() == ResultsToolbar.ViewMode.LIST
                    && state.getGroupBy() == ResultsProcessor.GroupBy.NONE) {
                roots = processor.processFlat(effectiveSource);
            } else {
                roots = processor.process(effectiveSource);
            }
        }

        roots = ResultsTreeNormalizer.normalize(roots);
        List<GuideResultRow> guideRows = favoritesPanel || compactMainPanel
                ? List.of()
                : GuideResultsProjector.project(query, guideSearchIndex);
        List<QuestResultRow> questRows = favoritesPanel || compactMainPanel
                ? List.of()
                : QuestResultsProjector.project(query, questSearchIndex);
        return new Projection(
                roots,
                guideRows,
                questRows,
                source.size(),
                effectiveSource.size(),
                summary(state, source.size(), effectiveSource.size(), guideRows.size(), questRows.size(), compactMainPanel, favoritesPanel)
        );
    }

    private static ResultsProcessor createProcessorForProjection(SearchState state, boolean compactMainPanel) {
        boolean gridPresentation = compactMainPanel || state.getViewMode() == ResultsToolbar.ViewMode.GRID;
        if (!gridPresentation) {
            return state.createProcessor();
        }

        return new ResultsProcessor(
                ResultsProcessor.SortField.REGISTRY,
                state.isAscending(),
                state.getGroupBy(),
                state.getSelectedMods(),
                state.getActiveFacets()
        );
    }

    private static String summary(SearchState state,
                                  int sourceCount,
                                  int displayedItemCount,
                                  int displayedGuideCount,
                                  int displayedQuestCount,
                                  boolean compactMainPanel,
                                  boolean favoritesPanel) {
        return "query=\"" + state.getQuery() + "\""
                + " entries=" + sourceCount
                + " displayed=" + displayedItemCount
                + " guides=" + displayedGuideCount
                + " quests=" + displayedQuestCount
                + " view=" + state.getViewMode()
                + " lens=" + state.getListLens()
                + " sort=" + state.getSortField()
                + " ascending=" + state.isAscending()
                + " group=" + state.getGroupBy()
                + " compact=" + compactMainPanel
                + " favorites=" + favoritesPanel;
    }

    public record Projection(
            List<TreeNode> roots,
            List<GuideResultRow> guideRows,
            List<QuestResultRow> questRows,
            int sourceCount,
            int displayedItemCount,
            String summary
    ) {
    }
}
