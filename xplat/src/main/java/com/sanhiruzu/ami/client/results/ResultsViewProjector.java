package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.AmiGuideSearchIndex;
import com.sanhiruzu.ami.index.AmiQuestSearchIndex;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.SearchService;
import com.sanhiruzu.ami.client.discovery.AmiDiscoveryState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        int originalSize = nodes.size();
        List<SearchNode> filtered = nodes.stream()
                .filter(n -> {
                    boolean isDeleted = DeletedSearchNodesTracker.isDeleted(n.id());
                    if (isDeleted) {
                        java.util.logging.Logger.getLogger(ResultsViewProjector.class.getName())
                                .log(java.util.logging.Level.INFO, "Filtering out deleted node: " + n.id());
                    }
                    return !isDeleted;
                })
                .filter(n -> {
                    boolean isFaded = SoftDeleteTracker.isFullyFaded(n.id());
                    if (isFaded) {
                        java.util.logging.Logger.getLogger(ResultsViewProjector.class.getName())
                                .log(java.util.logging.Level.INFO, "Filtering out faded node: " + n.id());
                    }
                    return !isFaded;
                })
                .collect(Collectors.toList());
        if (originalSize != filtered.size()) {
            java.util.logging.Logger.getLogger(ResultsViewProjector.class.getName())
                    .log(java.util.logging.Level.INFO, "applyUniversalFilters: " + originalSize + " -> " + filtered.size());
        }
        return filtered;
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
        effectiveSource = applyRuntimeMetadataForLens(effectiveSource);
        String query = state.getQuery();
        DiscoveryQuery discoveryQuery = DiscoveryQuery.parse(query);
        if (discoveryQuery.active()) {
            effectiveSource = discoveryQuery.filter(effectiveSource);
            query = discoveryQuery.remainingQuery();
        }

        if (!query.isEmpty() && searchService != null) {
            effectiveSource = discoveryQuery.active()
                    ? intersectQueriedSource(searchService, effectiveSource, query)
                    : SearchScope.resolveQueriedSource(searchService, effectiveSource, query, favoritesPanel);
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

    public static List<SearchNode> applyRuntimeMetadataForLens(List<SearchNode> nodes) {
        return nodes.stream()
                .map(AmiDiscoveryState.getInstance()::decorate)
                .collect(Collectors.toList());
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

    private static List<SearchNode> intersectQueriedSource(SearchService searchService, List<SearchNode> source, String query) {
        Map<NodeKey, SearchNode> sourceByKey = new LinkedHashMap<>();
        for (SearchNode node : source) {
            sourceByKey.put(NodeKey.of(node), node);
        }

        List<SearchNode> out = new ArrayList<>();
        for (List<SearchNode> bucket : searchService.query(query).values()) {
            for (SearchNode node : bucket) {
                SearchNode decorated = sourceByKey.remove(NodeKey.of(node));
                if (decorated != null) {
                    out.add(decorated);
                }
            }
        }
        return out;
    }

    private record NodeKey(NodeType type, ResourceLocation id) {
        static NodeKey of(SearchNode node) {
            return new NodeKey(node.type(), node.id());
        }
    }

    private record DiscoveryQuery(List<DiscoveryPredicate> predicates, String remainingQuery) {
        static DiscoveryQuery parse(String query) {
            if (query == null || query.isBlank()) {
                return new DiscoveryQuery(List.of(), "");
            }
            List<DiscoveryPredicate> predicates = new ArrayList<>();
            List<String> remaining = new ArrayList<>();
            for (String token : query.trim().split("\\s+")) {
                String normalized = normalizeToken(token);
                DiscoveryPredicate predicate = switch (normalized) {
                    case "discovered" -> DiscoveryPredicate.DISCOVERED_ANY;
                    case "undiscovered" -> DiscoveryPredicate.UNDISCOVERED_ANY;
                    case "visited" -> DiscoveryPredicate.VISITED_WORLD;
                    case "unvisited" -> DiscoveryPredicate.UNVISITED_WORLD;
                    case "tasted", "eaten" -> DiscoveryPredicate.TASTED_FOOD;
                    case "untasted", "uneaten" -> DiscoveryPredicate.UNTASTED_FOOD;
                    default -> null;
                };
                if (predicate == null) {
                    remaining.add(token);
                } else {
                    predicates.add(predicate);
                }
            }
            return new DiscoveryQuery(List.copyOf(predicates), String.join(" ", remaining));
        }

        boolean active() {
            return !predicates.isEmpty();
        }

        List<SearchNode> filter(List<SearchNode> nodes) {
            if (!active()) {
                return nodes;
            }
            return nodes.stream()
                    .filter(node -> predicates.stream().allMatch(predicate -> predicate.matches(node)))
                    .toList();
        }

        private static String normalizeToken(String token) {
            if (token == null) {
                return "";
            }
            return token.toLowerCase(Locale.ROOT).replaceAll("^[^a-z0-9]+|[^a-z0-9]+$", "");
        }
    }

    private enum DiscoveryPredicate {
        DISCOVERED_ANY {
            @Override
            boolean matches(SearchNode node) {
                return isDiscoverable(node) && isDiscovered(node);
            }
        },
        UNDISCOVERED_ANY {
            @Override
            boolean matches(SearchNode node) {
                return isDiscoverable(node) && !isDiscovered(node);
            }
        },
        VISITED_WORLD {
            @Override
            boolean matches(SearchNode node) {
                return isWorldDiscoveryNode(node) && isDiscovered(node);
            }
        },
        UNVISITED_WORLD {
            @Override
            boolean matches(SearchNode node) {
                return isWorldDiscoveryNode(node) && !isDiscovered(node);
            }
        },
        TASTED_FOOD {
            @Override
            boolean matches(SearchNode node) {
                return isFoodDiscoveryNode(node) && isDiscovered(node);
            }
        },
        UNTASTED_FOOD {
            @Override
            boolean matches(SearchNode node) {
                return isFoodDiscoveryNode(node) && !isDiscovered(node);
            }
        };

        abstract boolean matches(SearchNode node);

        static boolean isDiscovered(SearchNode node) {
            return AmiDiscoveryState.STATE_DISCOVERED.equals(node.meta(SearchNodeKeys.DISCOVERY_STATE, ""));
        }

        static boolean isDiscoverable(SearchNode node) {
            return isWorldDiscoveryNode(node) || isFoodDiscoveryNode(node);
        }

        static boolean isWorldDiscoveryNode(SearchNode node) {
            return node.type() == NodeType.BIOME || node.type() == NodeType.STRUCTURE;
        }

        static boolean isFoodDiscoveryNode(SearchNode node) {
            return node.type() == NodeType.ITEM && !node.meta(SearchNodeKeys.FOOD_NUTRITION, "").isBlank();
        }
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
