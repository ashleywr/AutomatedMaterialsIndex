package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class SophisticatedCompat {
    private SophisticatedCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }
        if (meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()) {
            CompatFamilyDetector.detect(id, meta);
        }
        if (!isSophisticatedFamilyItem(id, meta)) {
            return;
        }

        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addClassFacts(context, facts);
        addPathFacts(context, facts);
        addTagFacts(context, facts);

        String kind = classifyKind(context, facts);
        if (!kind.isBlank()) {
            meta.put(SearchNodeKeys.SOPHISTICATED_ITEM_KIND, kind);
            addSearchToken(meta, "sophisticated_" + kind);
        }
        String tier = classifyTier(context.path);
        if (!tier.isBlank()) {
            meta.put(SearchNodeKeys.SOPHISTICATED_TIER, tier);
            addSearchToken(meta, "sophisticated_" + tier);
        }
        if (!facts.isEmpty()) {
            meta.put(SearchNodeKeys.SOPHISTICATED_FACTS, join(facts));
            for (String fact : facts) {
                addSearchToken(meta, fact);
            }
        }
    }

    private static boolean isSophisticatedFamilyItem(ResourceLocation id, Map<String, String> meta) {
        return id.getNamespace().equals("sophisticatedbackpacks")
                || id.getNamespace().equals("sophisticatedstorage")
                || id.getNamespace().equals("sophisticatedcore")
                || CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.SOPHISTICATED);
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (containsAny(context.itemClass, "BackpackItem")) facts.add("backpack");
        if (containsAny(context.itemClass, "UpgradeItem")) facts.add("upgrade");
        if (containsAny(context.itemClass, "FilterUpgradeItem")) facts.add("filter");
        if (containsAny(context.itemClass, "Storage", "Barrel", "Chest", "LimitedBarrel", "Controller", "Connector")
                || containsAny(context.blockClass, "Storage", "Barrel", "Chest", "Controller", "Connector")) {
            facts.add("storage");
        }
        if (containsAny(context.itemClass, "BlockItemBase", "PaintbrushItem", "PackingTapeItem")) facts.add("tool_or_utility");
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = context.path;
        if (path.contains("backpack")) facts.add("backpack");
        if (path.contains("upgrade") || path.contains("downgrade")) facts.add("upgrade");
        if (path.contains("filter")) facts.add("filter");
        if (containsAny(path, "barrel", "chest", "limited_barrel", "shulker_box", "storage_connector",
                "storage_controller", "controller", "connector")) {
            facts.add("storage");
        }
        if (containsAny(path, "pickup", "magnet", "feeding", "compacting", "void", "restock", "deposit",
                "refill", "smelting", "smoking", "blasting", "crafting", "stonecutter", "stack",
                "jukebox", "tank", "battery", "pump", "hopper", "compression", "infinity", "alchemy")) {
            facts.add("upgrade_role");
        }
        if (containsAny(path, "packing_tape", "paintbrush", "xp_bucket", "decoration_table")) {
            facts.add("tool_or_utility");
        }
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        if (containsAny(context.tags, "sophisticatedbackpacks:upgrade", "sophisticatedstorage:upgrade")) facts.add("upgrade");
        if (containsAny(context.tags, "curios:back", "accessories:back")) facts.add("backpack");
    }

    private static String classifyKind(Context context, Set<String> facts) {
        if (facts.contains("backpack")) return "backpacks";
        if (facts.contains("filter")) return "filters";
        if (facts.contains("upgrade")) return "upgrades";
        if (facts.contains("storage")) return "storage";
        if (facts.contains("tool_or_utility")) return "tools";
        if (hasToken(context.facets, "storage")) return "storage";
        return "";
    }

    private static String classifyTier(String path) {
        if (path.contains("omega")) return "omega";
        for (String tier : new String[]{"netherite", "diamond", "gold", "iron", "copper", "advanced", "starter"}) {
            if (path.startsWith(tier + "_") || path.contains("_" + tier + "_") || path.endsWith("_" + tier)) {
                return tier;
            }
        }
        if (path.contains("tier_5")) return "tier_5";
        if (path.contains("tier_4")) return "tier_4";
        if (path.contains("tier_3")) return "tier_3";
        if (path.contains("tier_2")) return "tier_2";
        if (path.contains("tier_1")) return "tier_1";
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

    private static boolean hasToken(String csv, String token) {
        for (String value : splitCsv(csv)) {
            if (value.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private static Iterable<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String value : csv.split(",")) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }
        return values;
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

    private static final class Context {
        final String path;
        final String itemClass;
        final String blockClass;
        final String tags;
        final String facets;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.blockClass = meta.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
            this.facets = meta.getOrDefault(SearchNodeKeys.FACETS, "").toLowerCase(Locale.ROOT);
        }
    }
}
