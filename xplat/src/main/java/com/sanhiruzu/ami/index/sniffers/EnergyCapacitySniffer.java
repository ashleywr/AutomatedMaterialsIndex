package com.sanhiruzu.ami.index.sniffers;

import com.sanhiruzu.ami.api.IStaticEnergyProvider;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Safe startup-time energy capacity sniffer.
 *
 * <p>The ordering is deliberate. AMI first asks NeoForge's item capability
 * system, which is designed for direct {@link ItemStack} queries and does not
 * require a level. It then falls back to AMI's explicit static block contract.
 * It does not construct a {@code BlockEntity} or dummy {@code Level}; many
 * machine mods assume a real world context during block entity construction and
 * can throw while the client index is being built.</p>
 */
public final class EnergyCapacitySniffer implements ICapabilitySniffer<Integer> {
    private static Optional<Integer> sniffItemCapability(ItemStack stack) {
        return Services.PLATFORM.getItemEnergyCapacity(stack);
    }

    private static Optional<Integer> sniffStaticBlockDefault(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return Optional.empty();
        }

        Block block = blockItem.getBlock();
        if (!(block instanceof IStaticEnergyProvider provider)) {
            return Optional.empty();
        }

        BlockState defaultState = block.defaultBlockState();
        int capacity = provider.getBaseCapacity(defaultState);
        return capacity > 0 ? Optional.of(capacity) : Optional.empty();
    }

    @Override
    public Optional<Integer> sniff(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }

        Optional<Integer> itemCapabilityCapacity = sniffItemCapability(stack);
        if (itemCapabilityCapacity.isPresent()) {
            return itemCapabilityCapacity;
        }

        Optional<Integer> staticBlockCapacity = sniffStaticBlockDefault(stack);
        if (staticBlockCapacity.isPresent()) {
            return staticBlockCapacity;
        }

        return Optional.empty();
    }
}

