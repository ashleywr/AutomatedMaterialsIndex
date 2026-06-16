package com.sanhiruzu.ami.client;

/**
 * RenderSystem GL-state snapshot APIs removed in MC 26.x.
 * Retained as a stub so call sites compile; capture/restore are no-ops.
 */
public final class RenderStateSnapshot {
    private static final RenderStateSnapshot INSTANCE = new RenderStateSnapshot();

    private RenderStateSnapshot() {}

    public static RenderStateSnapshot capture() {
        return INSTANCE;
    }

    public void restore() {
    }
}
