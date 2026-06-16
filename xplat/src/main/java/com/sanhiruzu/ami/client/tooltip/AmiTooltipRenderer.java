package com.sanhiruzu.ami.client.tooltip;

import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class AmiTooltipRenderer {
    private static final ThreadLocal<Boolean> RENDERING_AMI_TOOLTIP = ThreadLocal.withInitial(() -> false);

    private AmiTooltipRenderer() {}

    public static boolean isRenderingAmiTooltip() {
        return RENDERING_AMI_TOOLTIP.get();
    }

    public static void render(GuiGraphicsExtractor g, Font font, ItemStack stack, int mouseX, int mouseY) {
        renderAtTooltipLayer(g, () -> g.setTooltipForNextFrame(font, stack, mouseX, mouseY));
    }

    public static void render(GuiGraphicsExtractor g, Font font, List<Component> lines,
                              Optional<TooltipComponent> image, int mouseX, int mouseY) {
        if (image.isPresent()) {
            renderAtTooltipLayer(g, () -> g.setTooltipForNextFrame(font, lines, image, mouseX, mouseY));
            return;
        }

        renderText(g, font, lines, mouseX, mouseY);
    }

    public static void render(GuiGraphicsExtractor g, Font font, ItemStack stack, List<Component> lines,
                              Optional<TooltipComponent> image, int mouseX, int mouseY) {
        if (image.isPresent()) {
            renderAtTooltipLayer(g, () -> Services.PLATFORM.renderItemTooltip(g, font, lines, image, stack, mouseX, mouseY));
            return;
        }

        renderText(g, font, lines, mouseX, mouseY);
    }

    private static void renderText(GuiGraphicsExtractor g, Font font, List<Component> lines, int mouseX, int mouseY) {
        renderAtTooltipLayer(g, () -> g.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY));
    }

    private static void renderAtTooltipLayer(GuiGraphicsExtractor g, Runnable renderer) {
        RENDERING_AMI_TOOLTIP.set(true);
        try {
            renderer.run();
        } finally {
            RENDERING_AMI_TOOLTIP.set(false);
        }
    }
}
