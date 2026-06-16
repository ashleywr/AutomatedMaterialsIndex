package com.sanhiruzu.ami.client.icon;

import com.mojang.authlib.GameProfile;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class PlayerModelRenderer implements IIconRenderer {
    private static final Map<String, RemotePlayer> PLAYER_CACHE = new HashMap<>();
    private static final Map<String, CompletableFuture<GameProfile>> PENDING_PROFILES = new HashMap<>();

    private static RemotePlayer resolvePlayer(String name) {
        RemotePlayer cached = PLAYER_CACHE.get(name);
        if (cached != null) {
            return cached;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        var connection = mc.getConnection();
        if (connection != null) {
            for (PlayerInfo info : connection.getOnlinePlayers()) {
                GameProfile profile = info.getProfile();
                if (profile != null && name.equalsIgnoreCase(profile.name())) {
                    return cache(name, new RenderOnlyPlayer(mc.level, profile));
                }
            }
        }
        CompletableFuture<GameProfile> pending = PENDING_PROFILES.get(name);
        if (pending != null) {
            if (!pending.isDone()) {
                return null;
            }
            PENDING_PROFILES.remove(name);
            GameProfile profile = pending.getNow(null);
            return profile == null ? null : cache(name, new RenderOnlyPlayer(mc.level, profile));
        }
        ResolvableProfile rp = ResolvableProfile.createUnresolved(name);
        PENDING_PROFILES.put(name, rp.resolveProfile(Minecraft.getInstance().services().profileResolver())
                .exceptionally(ex -> null));
        return null;
    }

    private static RemotePlayer cache(String name, RemotePlayer player) {
        PLAYER_CACHE.put(name, player);
        return player;
    }

    private static void renderPlayer(GuiGraphicsExtractor g, int x, int y, int size, RemotePlayer player, float yRot) {
        float renderScale = Math.max(1.0f, (size - 2.0f) / Math.max(0.1f, player.getBbHeight()));
        float xAngle = (yRot - 180.0f) / 20.0f;
        float offsetY = player.getBbHeight() / 2.0f;
        g.pose().pushMatrix();
        try {
            IconRenderState.render3dIcon(g, () ->
                    InventoryScreen.renderEntityInInventoryFollowsAngle(g, x, y, x + size, y + size, (int) renderScale, offsetY, xAngle, 0.0f, player)
            );
        } finally {
            g.pose().popMatrix();
        }
    }

    @Override
    public void render(GuiGraphicsExtractor g, SearchNode node, int x, int y, int size, boolean hovered) {
        if (size < 12) {
            FallbackTextRenderer.renderFallback(g, node, x, y, size);
            return;
        }
        String name = node == null ? "" : node.meta(SearchNodeKeys.PLAYER_HEAD_NAME, "");
        if (name.isBlank()) {
            FallbackTextRenderer.renderFallback(g, node, x, y, size);
            return;
        }
        RemotePlayer player = resolvePlayer(name);
        if (player == null) {
            FallbackTextRenderer.renderFallback(g, node, x, y, size);
            return;
        }
        float yRot = 180.0f;
        if (hovered) {
            yRot += (System.currentTimeMillis() % 3000L) / 3000.0f * 360.0f;
        }
        try {
            renderPlayer(g, x, y, size, player, yRot);
        } catch (RuntimeException e) {
            FallbackTextRenderer.renderFallback(g, node, x, y, size);
        }
    }

    @Override
    public List<Component> getTooltip(SearchNode node) {
        return List.of();
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(SearchNode node) {
        return Optional.empty();
    }

    @Override
    public void invalidate() {
        PLAYER_CACHE.clear();
        PENDING_PROFILES.clear();
    }

    private static final class RenderOnlyPlayer extends RemotePlayer {
        private final Supplier<PlayerSkin> skinSupplier;

        private RenderOnlyPlayer(ClientLevel level, GameProfile profile) {
            super(level, profile);
            this.skinSupplier = Minecraft.getInstance().getSkinManager().createLookup(profile, false);
        }

        @Override
        public PlayerSkin getSkin() {
            return skinSupplier.get();
        }
    }
}
