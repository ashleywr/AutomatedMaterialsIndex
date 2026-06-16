package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.Identifier;

final class DiscoveryVisuals {
    private static final int ICON_SIZE = 16;
    private static final GeneratedGuiSprite UNDISCOVERED_ICON = new GeneratedGuiSprite(
            Identifier.fromNamespaceAndPath("ami", "generated/discovery_undiscovered"),
            ICON_SIZE,
            ICON_SIZE,
            () -> 1,
            canvas -> {
                canvas.fill(0, 0, ICON_SIZE, ICON_SIZE, 0xE6181818);
                canvas.fill(1, 1, ICON_SIZE - 1, ICON_SIZE - 1, 0x663C3C3C);
                for (int offset = -ICON_SIZE; offset < ICON_SIZE; offset += 5) {
                    for (int i = 0; i < ICON_SIZE; i++) {
                        int py = i + offset;
                        if (py >= 0 && py < ICON_SIZE) {
                            canvas.fill(i, py, i + 1, py + 1, 0xFF777777);
                        }
                    }
                }
                canvas.border(0, 0, ICON_SIZE, ICON_SIZE, 0xFF8A8A8A);
            }
    );
    private static final GeneratedGuiSprite DISCOVERED_ICON = new GeneratedGuiSprite(
            Identifier.fromNamespaceAndPath("ami", "generated/discovery_discovered"),
            ICON_SIZE,
            ICON_SIZE,
            () -> 1,
            canvas -> {
                int badgeSize = 8;
                int bx = ICON_SIZE - badgeSize;
                int by = ICON_SIZE - badgeSize;
                canvas.fill(bx - 1, by - 1, ICON_SIZE, ICON_SIZE, 0xDD0B1F0B);
                canvas.fill(bx, by, ICON_SIZE - 1, ICON_SIZE - 1, 0xFF1F8F3A);
                canvas.fill(bx + 2, by + 4, bx + 3, by + 6, 0xFFFFFFFF);
                canvas.fill(bx + 3, by + 5, bx + 4, by + 7, 0xFFFFFFFF);
                canvas.fill(bx + 4, by + 4, bx + 5, by + 6, 0xFFFFFFFF);
                canvas.fill(bx + 5, by + 3, bx + 6, by + 5, 0xFFFFFFFF);
                canvas.fill(bx + 6, by + 2, bx + 7, by + 4, 0xFFFFFFFF);
            }
    );

    private DiscoveryVisuals() {
    }

    static boolean isUndiscovered(SearchNode node) {
        return AmiConfig.enableDiscoveryChecklist
                && node != null
                && "undiscovered".equals(node.meta(SearchNodeKeys.DISCOVERY_STATE, ""));
    }

    static boolean hasDiscoveryState(SearchNode node) {
        return AmiConfig.enableDiscoveryChecklist
                && node != null
                && !node.meta(SearchNodeKeys.DISCOVERY_STATE, "").isBlank();
    }

    static boolean isDiscovered(SearchNode node) {
        return AmiConfig.enableDiscoveryChecklist
                && node != null
                && "discovered".equals(node.meta(SearchNodeKeys.DISCOVERY_STATE, ""));
    }

    static int primaryTextColor(SearchNode node, int fallback) {
        return isUndiscovered(node) ? 0xFF9A9A9A : fallback;
    }

    static int subtitleTextColor(SearchNode node, int fallback) {
        return isUndiscovered(node) ? 0xFF777777 : fallback;
    }

    static void renderIconMask(net.minecraft.client.gui.GuiGraphicsExtractor g, int x, int y, int size) {
        UNDISCOVERED_ICON.blit(g, x, y);
    }

    static void renderIconOverlay(net.minecraft.client.gui.GuiGraphicsExtractor g, SearchNode node, int x, int y, int size) {
        if (isUndiscovered(node)) {
            UNDISCOVERED_ICON.blit(g, x, y);
        } else if (isDiscovered(node)) {
            DISCOVERED_ICON.blit(g, x, y);
        }
    }
}
