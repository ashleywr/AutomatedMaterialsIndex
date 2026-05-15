package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.IAmiDataProvider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Scans biome spawn settings and logs entity->biome spawn relations.
 * This provider is best-effort and will not crash if underlying methods differ.
 */
public class SpawnProvider implements IAmiDataProvider {

    @Override
    public void populate(GlobalIndex index, @Nullable ClientLevel level) {
        if (level == null) return;

        AMI.LOGGER.info("SpawnProvider: scanning biome spawns...");

        try {
            level.registryAccess().registry(Registries.BIOME).ifPresent(reg ->
                reg.holders().forEach(holder -> {
                    var biomeId = holder.key().location();
                    try {
                        // Attempt to access MobSpawnSettings via known getter
                        var mobSettings = holder.value().getMobSettings();
                        // Many mappings expose spawners() returning Map<MobCategory, List<SpawnerData>>
                        Map<?, ?> spawners = (Map<?, ?>) mobSettings.getClass().getMethod("spawners").invoke(mobSettings);
                        if (spawners != null) {
                            for (var entry : spawners.entrySet()) {
                                List<?> list = (List<?>) entry.getValue();
                                for (Object sp : list) {
                                    // Try to extract the entity type from SpawnerData via reflection
                                    try {
                                        EntityType<?> et = (EntityType<?>) sp.getClass().getMethod("type").invoke(sp);
                                                                                AMI.LOGGER.debug("Entity type {} spawns in biome {}", et, biomeId);
                                    } catch (Exception inner) {
                                        // ignore
                                    }
                                }
                            }
                        }
                    } catch (NoSuchMethodException nsme) {
                        // Mapping differs; skip for now
                    } catch (Throwable t) {
                        // Catch reflection exceptions to avoid crash
                    }
                })
            );
        } catch (Exception e) {
            AMI.LOGGER.warn("SpawnProvider skipped due to: {}", e.toString());
        }
    }
}
