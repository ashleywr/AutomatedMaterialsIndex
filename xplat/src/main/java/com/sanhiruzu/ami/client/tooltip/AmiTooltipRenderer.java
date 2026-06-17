package com.sanhiruzu.ami.client.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sanhiruzu.ami.client.RenderStateSnapshot;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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

    public static void render(GuiGraphics g, Font font, ItemStack stack, int mouseX, int mouseY) {
        renderAtTooltipLayer(g, () -> g.renderTooltip(font, stack, mouseX, mouseY));
    }

    public static void renderResultItemTooltip(GuiGraphics g, Font font, ItemStack stack, SearchNode entry,
                                               int mouseX, int mouseY) {
        renderAtTooltipLayer(g, () -> Services.PLATFORM.renderTooltipElements(g, font,
                AmiResultTooltipElements.buildItemTooltip(stack, entry), stack, mouseX, mouseY));
    }

    public static void render(GuiGraphics g, Font font, List<Component> lines,
                              Optional<TooltipComponent> image, int mouseX, int mouseY) {
        if (image.isPresent()) {
            renderAtTooltipLayer(g, () -> g.renderTooltip(font, lines, image, mouseX, mouseY));
            return;
        }

        renderText(g, font, lines, mouseX, mouseY);
    }

    public static void render(GuiGraphics g, Font font, ItemStack stack, List<Component> lines,
                              Optional<TooltipComponent> image, int mouseX, int mouseY) {
        if (image.isPresent()) {
            renderAtTooltipLayer(g, () -> Services.PLATFORM.renderItemTooltip(g, font, lines, image, stack, mouseX, mouseY));
            return;
        }

        renderText(g, font, lines, mouseX, mouseY);
    }

    private static void renderText(GuiGraphics g, Font font, List<Component> lines, int mouseX, int mouseY) {
        renderAtTooltipLayer(g, () -> g.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY));
    }

    private static void renderAtTooltipLayer(GuiGraphics g, Runnable renderer) {
        g.flush();
        RenderStateSnapshot state = RenderStateSnapshot.capture();
        RENDERING_AMI_TOOLTIP.set(true);
        try {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            renderer.run();
            g.flush();
        } finally {
            RENDERING_AMI_TOOLTIP.set(false);
            state.restore();
        }
    }
}
