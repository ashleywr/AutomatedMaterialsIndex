package com.sanhiruzu.ami.index.sniffers;

import com.sanhiruzu.ami.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Extracts static, searchable facts from entity types without constructing entities.
 *
 * <p>The cache is keyed by the registry singleton {@link EntityType}. Entity registries are stable
 * after load, so a bounded concurrent map is simpler and safer than tooltip-time recomputation.</p>
 */
public final class EntityDataSniffer {
    public static final ResourceLocation AMI_TAMABLE_TAG = Services.PLATFORM.rl("ami", "tamable");
    public static final ResourceLocation AMI_MOUNTABLE_TAG = Services.PLATFORM.rl("ami", "mountable");
    public static final ResourceLocation AMI_TRUSTS_PLAYER_TAG = Services.PLATFORM.rl("ami", "trusts_player");

    private static final Set<ResourceLocation> VANILLA_MOUNTABLE = Set.of(
            mc("camel"),
            mc("donkey"),
            mc("horse"),
            mc("llama"),
            mc("mule"),
            mc("pig"),
            mc("skeleton_horse"),
            mc("strider"),
            mc("trader_llama"),
            mc("zombie_horse")
    );

    private static final Map<ResourceLocation, List<String>> VANILLA_TAMING_ITEMS = Map.of(
            mc("cat"), List.of("minecraft:cod", "minecraft:salmon"),
            mc("horse"), List.of(),
            mc("donkey"), List.of(),
            mc("mule"), List.of(),
            mc("llama"), List.of(),
            mc("parrot"), List.of("minecraft:wheat_seeds", "minecraft:melon_seeds", "minecraft:pumpkin_seeds", "minecraft:beetroot_seeds"),
            mc("trader_llama"), List.of(),
            mc("wolf"), List.of("minecraft:bone")
    );

    private static final Map<ResourceLocation, List<String>> VANILLA_TRUST_ITEMS = Map.of(
            mc("fox"), List.of("minecraft:sweet_berries", "minecraft:glow_berries"),
            mc("ocelot"), List.of("minecraft:cod", "minecraft:salmon")
    );

    private final ConcurrentHashMap<EntityType<?>, List<String>> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<EntityType<?>, Map<String, String>> metadataCache = new ConcurrentHashMap<>();

    private static List<String> extractUncached(EntityType<?> entityType) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (id == null) {
            return List.of();
        }

        addAttributes(entityType, tags);

        if (isMountable(entityType, id)) {
            tags.add("#mountable");
            tags.add("mountable");
        }

        List<String> tamingItems = VANILLA_TAMING_ITEMS.get(id);
        if (tamingItems != null || hasAmiEntityTag(entityType, AMI_TAMABLE_TAG)) {
            tags.add("#tamable");
            tags.add("tamable");
            tags.add("tameable");
            tags.add("pet");
            addTamingItems(tags, tamingItems);
        }

        List<String> trustItems = VANILLA_TRUST_ITEMS.get(id);
        if (trustItems != null || hasAmiEntityTag(entityType, AMI_TRUSTS_PLAYER_TAG)) {
            tags.add("#trusts_player");
            tags.add("trusts_player");
            addTamingItems(tags, trustItems);
        }

        return List.copyOf(tags);
    }

    private static Map<String, String> extractNumericMetadataUncached(EntityType<?> entityType) {
        Map<String, String> metadata = new LinkedHashMap<>();
        AttributeSupplier attributes = attributeSupplier(entityType);
        if (attributes == null) {
            return Map.of();
        }

        if (attributes.hasAttribute(Attributes.MAX_HEALTH)) {
            metadata.put("health", Long.toString(Math.round(attributes.getBaseValue(Attributes.MAX_HEALTH))));
        }
        if (attributes.hasAttribute(Attributes.ATTACK_DAMAGE)) {
            long attackDamage = Math.round(attributes.getBaseValue(Attributes.ATTACK_DAMAGE));
            if (attackDamage > 0) {
                metadata.put("attack_damage", Long.toString(attackDamage));
            }
        }
        return Map.copyOf(metadata);
    }

    private static void addAttributes(EntityType<?> entityType, Set<String> tags) {
        AttributeSupplier attributes = attributeSupplier(entityType);
        if (attributes == null) {
            return;
        }

        if (attributes.hasAttribute(Attributes.MAX_HEALTH)) {
            tags.add("health:" + Math.round(attributes.getBaseValue(Attributes.MAX_HEALTH)));
        }
        if (attributes.hasAttribute(Attributes.ATTACK_DAMAGE)) {
            long attackDamage = Math.round(attributes.getBaseValue(Attributes.ATTACK_DAMAGE));
            if (attackDamage > 0) {
                tags.add("attack:" + attackDamage);
                tags.add("attack_damage:" + attackDamage);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static AttributeSupplier attributeSupplier(EntityType<?> entityType) {
        if (!DefaultAttributes.hasSupplier(entityType)) {
            return null;
        }
        return DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) entityType);
    }

    private static boolean isMountable(EntityType<?> entityType, ResourceLocation id) {
        return VANILLA_MOUNTABLE.contains(id) || hasAmiEntityTag(entityType, AMI_MOUNTABLE_TAG);
    }

    private static boolean hasAmiEntityTag(EntityType<?> entityType, ResourceLocation tagId) {
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entityType).tags()
                .anyMatch(tag -> tag.location().equals(tagId));
    }

    private static void addTamingItems(Set<String> tags, List<String> itemIds) {
        if (itemIds == null) {
            return;
        }
        for (String itemId : itemIds) {
            tags.add("tames_with:" + itemId);
            int separator = itemId.indexOf(':');
            if (separator >= 0 && separator + 1 < itemId.length()) {
                tags.add("tames_with:" + itemId.substring(separator + 1));
            }
        }
    }

    private static List<String> tagItems(TagKey<Item> tag) {
        List<String> ids = new ArrayList<>();
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(holder.value());
            if (id != null) {
                ids.add(id.toString());
            }
        }
        return ids.stream().distinct().sorted().collect(Collectors.toUnmodifiableList());
    }

    private static ResourceLocation mc(String path) {
        return Services.PLATFORM.rl("minecraft", path);
    }

    public List<String> extractSearchTags(SpawnEggItem eggItem) {
        if (eggItem == null) {
            return List.of();
        }
        return extractSearchTags(eggItem.getType(null));
    }

    public List<String> extractSearchTags(EntityType<?> entityType) {
        if (entityType == null) {
            return List.of();
        }
        return cache.computeIfAbsent(entityType, EntityDataSniffer::extractUncached);
    }

    public Map<String, String> extractNumericMetadata(EntityType<?> entityType) {
        if (entityType == null) {
            return Map.of();
        }
        return metadataCache.computeIfAbsent(entityType, EntityDataSniffer::extractNumericMetadataUncached);
    }
}
