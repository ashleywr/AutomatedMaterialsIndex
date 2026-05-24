package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.util.AmiWorldTooltipComposer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class FallbackTextRenderer implements IIconRenderer {

    @Override
    public void render(GuiGraphics g, SearchNode node, int x, int y, int size) {
        renderFallback(g, node, x, y, size);
    }

    /**
     * Static helper so other renderers can delegate without holding an instance.
     */
    public static void renderFallback(GuiGraphics g, SearchNode node, int x, int y, int size) {
        int bg = AmiConfig.devMode ? AMITheme.FALLBACK_BG_DEV : bgFor(node);
        g.fill(x, y, x + size, y + size, bg);

        String letter = node.displayName().isEmpty()
                ? Component.translatable("ami.tooltip.fallback_unknown").getString()
                : node.displayName().substring(0, 1).toUpperCase();
        var font = Minecraft.getInstance().font;
        int textX = x + (size - font.width(letter)) / 2;
        int textY = y + (size - font.lineHeight) / 2;
        g.drawString(font, letter, textX, textY, AMITheme.WHITE, false);
    }

    private static int bgFor(SearchNode node) {
        int c = node.color();
        int a = (c >> 24) & 0xFF;
        // If the node carries a meaningful ARGB color, use it (darkened)
        if (a > 10) {
            int r = Math.max(0, ((c >> 16) & 0xFF) - 60);
            int gv = Math.max(0, ((c >> 8) & 0xFF) - 60);
            int b = Math.max(0, (c & 0xFF) - 60);
            return AMITheme.BLACK | (r << 16) | (gv << 8) | b;
        }
        return switch (node.type()) {
            case ENTITY -> AMITheme.FALLBACK_BG_ENTITY;
            case PLAYER -> AMITheme.FALLBACK_BG_PLAYER;
            case BIOME -> AMITheme.FALLBACK_BG_BIOME;
            case STRUCTURE -> AMITheme.FALLBACK_BG_STRUCTURE;
            case DIMENSION -> AMITheme.FALLBACK_BG_DIMENSION;
            default -> AMITheme.FALLBACK_BG_DEFAULT;
        };
    }

    @Override
    public List<Component> getTooltip(SearchNode node) {
        return AmiWorldTooltipComposer.build(node);
    }
}
