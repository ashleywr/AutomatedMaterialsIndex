package com.sanhiruzu.ami.index.sniffers;

import com.sanhiruzu.ami.index.metrics.ArmorStats;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class ArmorMetricSniffer implements ICapabilitySniffer<ArmorStats> {
    @Override
    public Optional<ArmorStats> sniff(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem)) {
            return Optional.empty();
        }
        ArmorStats stats = new ArmorStats(armorItem.getDefense(), armorItem.getToughness());
        return stats.hasAny() ? Optional.of(stats) : Optional.empty();
    }
}
