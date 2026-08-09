package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.index.NodeType;

/**
 * Central factory: maps NodeType → IIconRenderer.
 * All renderer instances are singletons held here so caches survive across frames.
 * invalidateAll() is called on world unload and on every resource reload (see
 * ThemeResourceLoader), so packs added/changed/removed mid-session take effect promptly.
 */
public final class RendererRegistry {

    public static final ItemIconRenderer ITEM = new ItemIconRenderer();
    public static final FluidIconRenderer FLUID = new FluidIconRenderer();
    public static final RecipeViewerIngredientRenderer INGREDIENT = new RecipeViewerIngredientRenderer();
    public static final EntityIconRenderer ENTITY = new EntityIconRenderer();
    public static final PlayerFaceRenderer PLAYER = new PlayerFaceRenderer();
    public static final PlayerModelRenderer PLAYER_MODEL = new PlayerModelRenderer();
    public static final ProxyBlockRenderer PROXY = new ProxyBlockRenderer();
    public static final FallbackTextRenderer FALLBACK = new FallbackTextRenderer();

    private RendererRegistry() {
    }

    public static IIconRenderer get(NodeType type) {
        return switch (type) {
            case ITEM -> ITEM;
            case FLUID -> FLUID;
            case INGREDIENT -> INGREDIENT;
            case ENTITY -> ENTITY;
            case PLAYER -> PLAYER;
            case BIOME, STRUCTURE -> PROXY;
            default -> FALLBACK;
        };
    }

    public static void invalidateAll() {
        ITEM.invalidate();
        INGREDIENT.invalidate();
        ENTITY.invalidate();
        PLAYER_MODEL.invalidate();
        PROXY.invalidate();
        // PlayerFaceRenderer has no local cache (it reads from live PlayerInfo); no-op.
    }
}
