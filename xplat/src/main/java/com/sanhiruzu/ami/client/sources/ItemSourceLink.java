package com.sanhiruzu.ami.client.sources;

import com.sanhiruzu.ami.index.SearchNode;

public record ItemSourceLink(String label, SearchNode node) {
    public ItemSourceLink {
        if (label == null) label = "";
    }
}
