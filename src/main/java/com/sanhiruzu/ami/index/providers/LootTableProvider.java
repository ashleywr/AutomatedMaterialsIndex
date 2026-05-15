package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.IAmiDataProvider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import org.jetbrains.annotations.Nullable;

/**
 * Scans loot tables and records presence for later edge-resolution.
 * Best-effort: if API access is unavailable, the provider logs and exits.
 */
public class LootTableProvider implements IAmiDataProvider {

    @Override
    public void populate(GlobalIndex index, @Nullable ClientLevel level) {
        if (level == null) return;

        AMI.LOGGER.info("LootTableProvider: scanning loot tables...");

        try {
            level.registryAccess().registry(Registries.LOOT_TABLE).ifPresent(reg ->
                reg.holders().forEach(holder -> {
                    var id = holder.key().location();
                    // For now, just log discovered loot tables. Edge wiring is implemented later.
                    AMI.LOGGER.debug("Found loot table {}", id);
                })
            );
        } catch (Exception e) {
            AMI.LOGGER.warn("LootTableProvider skipped due to: {}", e.toString());
        }
    }
}
