package com.sanhiruzu.ami.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;

import java.util.List;
import java.util.Optional;

public final class AmiTooltipRenderer {
    private static TooltipHooks hooks;

    private AmiTooltipRenderer() {}

    public static void setHooks(TooltipHooks h) {
        hooks = h;
    }

    public static void render(GuiGraphics g, Font font, ItemStack stack, int mouseX, int mouseY, boolean preferLeft) {
        List<Component> lines = net.minecraft.client.gui.screens.Screen.getTooltipFromItem(
                net.minecraft.client.Minecraft.getInstance(), stack);
        List<ClientTooltipComponent> components = hooks.gather(stack, lines, stack.getTooltipImage(), mouseX, g.guiWidth(), g.guiHeight(), font);
        renderInternal(g, font, components, stack, mouseX, mouseY, preferLeft);
    }

    public static void render(GuiGraphics g, Font font, List<Component> lines,
                                           Optional<TooltipComponent> image, int mouseX, int mouseY, boolean preferLeft) {
        render(g, font, ItemStack.EMPTY, lines, image, mouseX, mouseY, preferLeft);
    }

    public static void render(GuiGraphics g, Font font, ItemStack stack, List<Component> lines,
                              Optional<TooltipComponent> image, int mouseX, int mouseY, boolean preferLeft) {
        List<ClientTooltipComponent> components = hooks.gather(stack, lines, image, mouseX, g.guiWidth(), g.guiHeight(), font);
        renderInternal(g, font, components, stack, mouseX, mouseY, preferLeft);
    }

    private static void renderInternal(GuiGraphics g, Font font, List<ClientTooltipComponent> components,
                               ItemStack stack, int mouseX, int mouseY, boolean preferLeft) {
        if (components.isEmpty()) return;

        ClientTooltipPositioner positioner = (screenWidth, screenHeight, mx, my, tooltipWidth, tooltipHeight) -> {
            int x = TooltipPositioning.chooseX(screenWidth, mx, tooltipWidth, preferLeft);
            int y = TooltipPositioning.chooseY(screenHeight, my, tooltipHeight);
            return new Vector2i(x, y);
        };

        Optional<Font> preFont = hooks.onPre(stack, g, mouseX, mouseY, components, font, positioner);
        if (preFont.isEmpty()) return;

        com.sanhiruzu.ami.mixin.GuiGraphicsInvoker invoker = (com.sanhiruzu.ami.mixin.GuiGraphicsInvoker) g;
        ItemStack oldStack = invoker.ami$getTooltipStack();
        invoker.ami$setTooltipStack(stack);
        try {
            invoker.ami$renderTooltipInternal(preFont.get(), components, mouseX, mouseY, positioner);
        } finally {
            invoker.ami$setTooltipStack(oldStack);
        }
    }

    public interface TooltipHooks {
        List<ClientTooltipComponent> gather(ItemStack stack, List<Component> lines,
                                            Optional<TooltipComponent> image,
                                            int mouseX, int guiWidth, int guiHeight, Font font);

        /**
         * Fires the platform tooltip Pre event. Returns the font to use (possibly modified by the event),
         * or empty if a listener canceled rendering.
         */
        Optional<Font> onPre(ItemStack stack, GuiGraphics g, int mouseX, int mouseY,
                             List<ClientTooltipComponent> components, Font font,
                             ClientTooltipPositioner positioner);
    }
}
