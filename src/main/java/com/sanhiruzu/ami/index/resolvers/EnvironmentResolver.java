package com.sanhiruzu.ami.index.resolvers;

import com.sanhiruzu.ami.index.*;

import java.util.*;

public class EnvironmentResolver implements IQueryResolver {
    private final List<SearchNode> dimensionNodes = new ArrayList<>();
    private final List<SearchNode> biomeNodes = new ArrayList<>();

    public void addNode(SearchNode node) {
        if (node.type() == NodeType.DIMENSION) {
            dimensionNodes.add(node);
        } else if (node.type() == NodeType.BIOME) {
            biomeNodes.add(node);
        }
    }

    @Override
    public Map<NodeType, List<SearchNode>> resolve(String query) {
        Map<NodeType, List<SearchNode>> result = new LinkedHashMap<>();
        String lowerQuery = query.toLowerCase();

        // Match dimensions
        List<SearchNode> matchedDimensions = new ArrayList<>();
        for (SearchNode node : dimensionNodes) {
            if (node.displayName().toLowerCase().contains(lowerQuery)) {
                matchedDimensions.add(node);
            }
        }

        // Match biomes in the queried dimension
        List<SearchNode> matchedBiomes = new ArrayList<>();
        for (SearchNode node : biomeNodes) {
            if (node.displayName().toLowerCase().contains(lowerQuery)) {
                matchedBiomes.add(node);
            }
        }

        if (!matchedDimensions.isEmpty()) {
            result.put(NodeType.DIMENSION, matchedDimensions);
        }
        if (!matchedBiomes.isEmpty()) {
            result.put(NodeType.BIOME, matchedBiomes);
        }

        return result;
    }
}
