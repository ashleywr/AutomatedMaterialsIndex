package com.sanhiruzu.searchableitems.api;

import net.minecraft.network.chat.Component;

/**
 * Viewer-neutral action that can be attached to an item result by any consumer.
 */
public record SearchableItemAction(
        String id,
        Component label,
        Character mnemonic,
        boolean enabled,
        Runnable onClick
) {
    public SearchableItemAction {
        if (id == null) {
            id = "";
        }
    }

    public static SearchableItemAction enabled(String id, Component label, char mnemonic, Runnable onClick) {
        return new SearchableItemAction(id, label, mnemonic, true, onClick);
    }

    public static SearchableItemAction enabled(String id, Component label, Runnable onClick) {
        return new SearchableItemAction(id, label, null, true, onClick);
    }

    public static SearchableItemAction disabled(String id, Component label, char mnemonic) {
        return new SearchableItemAction(id, label, mnemonic, false, null);
    }
}
