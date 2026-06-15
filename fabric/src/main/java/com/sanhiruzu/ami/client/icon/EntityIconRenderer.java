package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Fabric stub for EntityIconRenderer.
 * Full entity icon rendering (atlas bake, warmup, 3D mob preview) is deferred to a later milestone.
 * Provides the API surface required by xplat (RendererRegistry, ItemIconRenderer) so the project compiles.
 */
public class EntityIconRenderer implements IIconRenderer {

    // TODO(Milestone C+): implement Fabric entity icon rendering

    public EntityIconRenderer() {
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

    /**
     * Returns entity IDs for which a render attempt has failed (e.g. unsupported entity types).
     * Stub: always empty until Milestone C+.
     */
    public static List<String> collectMissingEntities() {
        return List.of();
    }
}
