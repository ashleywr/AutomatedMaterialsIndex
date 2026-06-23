package com.sanhiruzu.ami.client.sources;

import com.sanhiruzu.ami.index.NodeType;

import java.util.List;

public record ItemSourceRow(
        ItemSourceType type,
        String text,
        List<ItemSourceLink> links,
        ItemSourceLink primaryLink,
        String routeSummary,
        List<ItemSourceLink> biomeLinks
) {
    public ItemSourceRow {
        if (text == null) text = "";
        links = links == null ? List.of() : List.copyOf(links);
        if (primaryLink == null && !links.isEmpty()) primaryLink = links.get(0);
        if (routeSummary == null || routeSummary.isBlank()) routeSummary = text;
        biomeLinks = biomeLinks == null ? List.of() : List.copyOf(biomeLinks);
    }

    public ItemSourceRow(ItemSourceType type, String text, List<ItemSourceLink> links) {
        this(type, text, links, null, text, List.of());
    }

    public String routeActionLabel() {
        return switch (type) {
            case MOB_DROP -> "drop";
            case PROCESSING -> "process";
            case SALVAGE -> "salvage";
            case STRUCTURE_LOOT -> "loot";
            case TRADE -> "trade";
            case INDIRECT_SOURCE -> "route";
            case RECIPE -> "recipe";
        };
    }

    public ItemSourceLink routeOutputLink() {
        ItemSourceLink candidate = null;
        for (ItemSourceLink link : links) {
            if (link == null || link.node() == null) continue;
            if (sameLink(link, primaryLink)) continue;
            if (biomeLinks.stream().anyMatch(biome -> sameLink(link, biome))) continue;
            if (link.node().type() == NodeType.RECIPE) continue;
            candidate = link;
        }
        return candidate;
    }

    private static boolean sameLink(ItemSourceLink left, ItemSourceLink right) {
        if (left == null || right == null || left.node() == null || right.node() == null) {
            return false;
        }
        return left.node().type() == right.node().type() && left.node().id().equals(right.node().id());
    }
}
