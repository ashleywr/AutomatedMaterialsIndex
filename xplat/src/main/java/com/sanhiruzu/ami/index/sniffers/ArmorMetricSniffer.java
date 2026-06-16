package com.sanhiruzu.ami.index.sniffers;

import com.sanhiruzu.ami.index.metrics.ArmorStats;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.Optional;

public final class ArmorMetricSniffer implements ICapabilitySniffer<ArmorStats> {
    @Override
    public Optional<ArmorStats> sniff(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        if (stack.get(DataComponents.EQUIPPABLE) == null) return Optional.empty();
        ItemAttributeModifiers mods = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (mods == null) return Optional.empty();
        double defense = 0, toughness = 0;
        for (var entry : mods.modifiers()) {
            if (entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                if (entry.attribute() == Attributes.ARMOR) defense += entry.modifier().amount();
                else if (entry.attribute() == Attributes.ARMOR_TOUGHNESS) toughness += entry.modifier().amount();
            }
        }
        ArmorStats stats = new ArmorStats((int) defense, (float) toughness);
        return stats.hasAny() ? Optional.of(stats) : Optional.empty();
    }
}
