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
    private static final ClientTooltipPositioner LEFT_OF_CURSOR = (screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight) -> {
        int x = Math.max(mouseX - 12 - tooltipWidth, 4);
        int y = mouseY - 12;
        int heightWithPadding = tooltipHeight + 3;
        if (y + heightWithPadding > screenHeight) {
            y = Math.max(screenHeight - heightWithPadding, 4);
        }
        return new Vector2i(x, y);
    };

    private AmiTooltipRenderer() {
    }

    public static void renderLeftOfCursor(GuiGraphics g, Font font, ItemStack stack, int mouseX, int mouseY) {
        renderLeftOfCursor(g, font, net.minecraft.client.gui.screens.Screen.getTooltipFromItem(
                net.minecraft.client.Minecraft.getInstance(), stack), stack.getTooltipImage(), stack, mouseX, mouseY);
    }

    public static void renderLeftOfCursor(GuiGraphics g, Font font, List<Component> lines,
                                          Optional<TooltipComponent> image, int mouseX, int mouseY) {
        renderLeftOfCursor(g, font, lines, image, ItemStack.EMPTY, mouseX, mouseY);
    }

    private static void renderLeftOfCursor(GuiGraphics g, Font font, List<Component> lines,
                                           Optional<TooltipComponent> image, ItemStack stack,
                                           int mouseX, int mouseY) {
        List<ClientTooltipComponent> components = net.neoforged.neoforge.client.ClientHooks.gatherTooltipComponents(
                stack, lines, image, mouseX, g.guiWidth(), g.guiHeight(), font);
        render(g, font, components, stack, mouseX, mouseY);
    }

    private static void render(GuiGraphics g, Font font, List<ClientTooltipComponent> components,
                               ItemStack stack, int mouseX, int mouseY) {
        if (components.isEmpty()) return;

        var preEvent = net.neoforged.neoforge.client.ClientHooks.onRenderTooltipPre(
                stack, g, mouseX, mouseY, g.guiWidth(), g.guiHeight(), components, font, LEFT_OF_CURSOR);
        if (preEvent.isCanceled()) return;

        int tooltipWidth = 0;
        int tooltipHeight = components.size() == 1 ? -2 : 0;
        for (ClientTooltipComponent component : components) {
            tooltipWidth = Math.max(tooltipWidth, component.getWidth(preEvent.getFont()));
            tooltipHeight += component.getHeight();
        }

        Vector2ic pos = LEFT_OF_CURSOR.positionTooltip(g.guiWidth(), g.guiHeight(),
                preEvent.getX(), preEvent.getY(), tooltipWidth, tooltipHeight);
        int x = pos.x();
        int y = pos.y();

        g.pose().pushPose();
        var colorEvent = net.neoforged.neoforge.client.ClientHooks.onRenderTooltipColor(
                stack, g, x, y, preEvent.getFont(), components);
        g.flush();
        TooltipRenderUtil.renderTooltipBackground(g, x, y,
                tooltipWidth, tooltipHeight, 400,
                colorEvent.getBackgroundStart(), colorEvent.getBackgroundEnd(),
                colorEvent.getBorderStart(), colorEvent.getBorderEnd());
        g.flush();
        g.pose().translate(0.0F, 0.0F, 400.0F);

        int rowY = y;
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent component = components.get(i);
            component.renderText(preEvent.getFont(), x, rowY, g.pose().last().pose(), g.bufferSource());
            rowY += component.getHeight() + (i == 0 ? 2 : 0);
        }

        rowY = y;
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent component = components.get(i);
            component.renderImage(preEvent.getFont(), x, rowY, g);
            rowY += component.getHeight() + (i == 0 ? 2 : 0);
        }

        g.pose().popPose();
    }
}
