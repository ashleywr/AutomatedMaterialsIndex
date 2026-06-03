package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.index.*;
import com.sanhiruzu.ami.index.sniffers.EntityDataSniffer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Populates the GlobalIndex with entity types from BuiltInRegistries.ENTITY_TYPE.
 * Port of WorldAtlasIndexer entity section.
 */
public class EntityProvider implements IAmiDataProvider {

    private static final Set<String> NEUTRAL_MOBS = Set.of(
            "wolf", "bee", "polar_bear", "dolphin", "panda",
            "llama", "trader_llama", "goat", "iron_golem",
            "piglin", "zombified_piglin", "enderman",
            "spider", "cave_spider"
    );

    private static String classifyMobSubcategory(String path, MobCategory category) {
        if (NEUTRAL_MOBS.contains(path)) return "neutral";
        return category == MobCategory.MONSTER ? "hostile" : "passive";
    }

    private static boolean isInternalMarkerEntity(net.minecraft.resources.ResourceLocation id) {
        String path = id.getPath();
        return path.equals("marker") || path.endsWith("_marker");
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

    @Override
    public void populate(GlobalIndex index, @Nullable Level level) {
        List<SearchNode> nodes = new ArrayList<>();
        EntityDataSniffer entityDataSniffer = new EntityDataSniffer();

        BuiltInRegistries.ENTITY_TYPE.entrySet().forEach(e -> {
            var id = e.getKey().location();
            // Skip entities that have a direct item equivalent — they're already indexed as items.
            if (BuiltInRegistries.ITEM.containsKey(id)) return;
            if (isInternalMarkerEntity(id)) return;
            var entityType = e.getValue();
            var category = entityType.getCategory();
            List<String> searchTags = entityDataSniffer.extractSearchTags(entityType);
            String tags = entityTags(entityType, searchTags);
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
            }
            if (!tags.isEmpty()) {
                meta.put(SearchNodeKeys.TAGS, tags);
            }

            // ── Classification ──────────────────────────────────────────────────
            // Default everything to bestiary category
            meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, "bestiary");

            if (category == MobCategory.MISC) {
                if (id.getPath().equals("experience_orb")) {
                    meta.put(SearchNodeKeys.ACCESS_LEVEL, "dev");
                    meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, "magic");
                    meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "reagents");
                } else if (numericMetadata.containsKey("health")) {
                    // It has health, so it's a living entity even if categorized as MISC by the mod
                    meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "passive");
                } else {
                    meta.put(SearchNodeKeys.ACCESS_LEVEL, "dev");
                    meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, "misc");
                    meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "unknown");
                }
            } else {
                meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, classifyMobSubcategory(id.getPath(), category));
            }

            nodes.add(new SearchNode(
                    id, NodeType.ENTITY,
                    RegistryUtils.formatPath(id.getPath()),
                    RegistryUtils.categoryColor(category), 0, meta));
        });

        nodes.sort(RegistryUtils.ENTRY_ORDER);
        nodes.forEach(index::addNode);
    }
}
