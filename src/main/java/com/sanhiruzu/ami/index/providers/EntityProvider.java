package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.index.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Populates the GlobalIndex with entity types from BuiltInRegistries.ENTITY_TYPE.
 * Port of WorldAtlasIndexer entity section.
 */
public class EntityProvider implements IAmiDataProvider {

    @Override
    public void populate(GlobalIndex index, @Nullable ClientLevel level) {
        List<SearchNode> nodes = new ArrayList<>();

        BuiltInRegistries.ENTITY_TYPE.entrySet().forEach(e -> {
            var id = e.getKey().location();
            var entityType = e.getValue();
            var category = entityType.getCategory();

            Map<String, String> meta = new HashMap<>();
            meta.put(SearchNodeKeys.MOD_ID, id.getNamespace());
            meta.put(SearchNodeKeys.ENTITY_CATEGORY, category.name());
            meta.put(SearchNodeKeys.FIRE_IMMUNE, String.valueOf(entityType.fireImmune()));

            nodes.add(new SearchNode(
                id, NodeType.ENTITY,
                RegistryUtils.formatPath(id.getPath()),
                RegistryUtils.categoryColor(category), 0, meta));
        });

        nodes.sort(RegistryUtils.ENTRY_ORDER);
        nodes.forEach(index::addNode);
    }
}
