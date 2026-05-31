package com.sanhiruzu.ami.index.metrics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalLong;

/**
 * Storage-capacity compatibility rule backed by runtime facts.
 */
public interface StorageMetricAdapter {
    OptionalLong estimate(ItemStack stack, ResourceLocation id, @Nullable Level level);
}
