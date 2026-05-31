package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNode;

public interface ModCompatModule {
    String modId();

    default boolean isLoaded() {
        return ReflectiveCompat.classExists(probeClassName());
    }

    default String probeClassName() {
        return "";
    }

    default boolean handleResultClick(SearchNode node, int button) {
        return false;
    }

    default void invalidateCaches() {
    }
}
