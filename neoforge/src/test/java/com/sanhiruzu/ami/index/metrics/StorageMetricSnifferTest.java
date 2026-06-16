package com.sanhiruzu.ami.index.metrics;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageMetricSnifferTest {
    @Test
    void vanillaContainersUseStableVanillaCapacityBeforeCapabilities() {
        assertEquals(OptionalLong.of(27L * 64L),
                StorageMetricSniffer.estimate(new ItemStack(Items.CHEST), new Identifier("minecraft:chest"), null));
        assertEquals(OptionalLong.of(27L * 64L),
                StorageMetricSniffer.estimate(new ItemStack(Items.SHULKER_BOX), new Identifier("minecraft:shulker_box"), null));
    }
}
