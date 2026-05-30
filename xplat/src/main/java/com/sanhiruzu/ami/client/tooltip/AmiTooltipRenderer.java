package com.sanhiruzu.ami.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.List;
import java.util.Optional;

public final class AmiTooltipRenderer {
    static final ClientTooltipPositioner LEFT_OF_CURSOR = (screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight) -> {
        int x = TooltipPositioning.chooseX(screenWidth, mouseX, tooltipWidth, true);
        int y = TooltipPositioning.chooseY(screenHeight, mouseY, tooltipHeight);
        return new Vector2i(x, y);
    };

    static final ClientTooltipPositioner RIGHT_OF_CURSOR = (screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight) -> {
        int x = TooltipPositioning.chooseX(screenWidth, mouseX, tooltipWidth, false);
        int y = TooltipPositioning.chooseY(screenHeight, mouseY, tooltipHeight);
        return new Vector2i(x, y);
    };

    private static TooltipHooks hooks;

    private AmiTooltipRenderer() {}

    public static void setHooks(TooltipHooks h) {
        hooks = h;
    }

    public static void renderLeftOfCursor(GuiGraphics g, Font font, ItemStack stack, int mouseX, int mouseY) {
        List<Component> lines = net.minecraft.client.gui.screens.Screen.getTooltipFromItem(
                net.minecraft.client.Minecraft.getInstance(), stack);
        List<ClientTooltipComponent> components = hooks.gather(stack, lines, stack.getTooltipImage(), mouseX, g.guiWidth(), g.guiHeight(), font);
        render(g, font, components, stack, mouseX, mouseY, LEFT_OF_CURSOR);
    }

    public static void renderLeftOfCursor(GuiGraphics g, Font font, List<Component> lines,
                                          Optional<TooltipComponent> image, int mouseX, int mouseY) {
        List<ClientTooltipComponent> components = hooks.gather(ItemStack.EMPTY, lines, image, mouseX, g.guiWidth(), g.guiHeight(), font);
        render(g, font, components, ItemStack.EMPTY, mouseX, mouseY, LEFT_OF_CURSOR);
    }

    public static void renderRightOfCursor(GuiGraphics g, Font font, ItemStack stack, int mouseX, int mouseY) {
        List<Component> lines = net.minecraft.client.gui.screens.Screen.getTooltipFromItem(
                net.minecraft.client.Minecraft.getInstance(), stack);
        List<ClientTooltipComponent> components = hooks.gather(stack, lines, stack.getTooltipImage(), mouseX, g.guiWidth(), g.guiHeight(), font);
        render(g, font, components, stack, mouseX, mouseY, RIGHT_OF_CURSOR);
    }

    public static void renderRightOfCursor(GuiGraphics g, Font font, List<Component> lines,
                                           Optional<TooltipComponent> image, int mouseX, int mouseY) {
        List<ClientTooltipComponent> components = hooks.gather(ItemStack.EMPTY, lines, image, mouseX, g.guiWidth(), g.guiHeight(), font);
        render(g, font, components, ItemStack.EMPTY, mouseX, mouseY, RIGHT_OF_CURSOR);
    }

    private static void render(GuiGraphics g, Font font, List<ClientTooltipComponent> components,
                               ItemStack stack, int mouseX, int mouseY, ClientTooltipPositioner positioner) {
        if (components.isEmpty()) return;

        TooltipHooks.PreResult pre = hooks.onPre(stack, g, mouseX, mouseY, g.guiWidth(), g.guiHeight(), components, font, positioner);
        if (pre.canceled()) return;

        int tooltipWidth = 0;
        int tooltipHeight = components.size() == 1 ? -2 : 0;
        for (ClientTooltipComponent component : components) {
            tooltipWidth = Math.max(tooltipWidth, component.getWidth(pre.font()));
            tooltipHeight += component.getHeight();
        }

        Vector2ic pos = positioner.positionTooltip(g.guiWidth(), g.guiHeight(),
                pre.x(), pre.y(), tooltipWidth, tooltipHeight);
        int x = pos.x();
        int y = pos.y();

        g.pose().pushPose();
        TooltipHooks.ColorResult color = hooks.onColor(stack, g, x, y, pre.font(), components);
        g.flush();
        TooltipRenderUtil.renderTooltipBackground(g, x, y, tooltipWidth, tooltipHeight, 400,
                color.bgStart(), color.bgEnd(), color.borderStart(), color.borderEnd());
        g.flush();
        g.pose().translate(0.0F, 0.0F, 400.0F);

        int rowY = y;
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent component = components.get(i);
            component.renderText(pre.font(), x, rowY, g.pose().last().pose(), g.bufferSource());
            rowY += component.getHeight() + (i == 0 ? 2 : 0);
        }

        rowY = y;
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent component = components.get(i);
            component.renderImage(pre.font(), x, rowY, g);
            rowY += component.getHeight() + (i == 0 ? 2 : 0);
        }

        g.flush();
        g.pose().popPose();
    }

    public interface TooltipHooks {
        List<ClientTooltipComponent> gather(ItemStack stack, List<Component> lines,
                                            Optional<TooltipComponent> image,
                                            int mouseX, int guiWidth, int guiHeight, Font font);

        PreResult onPre(ItemStack stack, GuiGraphics g, int mouseX, int mouseY,
                        int guiWidth, int guiHeight, List<ClientTooltipComponent> components,
                        Font font, ClientTooltipPositioner positioner);

        ColorResult onColor(ItemStack stack, GuiGraphics g, int x, int y,
                            Font font, List<ClientTooltipComponent> components);

        record PreResult(boolean canceled, Font font, int x, int y) {}

        record ColorResult(int bgStart, int bgEnd, int borderStart, int borderEnd) {}
    }
}
