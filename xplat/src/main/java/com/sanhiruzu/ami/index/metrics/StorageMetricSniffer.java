package com.sanhiruzu.ami.index.metrics;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalLong;

/**
 * Estimates storage capacity as Equivalent Stack Metric: total item units.
 * <p>
 * The generic rules stay conservative. Mod-specific storage systems should be
 * added as explicit adapters when their APIs or stable item ids are available.
 */
public final class StorageMetricSniffer {
    private StorageMetricSniffer() {
    }

    public static OptionalLong estimate(ItemStack stack, Identifier id, @Nullable Level level) {
        if (stack == null || stack.isEmpty()) return OptionalLong.empty();
        return StorageMetricAdapters.estimate(stack, id, level);
    }
}
