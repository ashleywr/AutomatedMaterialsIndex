package com.sanhiruzu.ami.client.entitydetails;

import com.sanhiruzu.ami.index.SearchNode;

public record EntityDetailsLink(String label, SearchNode node) {
    public EntityDetailsLink {
        if (label == null) label = "";
    }
}
