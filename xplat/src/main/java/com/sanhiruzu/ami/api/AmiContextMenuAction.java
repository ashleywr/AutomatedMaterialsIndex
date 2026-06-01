package com.sanhiruzu.ami.api;

import net.minecraft.network.chat.Component;

/**
 * Public description of an AMI context-menu entry contributed by another mod.
 */
public record AmiContextMenuAction(
        String id,
        Component label,
        Character mnemonic,
        boolean enabled,
        Runnable onClick
) {
    public AmiContextMenuAction {
        if (id == null) {
            id = "";
        }
    }

    public static AmiContextMenuAction enabled(String id, Component label, char mnemonic, Runnable onClick) {
        return new AmiContextMenuAction(id, label, mnemonic, true, onClick);
    }

    public static AmiContextMenuAction enabled(String id, Component label, Runnable onClick) {
        return new AmiContextMenuAction(id, label, null, true, onClick);
    }

    public static AmiContextMenuAction disabled(String id, Component label, char mnemonic) {
        return new AmiContextMenuAction(id, label, mnemonic, false, null);
    }
}
