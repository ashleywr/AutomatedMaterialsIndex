package com.sanhiruzu.ami.index.sniffers;

import com.sanhiruzu.ami.index.metrics.FoodStats;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class FoodMetricSniffer implements ICapabilitySniffer<FoodStats> {
    @Override
    public Optional<FoodStats> sniff(ItemStack stack) {
        return Services.PLATFORM.getFoodStats(stack);
    }
}
