package com.sanhiruzu.ami.index.metrics;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalLong;

/**
 * Storage-capacity compatibility rule backed by runtime facts.
 */
public interface StorageMetricAdapter {
    OptionalLong estimate(ItemStack stack, Identifier id, @Nullable Level level);
}
