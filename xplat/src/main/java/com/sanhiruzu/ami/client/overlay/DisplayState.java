package com.sanhiruzu.ami.client.overlay;

import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/**
 * Snapshot of overlay display state: expansion, selection, scroll, view mode, filters.
 * Can be saved before operations (like delete) and restored after to preserve UX.
 */
public final class DisplayState {
    private final Set<String> expandedNodeIds;
    private final ResourceLocation selectedNodeId;
    private final double scrollY;
    private final String viewMode;
    private final Set<String> activeFacets;

    public DisplayState(Set<String> expandedNodeIds, ResourceLocation selectedNodeId,
                       double scrollY, String viewMode, Set<String> activeFacets) {
        this.expandedNodeIds = new HashSet<>(expandedNodeIds);
        this.selectedNodeId = selectedNodeId;
        this.scrollY = scrollY;
        this.viewMode = viewMode;
        this.activeFacets = new HashSet<>(activeFacets);
    }

    public Set<String> expandedNodeIds() {
        return new HashSet<>(expandedNodeIds);
    }

    public ResourceLocation selectedNodeId() {
        return selectedNodeId;
    }

    public double scrollY() {
        return scrollY;
    }

    public String viewMode() {
        return viewMode;
    }

    public Set<String> activeFacets() {
        return new HashSet<>(activeFacets);
    }
}
