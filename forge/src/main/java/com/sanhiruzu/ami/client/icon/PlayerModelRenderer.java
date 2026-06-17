package com.sanhiruzu.ami.client.icon;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.joml.Quaternionf;

import java.util.*;
import java.util.concurrent.CompletableFuture;

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
                if (profile != null && name.equalsIgnoreCase(profile.getName())) {
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
        GameProfile unresolved = new GameProfile((UUID) null, name);
        CompletableFuture<GameProfile> future = new CompletableFuture<>();
        PENDING_PROFILES.put(name, future);
        SkullBlockEntity.updateGameprofile(unresolved, profile -> future.complete(profile));
        return null;
    }

    private static RemotePlayer cache(String name, RemotePlayer player) {
        PLAYER_CACHE.put(name, player);
        return player;
    }

    private static void renderPlayer(GuiGraphics g, int x, int y, int size, RemotePlayer player, float yRot) {
        float savedBodyRot = player.yBodyRot;
        float savedYRot = player.getYRot();
        float savedXRot = player.getXRot();
        float savedHeadRotO = player.yHeadRotO;
        float savedHeadRot = player.yHeadRot;
        player.yBodyRot = yRot;
        player.setYRot(yRot);
        player.setXRot(0.0f);
        player.yHeadRot = player.getYRot();
        player.yHeadRotO = player.getYRot();
        int renderScale = Math.max(1, Math.round((size - 2.0f) / Math.max(0.1f, player.getBbHeight())));
        g.pose().pushPose();
        try {
            IconRenderState.render3dIcon(g, () -> InventoryScreen.renderEntityInInventory(
                    g,
                    x + size / 2,
                    y + size,
                    renderScale,
                    new Quaternionf().rotateZ((float) Math.PI),
                    new Quaternionf(),
                    player
            ));
        } finally {
            g.pose().popPose();
            player.yBodyRot = savedBodyRot;
            player.setYRot(savedYRot);
            player.setXRot(savedXRot);
            player.yHeadRotO = savedHeadRotO;
            player.yHeadRot = savedHeadRot;
        }
    }

    @Override
    public void render(GuiGraphics g, SearchNode node, int x, int y, int size, boolean hovered) {
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
        private RenderOnlyPlayer(ClientLevel level, GameProfile profile) {
            super(level, profile);
        }

        @Override
        public ResourceLocation getSkinTextureLocation() {
            return Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(getGameProfile());
        }

        @Override
        public String getModelName() {
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures =
                    Minecraft.getInstance().getSkinManager().getInsecureSkinInformation(getGameProfile());
            MinecraftProfileTexture skin = textures.get(MinecraftProfileTexture.Type.SKIN);
            return skin != null && "slim".equals(skin.getMetadata("model")) ? "slim" : "default";
        }
    }
}
