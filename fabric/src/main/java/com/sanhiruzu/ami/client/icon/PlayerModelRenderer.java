package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Fabric stub for PlayerModelRenderer.
 * Full player model rendering is deferred to a later milestone.
 * Provides the API surface required by xplat (RendererRegistry, ItemGridView, ResultsTreeView)
 * so the project compiles.
 */
public class PlayerModelRenderer implements IIconRenderer {

    // TODO(Milestone C+): implement Fabric player model rendering

    public PlayerModelRenderer() {
    }

    @Override
    public void render(GuiGraphics g, SearchNode node, int x, int y, int size, boolean hovered) {
        // No-op stub: renders nothing until Milestone C+
    }

    @Override
    public List<Component> getTooltip(SearchNode node) {
        return List.of();
    }

    @Override
    public void invalidate() {
    }
}
