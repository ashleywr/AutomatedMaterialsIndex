package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class FallbackTextRenderer implements IIconRenderer {

    @Override
    public void render(GuiGraphics g, SearchNode node, int x, int y, int size) {
        renderFallback(g, node, x, y, size);
    }

    /** Static helper so other renderers can delegate without holding an instance. */
    public static void renderFallback(GuiGraphics g, SearchNode node, int x, int y, int size) {
        int bg = bgFor(node);
        g.fill(x, y, x + size, y + size, bg);

        String letter = node.displayName().isEmpty() ? "?"
                : node.displayName().substring(0, 1).toUpperCase();
        var font = Minecraft.getInstance().font;
        int textX = x + (size - font.width(letter)) / 2;
        int textY = y + (size - font.lineHeight) / 2;
        g.drawString(font, letter, textX, textY, 0xFFFFFFFF, false);
    }

    private static int bgFor(SearchNode node) {
        int c = node.color();
        int a = (c >> 24) & 0xFF;
        // If the node carries a meaningful ARGB color, use it (darkened)
        if (a > 10) {
            int r = Math.max(0, ((c >> 16) & 0xFF) - 60);
            int gv = Math.max(0, ((c >> 8) & 0xFF) - 60);
            int b = Math.max(0, (c & 0xFF) - 60);
            return 0xFF000000 | (r << 16) | (gv << 8) | b;
        }
        return switch (node.type()) {
            case ENTITY    -> 0xFF1A2020;
            case PLAYER    -> 0xFF1A1A30;
            case BIOME     -> 0xFF1A2A1A;
            case STRUCTURE -> 0xFF2A2A14;
            case DIMENSION -> 0xFF201020;
            default        -> 0xFF1E1E1E;
        };
    }

    @Override
    public List<Component> getTooltip(SearchNode node) {
        return List.of(
                Component.literal(node.displayName()),
                Component.literal(node.id().toString()).withStyle(s -> s.withColor(0x666666))
        );
    }
}
