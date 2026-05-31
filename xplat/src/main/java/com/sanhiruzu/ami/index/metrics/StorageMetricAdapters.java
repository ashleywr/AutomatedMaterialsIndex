package com.sanhiruzu.ami.index.metrics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * Ordered registry for storage metric adapters.
 * <p>
 * Capacity should come from runtime components/capabilities or exact platform
 * APIs. Avoid id-to-slot tables for modded containers; configs and datapacks can
 * change those values outside AMI.
 */
public final class StorageMetricAdapters {
    private static final List<StorageMetricAdapter> ADAPTERS = new ArrayList<>();

    static {
        register(new VanillaStorageMetricAdapter());
    }

    private StorageMetricAdapters() {
    }

    private static synchronized void register(StorageMetricAdapter adapter) {
        ADAPTERS.add(adapter);
    }

    public static synchronized OptionalLong estimate(ItemStack stack, ResourceLocation id, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level) {
        for (StorageMetricAdapter adapter : ADAPTERS) {
            OptionalLong estimate = adapter.estimate(stack, id, level);
            if (estimate.isPresent()) return estimate;
        }
        return OptionalLong.empty();
    }
}
