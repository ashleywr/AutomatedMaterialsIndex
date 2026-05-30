package com.sanhiruzu.ami.index.sniffers;

import com.sanhiruzu.ami.index.metrics.ToolStats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;

import java.util.Optional;

public final class ToolMetricSniffer implements ICapabilitySniffer<ToolStats> {
    @Override
    public Optional<ToolStats> sniff(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof TieredItem tieredItem)) {
            return Optional.empty();
        }
        Tier tier = tieredItem.getTier();
        ToolStats stats = new ToolStats(tier.getSpeed(), tier.getUses(), tier.getAttackDamageBonus());
        return stats.hasAny() ? Optional.of(stats) : Optional.empty();
    }
}

