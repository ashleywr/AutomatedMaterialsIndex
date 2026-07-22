package com.sanhiruzu.ami.compat;

final class CraftablesScopePolicy {
    private CraftablesScopePolicy() {
    }

    static boolean shouldAccountMenuContents(boolean recipeBookMenu) {
        return recipeBookMenu;
    }

    static boolean shouldAccountRecipeBookSlot(boolean active, boolean empty, boolean acceptsCurrentStack) {
        return active && !empty && acceptsCurrentStack;
    }
}
