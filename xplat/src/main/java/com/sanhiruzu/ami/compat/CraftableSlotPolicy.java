package com.sanhiruzu.ami.compat;

final class CraftableSlotPolicy {
    private CraftableSlotPolicy() {
    }

    static boolean shouldAccountOpenContainerSlot(boolean active, boolean playerInventorySlot, boolean empty,
                                                  boolean acceptsCurrentStack) {
        return active && !playerInventorySlot && !empty && acceptsCurrentStack;
    }
}
