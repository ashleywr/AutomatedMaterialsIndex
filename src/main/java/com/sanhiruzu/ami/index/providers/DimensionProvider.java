package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.index.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Populates the GlobalIndex with dimensions from the level registry.
 * Port of WorldAtlasIndexer dimension section.
 */
public class DimensionProvider implements IAmiDataProvider {

    @Override
    public void populate(GlobalIndex index, @Nullable ClientLevel level) {
        if (level == null) return;

        List<SearchNode> nodes = new ArrayList<>();
        level.registryAccess().registry(Registries.DIMENSION).ifPresent(reg ->
            reg.holders().forEach(holder -> {
                var id = holder.key().location();
                int color = RegistryUtils.dimensionColor(id);

                Map<String, String> meta = new HashMap<>();
                meta.put(SearchNodeKeys.MOD_ID, id.getNamespace());

                nodes.add(new SearchNode(
                    id, NodeType.DIMENSION,
                    RegistryUtils.formatPath(id.getPath()),
                    color, 0, meta));
            })
        );

        nodes.sort(RegistryUtils.ENTRY_ORDER);
        index.replaceNodes(NodeType.DIMENSION, nodes);
    }
}
