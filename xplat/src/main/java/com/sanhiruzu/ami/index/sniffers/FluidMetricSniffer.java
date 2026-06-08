package com.sanhiruzu.ami.index.sniffers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.metrics.FluidMetricParser;
import com.sanhiruzu.ami.index.metrics.FluidStats;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

public final class FluidMetricSniffer {
    private static boolean shouldScanTooltip(ItemStack stack, ResourceLocation id) {
        String identity = (id + " " + stack.getHoverName().getString()).toLowerCase(Locale.ROOT);
        return containsAny(identity,
                "fluid", "liquid", "water", "lava", "milk", "oil", "fuel",
                "tank", "bucket", "bottle", "vial", "flask", "capsule",
                "cell", "can", "canister", "drum", "reservoir");
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public Optional<FluidStats> sniff(ItemStack stack, ResourceLocation id, @Nullable Level level) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        if (stack.getItem() instanceof BucketItem) {
            return Optional.of(new FluidStats(1.0D, "bucket"));
        }

        OptionalLong capabilityCapacity = Services.PLATFORM.getItemFluidCapacity(stack);
        if (capabilityCapacity.isPresent()) {
            return Optional.of(new FluidStats(capabilityCapacity.getAsLong() / 1000.0D, "capability"));
        }

        if (!shouldScanTooltip(stack, id)) {
            return Optional.empty();
        }

        try {
            List<String> lines = Services.PLATFORM.getTooltipLines(stack, level)
                    .stream()
                    .map(Component::getString)
                    .toList();
            return FluidMetricParser.parseTooltip(lines);
        } catch (RuntimeException | LinkageError e) {
            AmiCore.LOGGER.debug("Unable to inspect fluid tooltip for {}", id, e);
            return Optional.empty();
        }
    }
}
