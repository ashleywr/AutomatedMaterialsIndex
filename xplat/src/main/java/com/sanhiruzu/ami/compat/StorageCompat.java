package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class StorageCompat {
    private static final Set<String> GENERATED_SEARCH_TOKENS = Set.of(
            "storage",
            "portable_storage",
            "drawer_storage",
            "limited_storage",
            "compacting_storage",
            "storage_controller",
            "storage_connector",
            "upgrade",
            "filter",
            "access_tool",
            "redstone_control",
            "voiding",
            "item_transfer",
            "fluid_handling",
            "energy_storage",
            "stack_size_modifier",
            "capacity_indexed"
    );

    private StorageCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        clearGenericStorageMetadata(meta);
        if (id == null || meta == null || !isStorageFamilyItem(id, meta)) {
            return;
        }

        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addPathFacts(context, facts);
        addClassFacts(context, facts);
        addFacetFacts(context, facts);

        String kind = classifyKind(context, facts);
        if (!kind.isBlank()) {
            meta.put(SearchNodeKeys.STORAGE_ITEM_KIND, kind);
            addSearchToken(meta, "storage_" + kind);
        }
        String tier = classifyTier(context.path);
        if (!tier.isBlank()) {
            meta.put(SearchNodeKeys.STORAGE_TIER, tier);
            addSearchToken(meta, "storage_" + tier);
        }
        if (!facts.isEmpty()) {
            meta.put(SearchNodeKeys.STORAGE_FACTS, join(facts));
            for (String fact : facts) {
                addSearchToken(meta, fact);
            }
        }
    }

    private static boolean isStorageFamilyItem(ResourceLocation id, Map<String, String> meta) {
        Context context = new Context(id, meta);
        return isKnownStorageFamily(context.namespace)
                || hasStorageIdentity(context);
    }

    private static boolean isKnownStorageFamily(String namespace) {
        return namespace.equals("storagedrawers")
                || namespace.equals("ironchest")
                || namespace.equals("sophisticatedstorage")
                || namespace.equals("sophisticatedbackpacks");
    }

    private static boolean hasStorageIdentity(Context context) {
        return hasStorageRuntimeEvidence(context)
                && !hasUtilityInventoryShape(context)
                && (hasStoragePathIdentity(context)
                || hasStorageClassIdentity(context)
                || hasStorageTagIdentity(context)
                || hasStorageCreativeTabIdentity(context));
    }

    private static boolean hasStorageRuntimeEvidence(Context context) {
        return hasValue(context.meta, SearchNodeKeys.ESM_CAPACITY)
                || hasToken(context.facets, "storage")
                || hasToken(context.componentFacts, "container")
                || hasToken(context.componentFacts, "bundle_contents");
    }

    private static boolean hasUtilityInventoryShape(Context context) {
        return hasToken(context.facets, "machine")
                || hasToken(context.facets, "workstation")
                || hasToken(context.facets, "has_energy")
                || hasToken(context.facets, "fluid_container")
                || containsAny(context.itemClass, "Furnace", "Brewing", "Crafter", "Jukebox", "Machine", "Factory")
                || containsAny(context.blockClass, "Furnace", "Brewing", "Crafter", "Jukebox", "Machine", "Factory");
    }

    private static boolean hasStoragePathIdentity(Context context) {
        return hasPathToken(context.path,
                "backpack", "satchel", "pouch",
                "drawer", "drawers",
                "chest", "barrel", "cabinet", "crate", "basket", "silo", "locker")
                || context.path.contains("shulker_box")
                || context.path.contains("shipping_container")
                || context.path.contains("storage_container");
    }

    private static boolean hasStorageClassIdentity(Context context) {
        return containsAny(context.itemClass,
                "BackpackItem", "StorageBlockItem", "ItemDrawers", "IronChestBlockItem",
                "ChestBlockItem", "BarrelBlockItem", "Cabinet", "Basket")
                || containsAny(context.blockClass,
                "Drawer", "ChestBlock", "BarrelBlock", "Cabinet", "Basket", "StorageBlock");
    }

    private static boolean hasStorageTagIdentity(Context context) {
        return containsTagFamily(context.tags, "c:chests")
                || containsTagFamily(context.tags, "c:barrels")
                || containsTagFamily(context.tags, "minecraft:shulker_boxes")
                || containsTagFamily(context.blockTags, "c:chests")
                || containsTagFamily(context.blockTags, "c:barrels")
                || containsTagFamily(context.blockTags, "minecraft:shulker_boxes");
    }

    private static boolean hasStorageCreativeTabIdentity(Context context) {
        return containsAny(context.creativeTabId, "storage")
                || containsAny(context.creativeTabLabel, "storage");
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = context.path;
        if (hasPathToken(path, "backpack", "satchel", "pouch")) facts.add("portable_storage");
        if (hasPathToken(path, "drawer", "drawers")) facts.add("drawer_storage");
        if (path.contains("limited_barrel")) facts.add("limited_storage");
        if (hasPathToken(path, "compacting")) facts.add("compacting_storage");
        if (hasPathToken(path, "chest", "barrel", "cabinet", "crate", "basket", "silo", "locker")
                || path.contains("shulker_box")
                || path.contains("shipping_container")
                || path.contains("storage_container")) facts.add("storage");
        if (hasPathToken(path, "controller")) facts.add("storage_controller");
        if (hasPathToken(path, "connector")) facts.add("storage_connector");
        if (hasPathToken(path, "upgrade", "downgrade")) facts.add("upgrade");
        if (hasPathToken(path, "filter")) facts.add("filter");
        if (hasPathToken(path, "key", "remote")) facts.add("access_tool");
        if (hasPathToken(path, "redstone")) facts.add("redstone_control");
        if (hasPathToken(path, "void")) facts.add("voiding");
        if (hasPathToken(path, "hopper", "pickup", "magnet")) facts.add("item_transfer");
        if (hasPathToken(path, "tank", "pump")) facts.add("fluid_handling");
        if (hasPathToken(path, "battery")) facts.add("energy_storage");
        if (path.contains("stack_upgrade") || path.contains("stack_downgrade")) facts.add("stack_size_modifier");
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (containsAny(context.itemClass, "BackpackItem")) facts.add("portable_storage");
        if (containsAny(context.itemClass, "ItemDrawers", "IronChestBlockItem", "BarrelBlockItem", "ChestBlockItem", "ShulkerBoxItem")
                || containsAny(context.blockClass, "Drawer", "ChestBlock", "BarrelBlock", "LimitedBarrelBlock")) {
            facts.add("storage");
        }
        if (containsAny(context.itemClass, "UpgradeItem", "ChestUpgradeItem")) facts.add("upgrade");
        if (containsAny(context.itemClass, "FilterUpgradeItem")) facts.add("filter");
        if (containsAny(context.itemClass, "Key", "Remote")) facts.add("access_tool");
        if (containsAny(context.blockClass, "Controller")) facts.add("storage_controller");
        if (containsAny(context.blockClass, "Connector")) facts.add("storage_connector");
    }

    private static void addFacetFacts(Context context, Set<String> facts) {
        if (hasToken(context.facets, "storage")) facts.add("storage");
        if (hasValue(context.meta, SearchNodeKeys.ESM_CAPACITY)) facts.add("capacity_indexed");
    }

    private static String classifyKind(Context context, Set<String> facts) {
        if (facts.contains("portable_storage")) return "portable_storage";
        if (facts.contains("storage_controller")) return "controller";
        if (facts.contains("storage_connector")) return "connector";
        if (facts.contains("filter")) return "filter";
        if (facts.contains("upgrade")) return "upgrade";
        if (facts.contains("access_tool")) return "access_tool";
        if (facts.contains("drawer_storage")) return "drawer";
        if (facts.contains("limited_storage")) return "limited_storage";
        if (hasPathToken(context.path, "chest")) return "chest";
        if (hasPathToken(context.path, "barrel")) return "barrel";
        if (facts.contains("storage")) return "storage";
        return "";
    }

    private static String classifyTier(String path) {
        if (path.contains("omega")) return "omega";
        for (String tier : new String[]{"netherite", "diamond", "obsidian", "crystal", "gold", "iron", "copper", "basic", "starter"}) {
            if (path.startsWith(tier + "_") || path.contains("_" + tier + "_") || path.endsWith("_" + tier)) {
                return tier;
            }
        }
        for (String tier : new String[]{"tier_5", "tier_4", "tier_3", "tier_2", "tier_1"}) {
            if (path.contains(tier)) return tier;
        }
        return "";
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (normalized.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasValue(Map<String, String> meta, String key) {
        return !meta.getOrDefault(key, "").isBlank();
    }

    private static boolean hasToken(String csv, String token) {
        if (csv == null || csv.isBlank()) {
            return false;
        }
        for (String value : csv.split(",")) {
            if (value.trim().equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPathToken(String path, String... tokens) {
        if (path == null || path.isBlank()) {
            return false;
        }
        Set<String> values = new HashSet<>(Arrays.asList(path.split("[_\\-/]+")));
        for (String token : tokens) {
            if (values.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsTagFamily(String csv, String family) {
        if (csv == null || csv.isBlank()) {
            return false;
        }
        for (String value : csv.split(",")) {
            String tag = value.trim().toLowerCase(Locale.ROOT);
            if (tag.equals(family) || tag.startsWith(family + "/")) {
                return true;
            }
        }
        return false;
    }

    private static String join(Set<String> values) {
        StringJoiner joiner = new StringJoiner(",");
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                joiner.add(value);
            }
        }
        return joiner.toString();
    }

    private static void addSearchToken(Map<String, String> meta, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        String existing = meta.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, "");
        for (String value : existing.split("\\s+")) {
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        if (values.add(token)) {
            meta.put(SearchNodeKeys.SEARCH_TOKENS, String.join(" ", values));
        }
    }

    private static void clearGenericStorageMetadata(Map<String, String> meta) {
        if (meta == null) {
            return;
        }
        boolean hadGenericStorageMetadata = meta.containsKey(SearchNodeKeys.STORAGE_ITEM_KIND)
                || meta.containsKey(SearchNodeKeys.STORAGE_FACTS)
                || meta.containsKey(SearchNodeKeys.STORAGE_TIER);
        meta.remove(SearchNodeKeys.STORAGE_ITEM_KIND);
        meta.remove(SearchNodeKeys.STORAGE_FACTS);
        meta.remove(SearchNodeKeys.STORAGE_TIER);

        String existing = meta.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, "");
        if (existing.isBlank()) {
            return;
        }

        boolean hasStoragePrefixedToken = false;
        for (String value : existing.split("\\s+")) {
            if (value.startsWith("storage_")) {
                hasStoragePrefixedToken = true;
                break;
            }
        }
        if (!hadGenericStorageMetadata && !hasStoragePrefixedToken) {
            return;
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String value : existing.split("\\s+")) {
            if (value.isBlank()
                    || value.startsWith("storage_")
                    || GENERATED_SEARCH_TOKENS.contains(value)) {
                continue;
            }
            values.add(value);
        }
        if (values.isEmpty()) {
            meta.remove(SearchNodeKeys.SEARCH_TOKENS);
        } else {
            meta.put(SearchNodeKeys.SEARCH_TOKENS, String.join(" ", values));
        }
    }

    private static final class Context {
        final ResourceLocation id;
        final Map<String, String> meta;
        final String path;
        final String itemClass;
        final String blockClass;
        final String facets;
        final String tags;
        final String blockTags;
        final String creativeTabId;
        final String creativeTabLabel;
        final String componentFacts;
        final String namespace;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.id = id;
            this.meta = meta;
            this.namespace = id.getNamespace();
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.blockClass = meta.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
            this.facets = meta.getOrDefault(SearchNodeKeys.FACETS, "").toLowerCase(Locale.ROOT);
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
            this.blockTags = meta.getOrDefault(SearchNodeKeys.BLOCK_TAGS, "").toLowerCase(Locale.ROOT);
            this.creativeTabId = meta.getOrDefault(SearchNodeKeys.CREATIVE_TAB_ID, "").toLowerCase(Locale.ROOT);
            this.creativeTabLabel = meta.getOrDefault(SearchNodeKeys.CREATIVE_TAB_LABEL, "").toLowerCase(Locale.ROOT);
            this.componentFacts = meta.getOrDefault(SearchNodeKeys.COMPONENT_FACTS, "").toLowerCase(Locale.ROOT);
        }
    }
}
