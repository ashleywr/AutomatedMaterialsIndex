package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;
import java.util.Optional;

/**
 * Renders a node-type-appropriate icon into an axis-aligned pixel region.
 * All implementations must be stateless with respect to a single render call;
 * per-type caches are owned by the implementation but invalidated via invalidate().
 */
public interface IIconRenderer {

    /**
     * Draw the icon at (x, y) with the given pixel size on each side.
     */
    void render(GuiGraphics g, SearchNode node, int x, int y, int size, boolean hovered);

    /**
     * Text lines for the tooltip body (type-specific metadata).
     * Common elements like Name, Mod Branding, and Registry ID are handled by the master composer.
     * Return null for ITEM nodes to use native tooltips.
     */
    List<Component> getTooltip(SearchNode node);

    /**
     * Optional graphical block appended below the text lines.
     * Implement via a class that implements both TooltipComponent and ClientTooltipComponent,
     * registered with RegisterClientTooltipComponentFactoriesEvent (identity factory).
     * Default: empty — text-only tooltip.
     */
    default Optional<TooltipComponent> getTooltipImage(SearchNode node) {
        return Optional.empty();
    }

    /**
     * Release any GL or object caches. Called on world unload.
     */
    default void invalidate() {
    }
}
