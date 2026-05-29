package com.sanhiruzu.ami.index.sniffers;

import com.sanhiruzu.ami.index.metrics.FoodStats;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class FoodMetricSniffer implements ICapabilitySniffer<FoodStats> {
    @Override
    public Optional<FoodStats> sniff(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.getItem().isEdible()) {
            return Optional.empty();
        }

        FoodProperties food = stack.getItem().getFoodProperties(stack, null);
        if (food == null) {
            return Optional.empty();
        }

        return Optional.of(new FoodStats(
                food.getNutrition(),
                food.getNutrition() * food.getSaturationModifier() * 2.0F
        ));
    }
}
