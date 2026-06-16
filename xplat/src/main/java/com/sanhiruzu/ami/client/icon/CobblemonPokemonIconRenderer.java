package com.sanhiruzu.ami.client.icon;

import com.mojang.blaze3d.platform.NativeImage;
import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.compat.ReflectiveCompat;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
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
    private static final Map<Identifier, Object> STATE_CACHE = new HashMap<>();
    private static final Map<Identifier, Optional<Identifier>> SPRITE_CACHE = new HashMap<>();
    private static final Map<Identifier, Optional<Identifier>> GENERATED_PROFILE_SPRITES = new HashMap<>();
    private static final Set<Identifier> FAILED_PORTRAIT_RENDER = new HashSet<>();
    private static final Set<Identifier> FAILED_PROFILE_RENDER = new HashSet<>();
    private static final Set<Identifier> LOGGED_PORTRAIT_FAILURES = new HashSet<>();
    private static final Set<Identifier> LOGGED_PROFILE_FAILURES = new HashSet<>();
    private static final Set<Identifier> LOGGED_BLANK_PROFILE_CAPTURES = new HashSet<>();
    private static final int MAX_BLANK_CAPTURE_LOGS = 8;
    private static final Set<Identifier> BROKEN_PROFILE_RENDER = Set.of(
            Identifier.fromNamespaceAndPath("cobblemon", "coalossal")
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

    static void render(GuiGraphicsExtractor g, SearchNode node, int x, int y, int size, boolean hovered) {
        Identifier speciesId = speciesId(node);
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

    private static boolean canUseProfileRenderer(Identifier speciesId) {
        if (BROKEN_PROFILE_RENDER.contains(speciesId) || FAILED_PROFILE_RENDER.contains(speciesId)) {
            return false;
        }
        return true;
    }

    private static boolean tryRenderPortrait(GuiGraphicsExtractor g, Identifier speciesId, int x, int y, int size) {
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

        return false;
    }

    private static boolean tryRenderSprite(GuiGraphicsExtractor g, SearchNode node, Identifier speciesId, int x, int y, int size) {
        Optional<Identifier> sprite = SPRITE_CACHE.computeIfAbsent(speciesId, ignored -> findAndRegisterSprite(node, speciesId));
        if (sprite.isEmpty()) {
            return false;
        }

        g.blit(sprite.get(), x, y, x + size, y + size, 0.0f, 1.0f, 0.0f, 1.0f);
        return true;
    }

    private static boolean tryRenderCachedProfileSprite(GuiGraphicsExtractor g, Identifier speciesId, int x, int y, int size) {
        if (!GENERATED_PROFILE_SPRITES.containsKey(speciesId)) {
            Optional<Identifier> cached = loadGeneratedProfileTexture(speciesId);
            if (cached.isEmpty()) {
                cached = generateProfileTexture(speciesId);
            }
            GENERATED_PROFILE_SPRITES.put(speciesId, cached);
        }

        Optional<Identifier> texture = GENERATED_PROFILE_SPRITES.get(speciesId);
        if (texture == null || texture.isEmpty()) {
            return false;
        }

        g.blit(texture.get(), x, y, x + size, y + size, 0.0f, 1.0f, 0.0f, 1.0f);
        return true;
    }

    private static Optional<Identifier> loadGeneratedProfileTexture(Identifier speciesId) {
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

    private static Optional<Identifier> generateProfileTexture(Identifier speciesId) {
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

    private static NativeImage renderProfileToImage(Identifier speciesId) {
        return null;
    }

    private static Identifier registerGeneratedProfileTexture(Identifier speciesId, NativeImage image) {
        Identifier textureKey = generatedTextureKey(speciesId);
        Minecraft.getInstance().getTextureManager().release(textureKey);
        Minecraft.getInstance().getTextureManager().register(textureKey, new DynamicTexture(() -> "ami_cobblemon_icon", image));
        return textureKey;
    }

    private static void releaseGeneratedTextures() {
        var textureManager = Minecraft.getInstance().getTextureManager();
        for (Optional<Identifier> texture : GENERATED_PROFILE_SPRITES.values()) {
            texture.ifPresent(textureManager::release);
        }
    }

    private static void releaseSpriteTextures() {
        var textureManager = Minecraft.getInstance().getTextureManager();
        for (Optional<Identifier> texture : SPRITE_CACHE.values()) {
            texture.ifPresent(textureManager::release);
        }
    }

    private static Identifier generatedTextureKey(Identifier speciesId) {
        return Services.PLATFORM.rl("ami", "generated_cobblemon_icon/"
                + speciesId.getNamespace() + "/" + speciesId.getPath().replace('/', '_'));
    }

    private static Path generatedProfilePath(Identifier speciesId) {
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

    private static Optional<Identifier> findAndRegisterSprite(SearchNode node, Identifier speciesId) {
        String species = speciesId.getPath();
        String dexSpecies = dexSpeciesName(node, species);
        Identifier[] candidates = {
                Identifier.fromNamespaceAndPath("xaerominimap", "entity/icon/sprite/regular/" + dexSpecies + ".png"),
                Identifier.fromNamespaceAndPath("xaerominimap", "entity/icon/sprite/regular/" + species + ".png"),
                Identifier.fromNamespaceAndPath("xaerominimap", "entity/icon/sprite/cobblemon/" + dexSpecies + ".png"),
                Identifier.fromNamespaceAndPath("xaerominimap", "entity/icon/sprite/cobblemon/" + species + ".png"),
                Identifier.fromNamespaceAndPath("journeymap", "icon/entity/2d/cobblemon/" + dexSpecies + ".png"),
                Identifier.fromNamespaceAndPath("journeymap", "icon/entity/2d/cobblemon/" + species + ".png")
        };

        var resources = Minecraft.getInstance().getResourceManager();
        for (Identifier candidate : candidates) {
            Optional<Identifier> texture = resources.getResource(candidate).flatMap(resource -> {
                try (InputStream in = resource.open()) {
                    NativeImage image = normalizeIcon(NativeImage.read(in));
                    if (isBlankOrBlack(image)) {
                        image.close();
                        return Optional.empty();
                    }
                    Identifier textureKey = resourcePackSpriteTextureKey(speciesId);
                    Minecraft.getInstance().getTextureManager().release(textureKey);
                    Minecraft.getInstance().getTextureManager().register(textureKey, new DynamicTexture(() -> "ami_cobblemon_sprite", image));
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

    private static Identifier resourcePackSpriteTextureKey(Identifier speciesId) {
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

    private static boolean tryRenderProfile(GuiGraphicsExtractor g, Identifier speciesId, int x, int y, int size, boolean hovered, boolean cacheState) {
        return false;
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
                    Identifier.class,
                    Object.class,
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
                    Identifier.class,
                    Set.class,
                    Object.class,
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
                    Object.class,
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
                        Identifier.class
                ).orElseThrow();

                portraitApi = new PortraitApi(drawPortrait.get(), instance.get(null), getByIdentifier, null);
                logPortraitApiMode("species");
                return portraitApi;
            }

            Method drawPosablePortrait = ReflectiveCompat.findMethod(
                    guiUtils,
                    "drawPosablePortrait",
                    Identifier.class,
                    Object.class,
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

    private static Identifier speciesId(SearchNode node) {
        String species = node.meta(SearchNodeKeys.POKEMON_SPECIES, "");
        if (!species.isBlank()) {
            Identifier parsed = Identifier.tryParse(species);
            if (parsed != null) {
                return parsed;
            }
        }

        if ("cobblemon".equals(node.id().getNamespace()) && node.id().getPath().startsWith("species/")) {
            return Identifier.fromNamespaceAndPath("cobblemon", node.id().getPath().substring("species/".length()));
        }

        return null;
    }

    private static void renderBadge(GuiGraphicsExtractor g, SearchNode node, int x, int y, int size) {
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
                .getOptional(Identifier.fromNamespaceAndPath("cobblemon", "poke_ball"))
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
        if (!ball.isEmpty() && size >= 14) {
            g.pose().pushMatrix();
            g.pose().translate(x + size / 2.0f, y + size / 2.0f);
            float scale = size / 18.0f;
            g.pose().scale(scale, scale);
            g.item(ball, -8, -8);
            g.pose().popMatrix();
            return;
        }

        String letter = node.displayName().isEmpty() ? "?" : node.displayName().substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        var font = Minecraft.getInstance().font;
        int textX = x + (size - font.width(letter)) / 2;
        int textY = y + (size - font.lineHeight) / 2;
        g.text(font, letter, textX, textY, AMITheme.WHITE, false);
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
                int pixel = image.getPixel(x, y);
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
                    normalized.setPixel(offsetX + x, offsetY + y, scaled.getPixel(x, y));
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
                int alpha = (image.getPixel(x, y) >>> 24) & 0xFF;
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
        private Object newState(Identifier ignored) {
            try {
                return stateCtor.newInstance();
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }

        private Object[] arguments(Identifier speciesId, Object pose, Quaternionf rotation, Object state) {
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
        private Object species(Identifier speciesId) {
            if (getByIdentifier == null) {
                return speciesId;
            }
            try {
                return getByIdentifier.invoke(pokemonSpecies, speciesId);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }

        private Object[] arguments(Object species, Object pose) throws ReflectiveOperationException {
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

    private static void logPortraitFailure(Identifier speciesId, Throwable e) {
        if (!LOGGED_PORTRAIT_FAILURES.add(speciesId)) return;
        AmiCore.LOGGER.warn("AMI Cobblemon 3D Pokémon icons: failed to render portrait {}", speciesId, e);
    }

    private static void logProfileFailure(Identifier speciesId, Throwable e) {
        if (!LOGGED_PROFILE_FAILURES.add(speciesId)) return;
        AmiCore.LOGGER.warn("AMI Cobblemon 3D Pokémon icons: failed to render {}", speciesId, e);
    }

    private static void logBlankProfileCapture(Identifier speciesId) {
        if (!LOGGED_BLANK_PROFILE_CAPTURES.add(speciesId)) return;
        if (blankProfileCaptureLogCount < MAX_BLANK_CAPTURE_LOGS) {
            AmiCore.LOGGER.warn("AMI Cobblemon 3D Pokémon icons: profile capture was blank for {}; using live render fallback", speciesId);
        } else if (blankProfileCaptureLogCount == MAX_BLANK_CAPTURE_LOGS) {
            AmiCore.LOGGER.warn("AMI Cobblemon 3D Pokémon icons: suppressing further blank profile capture logs");
        }
        blankProfileCaptureLogCount++;
    }
}
