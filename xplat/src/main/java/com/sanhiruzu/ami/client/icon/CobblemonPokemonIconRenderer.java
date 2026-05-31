package com.sanhiruzu.ami.client.icon;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.compat.ReflectiveCompat;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CobblemonPokemonIconRenderer {
    private static final Map<ResourceLocation, Object> STATE_CACHE = new HashMap<>();
    private static final Set<ResourceLocation> FAILED_PROFILE_RENDER = new HashSet<>();
    private static ProfileApi profileApi;
    private static boolean profileApiUnavailable;

    private CobblemonPokemonIconRenderer() {
    }

    static void render(GuiGraphics g, SearchNode node, int x, int y, int size, boolean hovered) {
        ResourceLocation speciesId = speciesId(node);
        if (speciesId != null && !FAILED_PROFILE_RENDER.contains(speciesId) && tryRenderProfile(g, speciesId, x, y, size, hovered)) {
            return;
        }

        renderBadge(g, node, x, y, size);
    }

    public static void invalidate() {
        STATE_CACHE.clear();
        FAILED_PROFILE_RENDER.clear();
    }

    private static boolean tryRenderProfile(GuiGraphics g, ResourceLocation speciesId, int x, int y, int size, boolean hovered) {
        ProfileApi api = profileApi();
        if (api == null) {
            return false;
        }

        float[] shaderColor = RenderSystem.getShaderColor();
        float savedRed = shaderColor[0];
        float savedGreen = shaderColor[1];
        float savedBlue = shaderColor[2];
        float savedAlpha = shaderColor[3];
        g.pose().pushPose();
        try {
            Object state = STATE_CACHE.computeIfAbsent(speciesId, api::newState);
            if (state == null) {
                FAILED_PROFILE_RENDER.add(speciesId);
                return false;
            }

            float spin = hovered ? (System.currentTimeMillis() % 3000L) / 3000.0f * 360.0f : 30.0f;
            Quaternionf rotation = new Quaternionf().rotationXYZ((float) Math.toRadians(13.0f), (float) Math.toRadians(spin), 0.0f);

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);

            g.pose().translate(x + size / 2.0f, y + size * 0.9f, 180.0f);
            g.pose().scale(size / 42.0f, size / 42.0f, size / 42.0f);
            api.drawProfilePokemon.invoke(
                    null,
                    speciesId,
                    g.pose(),
                    rotation,
                    api.profilePose,
                    state,
                    0.0f,
                    20.0f,
                    false,
                    false,
                    false,
                    1.0f,
                    1.0f,
                    1.0f,
                    1.0f,
                    0.0f,
                    0.0f
            );
            return true;
        } catch (Throwable e) {
            FAILED_PROFILE_RENDER.add(speciesId);
            return false;
        } finally {
            g.pose().popPose();
            RenderSystem.setShaderColor(savedRed, savedGreen, savedBlue, savedAlpha);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private static ProfileApi profileApi() {
        if (profileApiUnavailable) {
            return null;
        }
        if (profileApi != null) {
            return profileApi;
        }

        try {
            Class<?> guiUtils = ReflectiveCompat.findClass("com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt").orElseThrow();
            Class<?> poseType = ReflectiveCompat.findClass("com.cobblemon.mod.common.entity.PoseType").orElseThrow();
            Class<?> posableState = ReflectiveCompat.findClass("com.cobblemon.mod.common.client.render.models.blockbench.PosableState").orElseThrow();
            Class<?> floatingState = ReflectiveCompat.findClass("com.cobblemon.mod.common.client.render.models.blockbench.FloatingState").orElseThrow();

            Method draw = ReflectiveCompat.findMethod(
                    guiUtils,
                    "drawProfilePokemon",
                    ResourceLocation.class,
                    PoseStack.class,
                    Quaternionf.class,
                    poseType,
                    posableState,
                    float.class,
                    float.class,
                    boolean.class,
                    boolean.class,
                    boolean.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class
            ).orElseThrow();
            Constructor<?> stateCtor = ReflectiveCompat.findConstructor(floatingState).orElseThrow();
            Object profilePose = Enum.valueOf(poseType.asSubclass(Enum.class), "PROFILE");

            profileApi = new ProfileApi(draw, stateCtor, profilePose);
            return profileApi;
        } catch (Throwable e) {
            profileApiUnavailable = true;
            return null;
        }
    }

    private static ResourceLocation speciesId(SearchNode node) {
        String species = node.meta(SearchNodeKeys.POKEMON_SPECIES, "");
        if (!species.isBlank()) {
            ResourceLocation parsed = ResourceLocation.tryParse(species);
            if (parsed != null) {
                return parsed;
            }
        }

        if ("cobblemon".equals(node.id().getNamespace()) && node.id().getPath().startsWith("species/")) {
            return ResourceLocation.fromNamespaceAndPath("cobblemon", node.id().getPath().substring("species/".length()));
        }

        return null;
    }

    private static void renderBadge(GuiGraphics g, SearchNode node, int x, int y, int size) {
        String primaryType = node.meta(SearchNodeKeys.POKEMON_PRIMARY_TYPE, node.meta(SearchNodeKeys.POKEMON_TYPE, ""));
        int bg = typeColor(firstToken(primaryType));
        String secondaryType = node.meta(SearchNodeKeys.POKEMON_SECONDARY_TYPE, "");
        int border = secondaryType.isBlank() ? 0xFFFFFFFF : typeColor(firstToken(secondaryType));

        g.fill(x, y, x + size, y + size, bg);
        g.fill(x, y, x + size, y + 1, border);
        g.fill(x, y + size - 1, x + size, y + size, border);
        g.fill(x, y, x + 1, y + size, border);
        g.fill(x + size - 1, y, x + size, y + size, border);

        ItemStack ball = BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.fromNamespaceAndPath("cobblemon", "poke_ball"))
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
        if (!ball.isEmpty() && size >= 14) {
            g.pose().pushPose();
            g.pose().translate(x + size / 2.0f, y + size / 2.0f, 0);
            float scale = size / 18.0f;
            g.pose().scale(scale, scale, 1.0f);
            g.renderItem(ball, -8, -8);
            g.pose().popPose();
            return;
        }

        String letter = node.displayName().isEmpty() ? "?" : node.displayName().substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        var font = Minecraft.getInstance().font;
        int textX = x + (size - font.width(letter)) / 2;
        int textY = y + (size - font.lineHeight) / 2;
        g.drawString(font, letter, textX, textY, AMITheme.WHITE, false);
    }

    private static String firstToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        int comma = raw.indexOf(',');
        int space = raw.indexOf(' ');
        int end = raw.length();
        if (comma >= 0) end = Math.min(end, comma);
        if (space >= 0) end = Math.min(end, space);
        return raw.substring(0, end).toLowerCase(java.util.Locale.ROOT);
    }

    private static int typeColor(String type) {
        return 0xFF000000 | com.sanhiruzu.ami.client.tooltip.PokemonTypeBadgesComponent.color(type);
    }

    private record ProfileApi(Method drawProfilePokemon, Constructor<?> stateCtor, Object profilePose) {
        private Object newState(ResourceLocation ignored) {
            try {
                return stateCtor.newInstance();
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
    }
}
