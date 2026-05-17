package com.sanhiruzu.ami.index;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Common interface for all AMI data indexers.
 */
public interface IAmiDataProvider {
    /**
     * Populate the provided index with nodes of this provider's type.
     * @param index The GlobalIndex to add nodes to.
     * @param level The current level context (client-side or server-side).
     */
    void populate(GlobalIndex index, @Nullable Level level);
}
