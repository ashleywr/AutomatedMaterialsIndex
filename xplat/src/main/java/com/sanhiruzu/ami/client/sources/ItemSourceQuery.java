package com.sanhiruzu.ami.client.sources;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchService;
import com.sanhiruzu.ami.index.query.SearchSyntax;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ItemSourceQuery {
    public static final String PREFIX = SearchSyntax.SOURCES_ROUTE_PREFIX;

    private ItemSourceQuery() {
    }

    public static boolean isRoute(String query) {
        return parseTarget(query).isPresent();
    }

    public static Optional<String> parseTarget(String query) {
        if (query == null) return Optional.empty();
        String trimmed = query.trim();
        if (!trimmed.toLowerCase(Locale.ROOT).startsWith(PREFIX)) {
            return Optional.empty();
        }
        String target = trimmed.substring(PREFIX.length()).trim();
        return target.isBlank() ? Optional.empty() : Optional.of(target);
    }

    public static String queryFor(SearchNode node) {
        if (node == null || node.id() == null) {
            return PREFIX;
        }
        return PREFIX + node.id();
    }

    public static Optional<SearchNode> resolveTarget(String query, SearchService searchService) {
        Optional<String> parsed = parseTarget(query);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        String target = parsed.get();
        GlobalIndex index = GlobalIndex.getInstance();

        ResourceLocation explicitId = ResourceLocation.tryParse(target);
        if (explicitId != null && target.contains(":")) {
            Optional<SearchNode> explicit = index.getNode(explicitId, NodeType.ITEM);
            if (explicit.isPresent()) return explicit;
        }

        String normalized = normalize(target);
        for (SearchNode item : index.getNodes(NodeType.ITEM)) {
            if (matches(item, normalized)) {
                return Optional.of(item);
            }
        }

        if (searchService != null) {
            List<SearchNode> results = searchService.query(target).getOrDefault(NodeType.ITEM, List.of());
            if (!results.isEmpty()) {
                SearchNode first = results.get(0);
                return index.getNode(first.id(), NodeType.ITEM).or(() -> Optional.of(first));
            }
        }
        return Optional.empty();
    }

    private static boolean matches(SearchNode item, String normalized) {
        if (item == null || item.id() == null) return false;
        return normalize(item.id().toString()).equals(normalized)
                || normalize(item.id().getPath()).equals(normalized)
                || normalize(item.displayName()).equals(normalized);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
