package com.sanhiruzu.ami.index.sniffers;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Generic, side-effect-minimal contract for extracting indexed facts from an
 * {@link ItemStack}.
 *
 * <p>Sniffers run during client index construction, where registry objects may
 * exist before a world is fully usable and where foreign mod code can be
 * sensitive to early initialization. Implementations should prefer data
 * components, item capabilities, and static item/block properties over creating
 * world objects, block entities, menus, or renderer state.</p>
 *
 * @param <T> extracted value type
 */
public interface ICapabilitySniffer<T> {
    /**
     * Attempts to extract a value from the supplied stack.
     *
     * @param stack stack to inspect
     * @return extracted value, or {@link Optional#empty()} when unavailable
     */
    Optional<T> sniff(ItemStack stack);
}
