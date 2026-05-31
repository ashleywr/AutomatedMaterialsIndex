package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNode;

import java.util.List;

public final class CompatRegistry {
    private static final List<ModCompatModule> MODULES = List.of(
            new CobblemonCompatModule()
    );

    private CompatRegistry() {
    }

    public static boolean handleResultClick(SearchNode node, int button) {
        for (ModCompatModule module : MODULES) {
            if (!module.isLoaded()) {
                continue;
            }
            if (module.handleResultClick(node, button)) {
                return true;
            }
        }
        return false;
    }

    public static void invalidateCaches() {
        for (ModCompatModule module : MODULES) {
            if (module.isLoaded()) {
                module.invalidateCaches();
            }
        }
    }
}
