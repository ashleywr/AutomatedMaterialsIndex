package com.sanhiruzu.ami.index.sniffers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.api.IStaticEnergyProvider;
import com.sanhiruzu.ami.index.metrics.PowerMetricParser;
import com.sanhiruzu.ami.index.metrics.PowerStats;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class PowerMetricSniffer {
    private final EnergyCapacitySniffer capacitySniffer = new EnergyCapacitySniffer();

    private static PowerStats sniffStaticBlockDefault(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return PowerStats.empty();
        }

        Block block = blockItem.getBlock();
        if (!(block instanceof IStaticEnergyProvider provider)) {
            return PowerStats.empty();
        }

        BlockState defaultState = block.defaultBlockState();
        Integer capacity = positiveInt(provider.getBaseCapacity(defaultState));
        Double generation = positiveDouble(provider.getBaseGenerationFePerTick(defaultState));
        Double consumption = positiveDouble(provider.getBaseConsumptionFePerTick(defaultState));
        return new PowerStats(capacity, generation, consumption, "static");
    }

    private static PowerStats sniffTooltip(ItemStack stack, ResourceLocation id, @Nullable Level level) {
        try {
            List<String> lines = Services.PLATFORM.getTooltipLines(stack, level)
                    .stream()
                    .map(Component::getString)
                    .toList();
            return PowerMetricParser.parseTooltip(lines, identity(stack, id)).orElse(PowerStats.empty());
        } catch (RuntimeException | LinkageError e) {
            AmiCore.LOGGER.debug("Unable to inspect power tooltip for {}", id, e);
            return PowerStats.empty();
        }
    }

    private static boolean shouldScanTooltip(ItemStack stack, ResourceLocation id, PowerStats stats) {
        return true;
    }

    private static String identity(ItemStack stack, ResourceLocation id) {
        return id + " " + stack.getHoverName().getString();
    }

    private static Integer positiveInt(int value) {
        return value > 0 ? value : null;
    }

    private static Double positiveDouble(double value) {
        return value > 0.0D ? value : null;
    }

    public Optional<PowerStats> sniff(ItemStack stack, ResourceLocation id, @Nullable Level level) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }

        PowerStats stats = PowerStats.empty();
        stats = stats.merge(capacitySniffer.sniff(stack)
                .map(capacity -> new PowerStats(capacity, null, null, "capability"))
                .orElse(PowerStats.empty()));
        stats = stats.merge(sniffStaticBlockDefault(stack));

        if (shouldScanTooltip(stack, id, stats)) {
            stats = stats.merge(sniffTooltip(stack, id, level));
        }

        return stats.hasAny() ? Optional.of(stats) : Optional.empty();
    }
}
