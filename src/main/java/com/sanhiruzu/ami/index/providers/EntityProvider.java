package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.index.*;
import com.sanhiruzu.ami.index.sniffers.EntityDataSniffer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Populates the GlobalIndex with entity types from BuiltInRegistries.ENTITY_TYPE.
 * Port of WorldAtlasIndexer entity section.
 */
public class EntityProvider implements IAmiDataProvider {

    @Override
    public void populate(GlobalIndex index, @Nullable ClientLevel level) {
        List<SearchNode> nodes = new ArrayList<>();
        EntityDataSniffer entityDataSniffer = new EntityDataSniffer();

        BuiltInRegistries.ENTITY_TYPE.entrySet().forEach(e -> {
            var id = e.getKey().location();
            var entityType = e.getValue();
            var category = entityType.getCategory();
            List<String> searchTags = entityDataSniffer.extractSearchTags(entityType);
            Map<String, String> numericMetadata = entityDataSniffer.extractNumericMetadata(entityType);

            Map<String, String> meta = new HashMap<>();
            meta.put(SearchNodeKeys.MOD_ID, id.getNamespace());
            meta.put(SearchNodeKeys.ENTITY_CATEGORY, category.name());
            meta.put(SearchNodeKeys.FIRE_IMMUNE, String.valueOf(entityType.fireImmune()));
            if (numericMetadata.containsKey("health")) {
                meta.put(SearchNodeKeys.ENTITY_HEALTH, numericMetadata.get("health"));
            }
            if (numericMetadata.containsKey("attack_damage")) {
                meta.put(SearchNodeKeys.ENTITY_ATTACK_DAMAGE, numericMetadata.get("attack_damage"));
            }
            if (!searchTags.isEmpty()) {
                meta.put(SearchNodeKeys.ENTITY_TRAITS, String.join(" ", searchTags));
                meta.put(SearchNodeKeys.SEARCH_TOKENS, String.join(" ", searchTags));
                meta.put(SearchNodeKeys.TAGS, entityTags(entityType, searchTags));
            }

            nodes.add(new SearchNode(
                id, NodeType.ENTITY,
                RegistryUtils.formatPath(id.getPath()),
                RegistryUtils.categoryColor(category), 0, meta));
        });

        nodes.sort(RegistryUtils.ENTRY_ORDER);
        nodes.forEach(index::addNode);
    }

    private static String entityTags(net.minecraft.world.entity.EntityType<?> entityType, List<String> searchTags) {
        List<String> tags = new ArrayList<>();
        BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entityType).tags()
                .map(tag -> tag.location().toString().toLowerCase())
                .forEach(tags::add);

        for (String searchTag : searchTags) {
            if (searchTag.startsWith("#") && searchTag.length() > 1) {
                tags.add("ami:" + searchTag.substring(1));
            }
        }

        return tags.stream().distinct().collect(Collectors.joining(","));
    }
}
