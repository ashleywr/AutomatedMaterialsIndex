package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchService;
import net.minecraft.resources.Identifier;

import java.util.*;

public final class SearchScope {
    private SearchScope() {
    }

    public static List<SearchNode> resolveQueriedSource(SearchService searchService, List<SearchNode> source,
                                                        String query, boolean favoritesScoped) {
        Map<NodeType, List<SearchNode>> results = searchService.query(query);
        List<SearchNode> queried = new ArrayList<>();
        for (List<SearchNode> list : results.values()) {
            queried.addAll(list);
        }

        if (!favoritesScoped) {
            return queried;
        }

        Set<Identifier> allowedIds = new HashSet<>();
        for (SearchNode node : source) {
            allowedIds.add(node.id());
        }
        queried.removeIf(node -> !allowedIds.contains(node.id()));
        return queried;
    }
}
