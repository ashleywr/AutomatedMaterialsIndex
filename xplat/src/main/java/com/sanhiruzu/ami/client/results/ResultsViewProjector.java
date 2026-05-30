package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchService;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.Collectors;

public final class ResultsViewProjector {
    private ResultsViewProjector() {
    }

    public static Projection project(List<SearchNode> source,
                                     SearchState state,
                                     SearchService searchService,
                                     boolean compactMainPanel,
                                     boolean favoritesPanel) {
        List<SearchNode> effectiveSource = source;
        String query = state.getQuery();

        if (!query.isEmpty() && searchService != null) {
            effectiveSource = SearchScope.resolveQueriedSource(searchService, source, query, favoritesPanel);
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
            ResultsProcessor processor = state.createProcessor();
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
        return new Projection(
                roots,
                source.size(),
                effectiveSource.size(),
                summary(state, source.size(), effectiveSource.size(), compactMainPanel, favoritesPanel)
        );
    }

    private static String summary(SearchState state,
                                  int sourceCount,
                                  int displayedItemCount,
                                  boolean compactMainPanel,
                                  boolean favoritesPanel) {
        return "query=\"" + state.getQuery() + "\""
                + " entries=" + sourceCount
                + " displayed=" + displayedItemCount
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
            int sourceCount,
            int displayedItemCount,
            String summary
    ) {
    }
}
