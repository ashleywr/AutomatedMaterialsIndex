package com.sanhiruzu.ami.index;

import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for data providers that populate GlobalIndex.
 */
@FunctionalInterface
public interface IAmiDataProvider {
    /**
     * Populate the given GlobalIndex with nodes.
     * @param index  the index to write into
     * @param level  the current ClientLevel; may be null if registry access is unavailable
     */
    void populate(GlobalIndex index, @Nullable ClientLevel level);
}
