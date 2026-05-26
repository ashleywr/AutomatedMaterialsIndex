package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.IAmiDataProvider;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class LootTableProvider implements IAmiDataProvider {
    @Override
    public void populate(GlobalIndex index, @Nullable Level level) {
        // Disabled in 1.20.1 since LOOT_TABLE is not a dynamic registry available on the client
    }
}
