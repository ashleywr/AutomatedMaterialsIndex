package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

/**
 * Renders the 8×8-pixel face from a player's skin texture into the icon cell.
 * The skin face region is naturally 8×8 pixels so it scales crisply at any
 * integer multiple; a 1-pixel border is drawn around it to separate it visually
 * from the background (profile-picture aesthetic).
 */
public class PlayerFaceRenderer implements IIconRenderer {

    private static Identifier resolveSkin(SearchNode node) {
        String uuidStr = node.meta(SearchNodeKeys.PLAYER_UUID);
        if (uuidStr.isEmpty()) return null;

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr.contains("-") ? uuidStr : dashedUuid(uuidStr));
        } catch (IllegalArgumentException e) {
            return null;
        }

        Minecraft mc = Minecraft.getInstance();
        var connection = mc.getConnection();
        if (connection == null) return null;

        PlayerInfo info = connection.getPlayerInfo(uuid);
        if (info == null) return null;

        return Services.PLATFORM.getPlayerSkinTexture(info);
    }

    private static String dashedUuid(String raw) {
        if (raw == null || raw.length() != 32) {
            return raw;
        }
        return raw.substring(0, 8) + "-"
                + raw.substring(8, 12) + "-"
                + raw.substring(12, 16) + "-"
                + raw.substring(16, 20) + "-"
                + raw.substring(20);
    }

    @Override
    public void render(GuiGraphicsExtractor g, SearchNode node, int x, int y, int size, boolean hovered) {
        Identifier skin = resolveSkin(node);
        if (skin == null) {
            FallbackTextRenderer.renderFallback(g, node, x, y, size);
            return;
        }

        // 1px border
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, AMITheme.ENTITY_CATEGORY_COLOR);

        // Skin face region: 8×8 at UV (8, 8) in a 64×64 texture.
        // Scale using the pose matrix so every pixel maps cleanly.
        var poses = g.pose();
        poses.pushMatrix();
        poses.translate(x, y);
        float s = size / 8f;
        poses.scale(s, s);
        g.blit(skin, 0, 0, 8, 8, 8.0f / 64f, 16.0f / 64f, 8.0f / 64f, 16.0f / 64f);
        poses.popMatrix();
    }

    @Override
    public List<Component> getTooltip(SearchNode node) {
        return List.of(
                Component.translatable("ami.tooltip.player_subtitle").withStyle(s -> s.withColor(AMITheme.ENTITY_PLAYER_TEXT))
        );
    }
}
