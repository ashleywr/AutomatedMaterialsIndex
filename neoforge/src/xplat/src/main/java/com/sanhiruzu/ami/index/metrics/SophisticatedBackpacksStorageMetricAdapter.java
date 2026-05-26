package com.sanhiruzu.ami.index.metrics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.OptionalLong;

final class SophisticatedBackpacksStorageMetricAdapter implements StorageMetricAdapter {
    private static final long DEFAULT_STACK_SIZE = 64L;

    @Override
    public OptionalLong estimate(ItemStack stack, ResourceLocation id) {
        if (id == null || !id.getNamespace().equals("sophisticatedbackpacks")) {
            return OptionalLong.empty();
        }

        String path = id.getPath().toLowerCase(Locale.ROOT);
        if (path.contains("iron_backpack")) {
            return OptionalLong.of(54L * DEFAULT_STACK_SIZE);
        }
        if (path.contains("gold_backpack")) {
            return OptionalLong.of(81L * DEFAULT_STACK_SIZE);
        }
        if (path.contains("diamond_backpack")) {
            return OptionalLong.of(108L * DEFAULT_STACK_SIZE);
        }
        if (path.contains("netherite_backpack")) {
            return OptionalLong.of(120L * DEFAULT_STACK_SIZE);
        }
        if (path.endsWith("backpack")) {
            return OptionalLong.of(27L * DEFAULT_STACK_SIZE);
        }

        return OptionalLong.empty();
    }
}
