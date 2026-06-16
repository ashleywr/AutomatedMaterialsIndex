package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

final class QuestMarkerSpriteRenderer {
    private static final int SIZE = 6;
    private static final GeneratedGuiSprite REQUIREMENT = marker("requirement", false);
    private static final GeneratedGuiSprite REQUIREMENT_MULTI = marker("requirement_multi", true);
    private static final GeneratedGuiSprite REWARD = marker("reward", false);
    private static final GeneratedGuiSprite REWARD_MULTI = marker("reward_multi", true);

    private QuestMarkerSpriteRenderer() {
    }

    static void render(GuiGraphicsExtractor g, int x, int y, boolean requirement, boolean multi) {
        GeneratedGuiSprite sprite = requirement
                ? multi ? REQUIREMENT_MULTI : REQUIREMENT
                : multi ? REWARD_MULTI : REWARD;
        sprite.blit(g, x, y);
    }

    private static GeneratedGuiSprite marker(String name, boolean multi) {
        return new GeneratedGuiSprite(
                Identifier.fromNamespaceAndPath("ami", "generated/quest_marker_" + name),
                SIZE,
                SIZE,
                () -> signature(multi),
                canvas -> {
                    int color = name.startsWith("requirement") ? AMITheme.ACCENT_BLUE : AMITheme.ACCENT_GOLD;
                    canvas.fill(0, 0, SIZE, SIZE, 0xCC000000);
                    canvas.fill(1, 1, SIZE - 1, SIZE - 1, color);
                    if (multi) {
                        canvas.fill(4, 4, SIZE, SIZE, AMITheme.WHITE);
                    }
                }
        );
    }

    private static int signature(boolean multi) {
        int result = AMITheme.ACCENT_BLUE;
        result = 31 * result + AMITheme.ACCENT_GOLD;
        result = 31 * result + AMITheme.WHITE;
        result = 31 * result + (multi ? 1 : 0);
        return result;
    }
}
