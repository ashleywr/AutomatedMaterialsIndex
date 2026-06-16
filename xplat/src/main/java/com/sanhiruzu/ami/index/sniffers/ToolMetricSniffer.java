package com.sanhiruzu.ami.index.sniffers;

import com.sanhiruzu.ami.index.metrics.ToolStats;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;

import java.util.Optional;

public final class ToolMetricSniffer implements ICapabilitySniffer<ToolStats> {
    @Override
    public Optional<ToolStats> sniff(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        Tool tool = stack.get(DataComponents.TOOL);
        if (tool == null) return Optional.empty();
        float speed = tool.defaultMiningSpeed();
        int uses = stack.get(DataComponents.MAX_DAMAGE) != null ? stack.get(DataComponents.MAX_DAMAGE) : 0;
        float attackDamage = 0;
        ItemAttributeModifiers mods = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (mods != null) {
            for (var entry : mods.modifiers()) {
                if (entry.attribute() == Attributes.ATTACK_DAMAGE
                        && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                    attackDamage += (float) entry.modifier().amount();
                }
            }
        }
        ToolStats stats = new ToolStats(speed, uses, attackDamage);
        return stats.hasAny() ? Optional.of(stats) : Optional.empty();
    }
}
