package com.sanhiruzu.ami.index.resolvers;

import com.sanhiruzu.ami.index.IQueryResolver;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.runtime.RuntimeSearchProviders;

import java.util.List;
import java.util.Map;

/**
 * Resolves volatile runtime documents through the same literal search surface
 * as persisted AMI index documents.
 */
public final class RuntimeSearchResolver implements IQueryResolver {
    @Override
    public Map<NodeType, List<SearchNode>> resolve(String query) {
        LiteralResolver resolver = new LiteralResolver(true);
        for (SearchNode node : RuntimeSearchProviders.nodes()) {
            resolver.addNode(node);
        }
        return resolver.resolve(query);
    }
}
