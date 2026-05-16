package com.sanhiruzu.ami.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

/**
 * Stacks multiple ClientTooltipComponents vertically inside a single tooltip image slot.
 * Implements both TooltipComponent (marker, for the Optional argument in renderTooltip)
 * and ClientTooltipComponent (renderer). Register with an identity factory:
 *   event.register(CompositeTooltipComponent.class, c -> c)
 */
public final class CompositeTooltipComponent implements TooltipComponent, ClientTooltipComponent {

    private final List<ClientTooltipComponent> children;

    public CompositeTooltipComponent(List<ClientTooltipComponent> children) {
        this.children = List.copyOf(children);
    }

    @Override
    public int getHeight() {
        return children.stream().mapToInt(ClientTooltipComponent::getHeight).sum();
    }

    @Override
    public int getWidth(Font font) {
        return children.stream().mapToInt(c -> c.getWidth(font)).max().orElse(0);
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics g) {
        int cy = y;
        for (ClientTooltipComponent child : children) {
            child.renderImage(font, x, cy, g);
            cy += child.getHeight();
        }
    }
}
