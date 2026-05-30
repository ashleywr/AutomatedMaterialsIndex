package com.sanhiruzu.ami.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;

import java.util.List;
import java.util.Optional;

public class NeoForgeTooltipHooks implements AmiTooltipRenderer.TooltipHooks {
    @Override
    public List<ClientTooltipComponent> gather(ItemStack stack, List<Component> lines,
                                               Optional<TooltipComponent> image,
                                               int mouseX, int guiWidth, int guiHeight, Font font) {
        return ClientHooks.gatherTooltipComponents(stack, lines, image, mouseX, guiWidth, guiHeight, font);
    }

    @Override
    public PreResult onPre(ItemStack stack, GuiGraphics g, int mouseX, int mouseY,
                           int guiWidth, int guiHeight, List<ClientTooltipComponent> components,
                           Font font, ClientTooltipPositioner positioner) {
        var e = ClientHooks.onRenderTooltipPre(stack, g, mouseX, mouseY, guiWidth, guiHeight, components, font, positioner);
        return new PreResult(e.isCanceled(), e.getFont(), e.getX(), e.getY());
    }

    @Override
    public ColorResult onColor(ItemStack stack, GuiGraphics g, int x, int y,
                               Font font, List<ClientTooltipComponent> components) {
        var e = ClientHooks.onRenderTooltipColor(stack, g, x, y, font, components);
        return new ColorResult(e.getBackgroundStart(), e.getBackgroundEnd(), e.getBorderStart(), e.getBorderEnd());
    }
}
