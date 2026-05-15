package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Renders a node-type-appropriate icon into an axis-aligned pixel region.
 * All implementations must be stateless with respect to a single render call;
 * per-type caches are owned by the implementation but invalidated via invalidate().
 */
public interface IIconRenderer {

    /** Draw the icon at (x, y) with the given pixel size on each side. */
    void render(GuiGraphics g, SearchNode node, int x, int y, int size);

    /**
     * Tooltip lines to show when the node is hovered.
     * Return null to suppress the tooltip entirely.
     */
    List<Component> getTooltip(SearchNode node);

    /** Release any GL or object caches. Called on world unload. */
    default void invalidate() {}
}
