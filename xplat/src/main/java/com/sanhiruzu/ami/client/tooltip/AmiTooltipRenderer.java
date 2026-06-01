package com.sanhiruzu.ami.client.tooltip;

import com.sanhiruzu.ami.client.overlay.OverlayLayers;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
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

    public static void render(GuiGraphics g, Font font, ItemStack stack, int mouseX, int mouseY) {
        renderAtTooltipLayer(g, () -> g.renderTooltip(font, stack, mouseX, mouseY));
    }

    public static void render(GuiGraphics g, Font font, List<Component> lines,
                              Optional<TooltipComponent> image, int mouseX, int mouseY) {
        if (image.isPresent()) {
            renderAtTooltipLayer(g, () -> g.renderTooltip(font, lines, image, mouseX, mouseY));
            return;
        }

        renderVisualText(g, font, lines, mouseX, mouseY);
    }

    public static void render(GuiGraphics g, Font font, ItemStack stack, List<Component> lines,
                              Optional<TooltipComponent> image, int mouseX, int mouseY) {
        if (image.isPresent()) {
            renderAtTooltipLayer(g, () -> g.renderTooltip(font, lines, image, stack, mouseX, mouseY));
            return;
        }

        renderVisualText(g, font, lines, mouseX, mouseY);
    }

    private static void renderVisualText(GuiGraphics g, Font font, List<Component> lines, int mouseX, int mouseY) {
        List<FormattedCharSequence> visualLines = lines.stream()
                .map(Component::getVisualOrderText)
                .toList();
        renderAtTooltipLayer(g, () -> g.renderTooltip(font, visualLines, mouseX, mouseY));
    }

    private static void renderAtTooltipLayer(GuiGraphics g, Runnable renderer) {
        RENDERING_AMI_TOOLTIP.set(true);
        g.pose().pushPose();
        g.pose().translate(0, 0, OverlayLayers.TRANSIENT_TOOLTIP);
        try {
            renderer.run();
        } finally {
            g.pose().popPose();
            RENDERING_AMI_TOOLTIP.set(false);
        }
        g.flush();
    }
}
