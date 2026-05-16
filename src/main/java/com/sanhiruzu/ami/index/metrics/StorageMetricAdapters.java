package com.sanhiruzu.ami.index.metrics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * Ordered registry for storage metric adapters.
 *
 * Generic vanilla/capability rules should stay last. Mod-specific adapters can
 * be registered before them so they win when a storage system needs a custom
 * conversion model.
 */
public final class StorageMetricAdapters {
    private static final List<StorageMetricAdapter> ADAPTERS = new ArrayList<>();

    static {
        register(new VanillaStorageMetricAdapter());
        register(new SophisticatedBackpacksStorageMetricAdapter());
    }

    private StorageMetricAdapters() {
    }

    public static synchronized void register(StorageMetricAdapter adapter) {
        ADAPTERS.add(adapter);
    }

    public static synchronized OptionalLong estimate(ItemStack stack, ResourceLocation id) {
        for (StorageMetricAdapter adapter : ADAPTERS) {
            OptionalLong estimate = adapter.estimate(stack, id);
            if (estimate.isPresent()) return estimate;
        }
        return OptionalLong.empty();
    }
}
