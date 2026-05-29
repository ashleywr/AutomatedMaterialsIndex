package com.sanhiruzu.ami.index.sniffers;

import com.sanhiruzu.ami.forge.AMI;
import com.sanhiruzu.ami.index.metrics.FluidMetricParser;
import com.sanhiruzu.ami.index.metrics.FluidStats;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class FluidMetricSniffer {
    public Optional<FluidStats> sniff(ItemStack stack, ResourceLocation id, @Nullable Level level) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        if (stack.getItem() instanceof BucketItem) {
            return Optional.of(new FluidStats(1.0D, "bucket"));
        }
        if (!isLikelyFluidContainer(id)) {
            return Optional.empty();
        }
        try {
            List<String> lines = stack.getTooltipLines(null, TooltipFlag.Default.NORMAL)
                    .stream()
                    .map(Component::getString)
                    .toList();
            return FluidMetricParser.parseTooltip(lines);
        } catch (RuntimeException | LinkageError e) {
            AMI.LOGGER.debug("Unable to inspect fluid tooltip for {}", id, e);
            return Optional.empty();
        }
    }

    private static boolean isLikelyFluidContainer(ResourceLocation id) {
        String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
        return path.contains("bucket")
                || path.contains("tank")
                || path.contains("reservoir")
                || path.contains("drum")
                || path.contains("fluid")
                || path.contains("cell")
                || path.contains("canister");
    }
}
