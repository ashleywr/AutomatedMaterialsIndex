package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.ResourceLocation;

/**
 * Debug-only policy for experimenting with known hot items without changing
 * default indexing behavior for all users.
 */
public final class IndexingHotItemPolicy {
    private static final boolean DEFER_FACADE_ITEMS =
            Boolean.getBoolean("ami.debug.deferFacadeItems");
    private static final boolean FAST_FACADE_INDEX =
            Boolean.getBoolean("ami.debug.fastFacadeIndex");

    private IndexingHotItemPolicy() {
    }

    public static boolean shouldDeferUntilTail(ResourceLocation id) {
        return DEFER_FACADE_ITEMS && isFacadeLike(id);
    }

    public static boolean shouldCollapseCreativeStacks(ResourceLocation id) {
        return shouldUseFastFacadeIndex(id);
    }

    public static boolean shouldUseFastFacadeIndex(ResourceLocation id) {
        return FAST_FACADE_INDEX && !AmiConfig.devMode && isFacadeLike(id);
    }

    public static String cacheKeyFragment() {
        return "_deferFacadeItems=" + DEFER_FACADE_ITEMS
                + "_fastFacadeIndex=" + FAST_FACADE_INDEX;
    }

    public static boolean isFacadeLike(ResourceLocation id) {
        if (id == null) return false;
        String namespace = id.getNamespace();
        String path = id.getPath();
        return (("ae2".equals(namespace) || "appliedenergistics2".equals(namespace))
                && "facade".equals(path))
                || path.contains("facade");
    }
}
