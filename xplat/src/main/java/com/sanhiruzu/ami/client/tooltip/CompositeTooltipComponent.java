package com.sanhiruzu.ami.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

/**
 * Stacks multiple ClientTooltipComponents vertically inside a single tooltip image slot.
 * Implements both TooltipComponent (marker, for the Optional argument in renderTooltip)
 * and ClientTooltipComponent (renderer). Register with an identity factory:
 * event.register(CompositeTooltipComponent.class, c -> c)
 */
public final class CompositeTooltipComponent implements TooltipComponent, ClientTooltipComponent {

    private final List<ClientTooltipComponent> children;

    public CompositeTooltipComponent(List<ClientTooltipComponent> children) {
        this.children = List.copyOf(children);
    }

    @Override
    public int getHeight(Font font) {
        return children.stream().mapToInt(c -> c.getHeight(font)).sum();
    }

    @Override
    public int getWidth(Font font) {
        return children.stream().mapToInt(c -> c.getWidth(font)).max().orElse(0);
    }

    @Override
    public void extractImage(Font font, int x, int y, int mouseX, int mouseY, GuiGraphicsExtractor g) {
        int cy = y;
        for (ClientTooltipComponent child : children) {
            child.extractImage(font, x, cy, mouseX, mouseY, g);
            cy += child.getHeight(font);
        }
    }
}
