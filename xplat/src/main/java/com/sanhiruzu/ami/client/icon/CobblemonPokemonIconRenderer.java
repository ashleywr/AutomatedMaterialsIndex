package com.sanhiruzu.ami.client.icon;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.RenderStateSnapshot;
import com.sanhiruzu.ami.compat.ReflectiveCompat;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CobblemonPokemonIconRenderer {
    private static final int GENERATED_SPRITE_SIZE = 64;
    private static final int ICON_CONTENT_SIZE = 62;
    private static final String GENERATED_SPRITE_CACHE_VERSION = "v3";
    private static final Map<ResourceLocation, Object> STATE_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Optional<ResourceLocation>> SPRITE_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Optional<ResourceLocation>> GENERATED_PROFILE_SPRITES = new HashMap<>();
    private static final Set<ResourceLocation> FAILED_PORTRAIT_RENDER = new HashSet<>();
    private static final Set<ResourceLocation> FAILED_PROFILE_RENDER = new HashSet<>();
    private static final Set<ResourceLocation> LOGGED_PORTRAIT_FAILURES = new HashSet<>();
    private static final Set<ResourceLocation> LOGGED_PROFILE_FAILURES = new HashSet<>();
    private static final Set<ResourceLocation> LOGGED_BLANK_PROFILE_CAPTURES = new HashSet<>();
    private static final int MAX_BLANK_CAPTURE_LOGS = 8;
    private static final Set<ResourceLocation> BROKEN_PROFILE_RENDER = Set.of(
            ResourceLocation.fromNamespaceAndPath("cobblemon", "coalossal")
    );
    private static PortraitApi portraitApi;
    private static ProfileApi profileApi;
    private static boolean portraitApiUnavailable;
    private static boolean profileApiUnavailable;
    private static boolean loggedPortraitApiUnavailable;
    private static boolean loggedPortraitApiMode;
    private static boolean loggedProfileApiUnavailable;
    private static boolean loggedProfileApiMode;
    private static int blankProfileCaptureLogCount;

    private CobblemonPokemonIconRenderer() {
    }

    static void render(GuiGraphics g, SearchNode node, int x, int y, int size, boolean hovered) {
        ResourceLocation speciesId = speciesId(node);
        if (speciesId != null
                && AmiConfig.useCobblemonResourcePackSprites
                && tryRenderSprite(g, node, speciesId, x, y, size)) {
            return;
        }

        if (speciesId != null
                && AmiConfig.renderCobblemon3dPokemonIcons
                && isImplementedPokemon(node)) {
            if (tryRenderCachedProfileSprite(g, speciesId, x, y, size)) {
                return;
            }
            if (tryRenderPortrait(g, speciesId, x, y, size)) {
                return;
            }
            renderBadge(g, node, x, y, size);
            tryRenderProfile(g, speciesId, x, y, size, hovered, true);
            return;
        }

        renderBadge(g, node, x, y, size);
    }

    public static void invalidate() {
        STATE_CACHE.clear();
        releaseSpriteTextures();
        SPRITE_CACHE.clear();
        releaseGeneratedTextures();
        GENERATED_PROFILE_SPRITES.clear();
        FAILED_PORTRAIT_RENDER.clear();
        FAILED_PROFILE_RENDER.clear();
        LOGGED_PORTRAIT_FAILURES.clear();
        LOGGED_PROFILE_FAILURES.clear();
        LOGGED_BLANK_PROFILE_CAPTURES.clear();
        blankProfileCaptureLogCount = 0;
    }

    private static boolean isImplementedPokemon(SearchNode node) {
        return Boolean.parseBoolean(node.meta(SearchNodeKeys.POKEMON_IMPLEMENTED, "true"));
    }

    private static boolean canUseProfileRenderer(ResourceLocation speciesId) {
        if (BROKEN_PROFILE_RENDER.contains(speciesId) || FAILED_PROFILE_RENDER.contains(speciesId)) {
            return false;
        }
        return true;
    }

    private static boolean tryRenderPortrait(GuiGraphics g, ResourceLocation speciesId, int x, int y, int size) {
        if (FAILED_PORTRAIT_RENDER.contains(speciesId)) {
            return false;
        }

        PortraitApi api = portraitApi();
        if (api == null) {
            return false;
        }

        Object species = api.species(speciesId);
        if (species == null) {
            FAILED_PORTRAIT_RENDER.add(speciesId);
            return false;
        }

        g.pose().pushPose();
        try {
            IconRenderState.render3dIcon(g, () -> {
                try {
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    // Cobbledex renders its 16px collection entries at x+8,y-2 with a 0.5x pose scale.
                    g.pose().translate(x + size * 0.5f, y - size * 0.125f, 0.0f);
                    float slotScale = size / 32.0f;
                    g.pose().scale(slotScale, slotScale, 1.0f);
                    api.drawPortraitPokemon.invoke(null, api.arguments(species, g.pose()));
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            });
            return true;
        } catch (Throwable e) {
            FAILED_PORTRAIT_RENDER.add(speciesId);
            logPortraitFailure(speciesId, e);
            return false;
        } finally {
            g.pose().popPose();
        }
    }

    private static boolean tryRenderSprite(GuiGraphics g, SearchNode node, ResourceLocation speciesId, int x, int y, int size) {
        Optional<ResourceLocation> sprite = SPRITE_CACHE.computeIfAbsent(speciesId, ignored -> findAndRegisterSprite(node, speciesId));
        if (sprite.isEmpty()) {
            return false;
        }

        RenderStateSnapshot state = RenderStateSnapshot.capture();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(sprite.get(), x, y, size, size, 0.0f, 0.0f, 64, 64, 64, 64);
        } finally {
            state.restore();
        }
        return true;
    }

    private static boolean tryRenderCachedProfileSprite(GuiGraphics g, ResourceLocation speciesId, int x, int y, int size) {
        if (!GENERATED_PROFILE_SPRITES.containsKey(speciesId)) {
            Optional<ResourceLocation> cached = loadGeneratedProfileTexture(speciesId);
            if (cached.isEmpty()) {
                g.flush();
                cached = generateProfileTexture(speciesId);
            }
            GENERATED_PROFILE_SPRITES.put(speciesId, cached);
        }

        Optional<ResourceLocation> texture = GENERATED_PROFILE_SPRITES.get(speciesId);
        if (texture == null || texture.isEmpty()) {
            return false;
        }

        RenderStateSnapshot state = RenderStateSnapshot.capture();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(texture.get(), x, y, size, size, 0.0f, 0.0f,
                    GENERATED_SPRITE_SIZE, GENERATED_SPRITE_SIZE, GENERATED_SPRITE_SIZE, GENERATED_SPRITE_SIZE);
        } finally {
            state.restore();
        }
        return true;
    }

    private static Optional<ResourceLocation> loadGeneratedProfileTexture(ResourceLocation speciesId) {
        Path file = generatedProfilePath(speciesId);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }

        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = normalizeIcon(NativeImage.read(in));
            if (isBlankOrBlack(image)) {
                image.close();
                return Optional.empty();
            }
            return Optional.of(registerGeneratedProfileTexture(speciesId, image));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    private static Optional<ResourceLocation> generateProfileTexture(ResourceLocation speciesId) {
        NativeImage image;
        try {
            image = renderProfileToImage(speciesId);
        } catch (RuntimeException e) {
            FAILED_PROFILE_RENDER.add(speciesId);
            logProfileFailure(speciesId, e);
            return Optional.empty();
        }
        if (image == null || isBlankOrBlack(image)) {
            if (image != null) {
                image.close();
            }
            logBlankProfileCapture(speciesId);
            return Optional.empty();
        }
        image = normalizeIcon(image);

        try {
            Path file = generatedProfilePath(speciesId);
            Files.createDirectories(file.getParent());
            image.writeToFile(file);
        } catch (IOException ignored) {
            // Disk caching is best-effort; keep the in-memory texture for this session.
        }

        return Optional.of(registerGeneratedProfileTexture(speciesId, image));
    }

    private static NativeImage renderProfileToImage(ResourceLocation speciesId) {
        Minecraft mc = Minecraft.getInstance();
        RenderStateSnapshot state = RenderStateSnapshot.capture();
        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());

        RenderTarget rt = new RenderTarget(true) {
        };
        NativeImage image = null;
        try {
            rt.resize(GENERATED_SPRITE_SIZE, GENERATED_SPRITE_SIZE, Minecraft.ON_OSX);
            rt.setClearColor(0f, 0f, 0f, 0f);
            rt.clear(Minecraft.ON_OSX);
            rt.bindWrite(true);
            RenderSystem.disableScissor();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            GlStateManager._viewport(0, 0, GENERATED_SPRITE_SIZE, GENERATED_SPRITE_SIZE);
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(0, GENERATED_SPRITE_SIZE, GENERATED_SPRITE_SIZE, 0, -100, 3000),
                    VertexSorting.ORTHOGRAPHIC_Z);

            GuiGraphics cacheG = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            // Match ItemIconCache's Forge 1.20.x framebuffer compensation. Cobblemon's
            // profile renderer applies the current model-view matrix, so a GUI render
            // offset can otherwise push the offscreen model outside the capture frustum.
            float mvZ = RenderSystem.getModelViewMatrix().m32();
            if (mvZ != 0) {
                cacheG.pose().translate(0.0, 0.0, (double) -mvZ);
            }
            if (!tryRenderPortrait(cacheG, speciesId, 0, 0, GENERATED_SPRITE_SIZE)
                    && !tryRenderProfile(cacheG, speciesId, 0, 0, GENERATED_SPRITE_SIZE, false, false)) {
                return null;
            }
            cacheG.flush();
            image = Screenshot.takeScreenshot(rt);
            return image;
        } finally {
            mc.getMainRenderTarget().bindWrite(true);
            RenderSystem.setProjectionMatrix(savedProj, VertexSorting.ORTHOGRAPHIC_Z);
            state.restore();
            rt.destroyBuffers();
        }
    }

    private static ResourceLocation registerGeneratedProfileTexture(ResourceLocation speciesId, NativeImage image) {
        ResourceLocation textureKey = generatedTextureKey(speciesId);
        Minecraft.getInstance().getTextureManager().release(textureKey);
        Minecraft.getInstance().getTextureManager().register(textureKey, new DynamicTexture(image));
        return textureKey;
    }

    private static void releaseGeneratedTextures() {
        var textureManager = Minecraft.getInstance().getTextureManager();
        for (Optional<ResourceLocation> texture : GENERATED_PROFILE_SPRITES.values()) {
            texture.ifPresent(textureManager::release);
        }
    }

    private static void releaseSpriteTextures() {
        var textureManager = Minecraft.getInstance().getTextureManager();
        for (Optional<ResourceLocation> texture : SPRITE_CACHE.values()) {
            texture.ifPresent(textureManager::release);
        }
    }

    private static ResourceLocation generatedTextureKey(ResourceLocation speciesId) {
        return Services.PLATFORM.rl("ami", "generated_cobblemon_icon/"
                + speciesId.getNamespace() + "/" + speciesId.getPath().replace('/', '_'));
    }

    private static Path generatedProfilePath(ResourceLocation speciesId) {
        return Services.PLATFORM.getConfigDir()
                .resolve("ami")
                .resolve("generated-cobblemon-icons")
                .resolve(GENERATED_SPRITE_CACHE_VERSION)
                .resolve(safePathPart(speciesId.getNamespace()))
                .resolve(safePathPart(speciesId.getPath()) + ".png");
    }

    private static String safePathPart(String raw) {
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static Optional<ResourceLocation> findAndRegisterSprite(SearchNode node, ResourceLocation speciesId) {
        String species = speciesId.getPath();
        String dexSpecies = dexSpeciesName(node, species);
        ResourceLocation[] candidates = {
                ResourceLocation.fromNamespaceAndPath("xaerominimap", "entity/icon/sprite/regular/" + dexSpecies + ".png"),
                ResourceLocation.fromNamespaceAndPath("xaerominimap", "entity/icon/sprite/regular/" + species + ".png"),
                ResourceLocation.fromNamespaceAndPath("xaerominimap", "entity/icon/sprite/cobblemon/" + dexSpecies + ".png"),
                ResourceLocation.fromNamespaceAndPath("xaerominimap", "entity/icon/sprite/cobblemon/" + species + ".png"),
                ResourceLocation.fromNamespaceAndPath("journeymap", "icon/entity/2d/cobblemon/" + dexSpecies + ".png"),
                ResourceLocation.fromNamespaceAndPath("journeymap", "icon/entity/2d/cobblemon/" + species + ".png")
        };

        var resources = Minecraft.getInstance().getResourceManager();
        for (ResourceLocation candidate : candidates) {
            Optional<ResourceLocation> texture = resources.getResource(candidate).flatMap(resource -> {
                try (InputStream in = resource.open()) {
                    NativeImage image = normalizeIcon(NativeImage.read(in));
                    if (isBlankOrBlack(image)) {
                        image.close();
                        return Optional.empty();
                    }
                    ResourceLocation textureKey = resourcePackSpriteTextureKey(speciesId);
                    Minecraft.getInstance().getTextureManager().release(textureKey);
                    Minecraft.getInstance().getTextureManager().register(textureKey, new DynamicTexture(image));
                    return Optional.of(textureKey);
                } catch (IOException | RuntimeException e) {
                    return Optional.empty();
                }
            });
            if (texture.isPresent()) {
                return texture;
            }
        }
        return Optional.empty();
    }

    private static ResourceLocation resourcePackSpriteTextureKey(ResourceLocation speciesId) {
        return Services.PLATFORM.rl("ami", "cobblemon_resource_sprite/"
                + speciesId.getNamespace() + "/" + speciesId.getPath().replace('/', '_'));
    }

    private static String dexSpeciesName(SearchNode node, String species) {
        String dex = node.meta(SearchNodeKeys.POKEMON_DEX_NUMBER, "");
        try {
            return String.format(java.util.Locale.ROOT, "%04d_%s", Integer.parseInt(dex), species);
        } catch (NumberFormatException ignored) {
            return species;
        }
    }

    private static boolean tryRenderProfile(GuiGraphics g, ResourceLocation speciesId, int x, int y, int size, boolean hovered, boolean cacheState) {
        if (!canUseProfileRenderer(speciesId)) {
            return false;
        }

        ProfileApi api = profileApi();
        if (api == null) {
            return false;
        }

        g.pose().pushPose();
        try {
            Object state = cacheState ? STATE_CACHE.computeIfAbsent(speciesId, api::newState) : api.newState(speciesId);
            if (state == null) {
                FAILED_PROFILE_RENDER.add(speciesId);
                return false;
            }

            float spin = hovered ? (System.currentTimeMillis() % 3000L) / 3000.0f * 360.0f : 30.0f;
            Quaternionf rotation = new Quaternionf().rotationXYZ((float) Math.toRadians(13.0f), (float) Math.toRadians(spin), 0.0f);

            IconRenderState.render3dIcon(g, () -> {
                try {
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    g.pose().translate(x + size / 2.0f, y + size * 0.9f, 180.0f);
                    g.pose().scale(size / 42.0f, size / 42.0f, size / 42.0f);
                    api.drawProfilePokemon.invoke(
                            null,
                            api.arguments(speciesId, g.pose(), rotation, state)
                    );
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            });
            return true;
        } catch (Throwable e) {
            FAILED_PROFILE_RENDER.add(speciesId);
            logProfileFailure(speciesId, e);
            return false;
        } finally {
            g.pose().popPose();
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
            Class<?> posableState = firstClass(
                    "com.cobblemon.mod.common.client.render.models.blockbench.PosableState",
                    "com.cobblemon.mod.common.client.render.models.blockbench.PoseableEntityState"
            ).orElseThrow();
            Class<?> floatingState = firstClass(
                    "com.cobblemon.mod.common.client.render.models.blockbench.FloatingState",
                    "com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonFloatingState"
            ).orElseThrow();

            Optional<Method> currentDraw = ReflectiveCompat.findMethod(
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
            );
            Constructor<?> stateCtor = ReflectiveCompat.findConstructor(floatingState).orElseThrow();
            Object profilePose = Enum.valueOf(poseType.asSubclass(Enum.class), "PROFILE");

            if (currentDraw.isPresent()) {
                profileApi = new ProfileApi(currentDraw.get(), stateCtor, profilePose, false);
                logProfileApiMode("current");
                return profileApi;
            }

            Method legacyDraw = ReflectiveCompat.findMethod(
                    guiUtils,
                    "drawProfilePokemon",
                    ResourceLocation.class,
                    Set.class,
                    PoseStack.class,
                    Quaternionf.class,
                    posableState,
                    float.class,
                    float.class
            ).orElseThrow();
            profileApi = new ProfileApi(legacyDraw, stateCtor, profilePose, true);
            logProfileApiMode("legacy");
            return profileApi;
        } catch (Throwable e) {
            profileApiUnavailable = true;
            logProfileApiUnavailable(e);
            return null;
        }
    }

    private static PortraitApi portraitApi() {
        if (portraitApiUnavailable) {
            return null;
        }
        if (portraitApi != null) {
            return portraitApi;
        }

        try {
            Class<?> guiUtils = ReflectiveCompat.findClass("com.cobblemon.mod.common.api.gui.GuiUtilsKt").orElseThrow();
            Class<?> pokemonSpecies = ReflectiveCompat.findClass("com.cobblemon.mod.common.api.pokemon.PokemonSpecies").orElseThrow();
            Class<?> species = ReflectiveCompat.findClass("com.cobblemon.mod.common.pokemon.Species").orElseThrow();
            Class<?> posableState = firstClass(
                    "com.cobblemon.mod.common.client.render.models.blockbench.PosableState",
                    "com.cobblemon.mod.common.client.render.models.blockbench.PoseableEntityState"
            ).orElseThrow();

            Optional<Method> drawPortrait = ReflectiveCompat.findMethod(
                    guiUtils,
                    "drawPortraitPokemon",
                    species,
                    Set.class,
                    PoseStack.class,
                    float.class,
                    boolean.class,
                    posableState,
                    float.class
            );
            if (drawPortrait.isPresent()) {
                Field instance = pokemonSpecies.getField("INSTANCE");
                Method getByIdentifier = ReflectiveCompat.findMethod(
                        pokemonSpecies,
                        "getByIdentifier",
                        ResourceLocation.class
                ).orElseThrow();

                portraitApi = new PortraitApi(drawPortrait.get(), instance.get(null), getByIdentifier, null);
                logPortraitApiMode("species");
                return portraitApi;
            }

            Method drawPosablePortrait = ReflectiveCompat.findMethod(
                    guiUtils,
                    "drawPosablePortrait",
                    ResourceLocation.class,
                    PoseStack.class,
                    posableState,
                    float.class
            ).orElseThrow();
            Class<?> floatingState = firstClass(
                    "com.cobblemon.mod.common.client.render.models.blockbench.FloatingState",
                    "com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonFloatingState"
            ).orElseThrow();
            Constructor<?> stateCtor = ReflectiveCompat.findConstructor(floatingState).orElseThrow();

            portraitApi = new PortraitApi(drawPosablePortrait, null, null, stateCtor);
            logPortraitApiMode("posable");
            return portraitApi;
        } catch (Throwable e) {
            portraitApiUnavailable = true;
            logPortraitApiUnavailable(e);
            return null;
        }
    }

    private static Optional<Class<?>> firstClass(String... classNames) {
        for (String className : classNames) {
            Optional<Class<?>> type = ReflectiveCompat.findClass(className);
            if (type.isPresent()) {
                return type;
            }
        }
        return Optional.empty();
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
            g.pose().translate(x + size / 2.0f, y + size / 2.0f, com.sanhiruzu.ami.client.overlay.OverlayLayers.SCREEN);
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

    private static boolean isBlankOrBlack(NativeImage image) {
        boolean sawVisible = false;
        boolean sawNonBlack = false;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                int alpha = (pixel >>> 24) & 0xFF;
                if (alpha == 0) {
                    continue;
                }
                sawVisible = true;
                int red = pixel & 0xFF;
                int green = (pixel >>> 8) & 0xFF;
                int blue = (pixel >>> 16) & 0xFF;
                if (red > 8 || green > 8 || blue > 8) {
                    sawNonBlack = true;
                }
            }
        }
        return !sawVisible || !sawNonBlack;
    }

    private static NativeImage normalizeIcon(NativeImage source) {
        Bounds bounds = visibleBounds(source);
        if (bounds == null) {
            return source;
        }

        int sourceWidth = bounds.width();
        int sourceHeight = bounds.height();
        int targetWidth;
        int targetHeight;
        if (sourceWidth >= sourceHeight) {
            targetWidth = ICON_CONTENT_SIZE;
            targetHeight = Math.max(1, Math.round(ICON_CONTENT_SIZE * (sourceHeight / (float) sourceWidth)));
        } else {
            targetHeight = ICON_CONTENT_SIZE;
            targetWidth = Math.max(1, Math.round(ICON_CONTENT_SIZE * (sourceWidth / (float) sourceHeight)));
        }

        NativeImage scaled = new NativeImage(targetWidth, targetHeight, false);
        NativeImage normalized = new NativeImage(GENERATED_SPRITE_SIZE, GENERATED_SPRITE_SIZE, false);
        try {
            source.resizeSubRectTo(bounds.left(), bounds.top(), sourceWidth, sourceHeight, scaled);
            int offsetX = (GENERATED_SPRITE_SIZE - targetWidth) / 2;
            int offsetY = (GENERATED_SPRITE_SIZE - targetHeight) / 2;
            for (int y = 0; y < targetHeight; y++) {
                for (int x = 0; x < targetWidth; x++) {
                    normalized.setPixelRGBA(offsetX + x, offsetY + y, scaled.getPixelRGBA(x, y));
                }
            }
            return normalized;
        } finally {
            scaled.close();
            source.close();
        }
    }

    private static Bounds visibleBounds(NativeImage image) {
        int left = image.getWidth();
        int top = image.getHeight();
        int right = -1;
        int bottom = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getPixelRGBA(x, y) >>> 24) & 0xFF;
                if (alpha <= 8) {
                    continue;
                }
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
            }
        }

        if (right < left || bottom < top) {
            return null;
        }
        return new Bounds(left, top, right, bottom);
    }

    private record Bounds(int left, int top, int right, int bottom) {
        int width() {
            return right - left + 1;
        }

        int height() {
            return bottom - top + 1;
        }
    }

    private record ProfileApi(Method drawProfilePokemon, Constructor<?> stateCtor, Object profilePose, boolean legacy) {
        private Object newState(ResourceLocation ignored) {
            try {
                return stateCtor.newInstance();
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }

        private Object[] arguments(ResourceLocation speciesId, PoseStack pose, Quaternionf rotation, Object state) {
            if (legacy) {
                return new Object[]{
                        speciesId,
                        Set.of(),
                        pose,
                        rotation,
                        state,
                        0.0f,
                        20.0f
                };
            }
            return new Object[]{
                    speciesId,
                    pose,
                    rotation,
                    profilePose,
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
            };
        }
    }

    private record PortraitApi(
            Method drawPortraitPokemon,
            Object pokemonSpecies,
            Method getByIdentifier,
            Constructor<?> stateCtor
    ) {
        private Object species(ResourceLocation speciesId) {
            if (getByIdentifier == null) {
                return speciesId;
            }
            try {
                return getByIdentifier.invoke(pokemonSpecies, speciesId);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }

        private Object[] arguments(Object species, PoseStack pose) throws ReflectiveOperationException {
            if (getByIdentifier != null) {
                return new Object[]{
                        species,
                        Set.of(),
                        pose,
                        13.0f,
                        false,
                        null,
                        0.0f
                };
            }
            return new Object[]{
                    species,
                    pose,
                    stateCtor.newInstance(),
                    13.0f
            };
        }
    }

    private static void logPortraitApiMode(String mode) {
        if (loggedPortraitApiMode) return;
        loggedPortraitApiMode = true;
        AmiCore.LOGGER.info("AMI Cobblemon 3D Pokémon icons: using {} portrait renderer", mode);
    }

    private static void logPortraitApiUnavailable(Throwable e) {
        if (loggedPortraitApiUnavailable) return;
        loggedPortraitApiUnavailable = true;
        AmiCore.LOGGER.warn("AMI Cobblemon 3D Pokémon icons: portrait renderer API unavailable", e);
    }

    private static void logProfileApiMode(String mode) {
        if (loggedProfileApiMode) return;
        loggedProfileApiMode = true;
        AmiCore.LOGGER.info("AMI Cobblemon 3D Pokémon icons: using {} profile renderer", mode);
    }

    private static void logProfileApiUnavailable(Throwable e) {
        if (loggedProfileApiUnavailable) return;
        loggedProfileApiUnavailable = true;
        AmiCore.LOGGER.warn("AMI Cobblemon 3D Pokémon icons: profile renderer API unavailable", e);
    }

    private static void logPortraitFailure(ResourceLocation speciesId, Throwable e) {
        if (!LOGGED_PORTRAIT_FAILURES.add(speciesId)) return;
        AmiCore.LOGGER.warn("AMI Cobblemon 3D Pokémon icons: failed to render portrait {}", speciesId, e);
    }

    private static void logProfileFailure(ResourceLocation speciesId, Throwable e) {
        if (!LOGGED_PROFILE_FAILURES.add(speciesId)) return;
        AmiCore.LOGGER.warn("AMI Cobblemon 3D Pokémon icons: failed to render {}", speciesId, e);
    }

    private static void logBlankProfileCapture(ResourceLocation speciesId) {
        if (!LOGGED_BLANK_PROFILE_CAPTURES.add(speciesId)) return;
        if (blankProfileCaptureLogCount < MAX_BLANK_CAPTURE_LOGS) {
            AmiCore.LOGGER.warn("AMI Cobblemon 3D Pokémon icons: profile capture was blank for {}; using live render fallback", speciesId);
        } else if (blankProfileCaptureLogCount == MAX_BLANK_CAPTURE_LOGS) {
            AmiCore.LOGGER.warn("AMI Cobblemon 3D Pokémon icons: suppressing further blank profile capture logs");
        }
        blankProfileCaptureLogCount++;
    }
}
