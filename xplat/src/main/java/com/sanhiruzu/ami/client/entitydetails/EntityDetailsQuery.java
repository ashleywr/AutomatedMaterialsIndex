package com.sanhiruzu.ami.client.entitydetails;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchService;
import com.sanhiruzu.ami.index.query.SearchSyntax;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class EntityDetailsQuery {
    public static final String PREFIX = SearchSyntax.ENTITY_DETAILS_ROUTE_PREFIX;
    public static final String MOB_ALIAS_PREFIX = SearchSyntax.MOB_DETAILS_ROUTE_PREFIX;

    private EntityDetailsQuery() {
    }

    public static boolean isRoute(String query) {
        return parseTarget(query).isPresent();
    }

    public static Optional<String> parseTarget(String query) {
        if (query == null) return Optional.empty();
        String trimmed = query.trim();
        String lowered = trimmed.toLowerCase(Locale.ROOT);
        String prefix = lowered.startsWith(PREFIX) ? PREFIX : lowered.startsWith(MOB_ALIAS_PREFIX) ? MOB_ALIAS_PREFIX : "";
        if (prefix.isEmpty()) return Optional.empty();
        String target = trimmed.substring(prefix.length()).trim();
        return target.isBlank() ? Optional.empty() : Optional.of(target);
    }

    public static String queryFor(SearchNode node) {
        if (node == null || node.id() == null) return PREFIX;
        return PREFIX + node.id();
    }

    public static Optional<SearchNode> resolveTarget(String query, SearchService searchService) {
        Optional<String> parsed = parseTarget(query);
        if (parsed.isEmpty()) return Optional.empty();
        String target = parsed.get();
        GlobalIndex index = GlobalIndex.getInstance();

        ResourceLocation explicitId = ResourceLocation.tryParse(target);
        if (explicitId != null && target.contains(":")) {
            Optional<SearchNode> explicit = index.getNode(explicitId, NodeType.ENTITY);
            if (explicit.isPresent()) return explicit;
        }

        String normalized = normalize(target);
        List<SearchNode> exactMatches = new ArrayList<>();
        for (SearchNode entity : index.getNodes(NodeType.ENTITY)) {
            if (matches(entity, normalized)) exactMatches.add(entity);
        }
        if (exactMatches.size() == 1) return Optional.of(exactMatches.get(0));
        if (exactMatches.size() > 1) return Optional.empty();

        if (searchService != null) {
            List<SearchNode> results = searchService.query(target).getOrDefault(NodeType.ENTITY, List.of());
            if (!results.isEmpty()) {
                SearchNode first = results.get(0);
                return index.getNode(first.id(), NodeType.ENTITY).or(() -> Optional.of(first));
            }
        }
        return Optional.empty();
    }

    private static boolean matches(SearchNode entity, String normalized) {
        if (entity == null || entity.id() == null) return false;
        return normalize(entity.id().toString()).equals(normalized)
                || normalize(entity.id().getPath()).equals(normalized)
                || normalize(entity.displayName()).equals(normalized);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
