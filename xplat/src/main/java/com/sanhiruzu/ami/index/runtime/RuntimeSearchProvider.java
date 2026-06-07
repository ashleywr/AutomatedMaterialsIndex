package com.sanhiruzu.ami.index.runtime;

import com.sanhiruzu.ami.index.SearchNode;

import java.util.List;

public interface RuntimeSearchProvider {
    String id();

    long revision();

    List<SearchNode> nodes();
}
