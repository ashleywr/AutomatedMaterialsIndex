package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.index.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Populates the GlobalIndex with biomes from the level registry.
 * Port of WorldAtlasIndexer biome section.
 */
public class BiomeProvider implements IAmiDataProvider {

    @Override
    public void populate(GlobalIndex index, @Nullable ClientLevel level) {
        if (level == null) return;

        List<SearchNode> nodes = new ArrayList<>();
        level.registryAccess().registry(Registries.BIOME).ifPresent(reg ->
            reg.holders().forEach(holder -> {
                var id = holder.key().location();
                int waterColor = holder.value().getSpecialEffects().getWaterColor();

                String dimension = "overworld";
                if (holder.is(BiomeTags.IS_NETHER)) dimension = "nether";
                else if (holder.is(BiomeTags.IS_END)) dimension = "end";

                float temperature = holder.value().getBaseTemperature();

                Map<String, String> meta = new HashMap<>();
                meta.put(SearchNodeKeys.MOD_ID, id.getNamespace());
                meta.put(SearchNodeKeys.DIMENSION, dimension);
                meta.put(SearchNodeKeys.TEMPERATURE, String.format("%.3f", temperature));

                nodes.add(new SearchNode(
                    id, NodeType.BIOME,
                    RegistryUtils.formatPath(id.getPath()),
                    0xFF000000 | waterColor, 0, meta));
            })
        );

        nodes.sort(RegistryUtils.ENTRY_ORDER);
        nodes.forEach(index::addNode);
    }
}
