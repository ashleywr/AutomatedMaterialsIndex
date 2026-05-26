package com.sanhiruzu.ami.index.metrics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.OptionalLong;

/**
 * Extension point for storage-capacity compatibility rules.
 */
public interface StorageMetricAdapter {
    OptionalLong estimate(ItemStack stack, ResourceLocation id);
}
