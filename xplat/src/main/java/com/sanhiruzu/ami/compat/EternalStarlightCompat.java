package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class EternalStarlightCompat {
    private static final String MOD_ID = "eternal_starlight";

    private EternalStarlightCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null || !MOD_ID.equals(id.getNamespace())) {
            return;
        }
        meta.putIfAbsent(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, MOD_ID);
        meta.putIfAbsent(SearchNodeKeys.COMPAT_FAMILIES, MOD_ID);
        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addClassFacts(context, facts);
        addTagFacts(context, facts);
        addPathFacts(context, facts);

        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put("eternalStarlightItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "eternal_starlight_" + kind);
        }
        applyKindMetadata(context, kind, meta);
        if (!facts.isEmpty()) {
            meta.put("eternalStarlightFacts", CompatMetaUtil.join(facts));
        }
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (CompatMetaUtil.containsAny(context.itemClass, "ESPaintingItem")) facts.add("painting");
        if (CompatMetaUtil.containsAny(context.itemClass, "GalacticQuiverItem", "SoulitSpectatorItem", "LivingArmItem")) facts.add("accessory");
        if (CompatMetaUtil.containsAny(context.itemClass, "EthericEyeItem", "SeekingEyeItem", "OrbOfProphecyItem",
                "StarfireItem", "CandlashItem", "ColdsnapItem", "ChainOfSoulsItem", "TentacleSpikeItem")) {
            facts.add("artifact");
        }
        if (CompatMetaUtil.containsAny(context.itemClass, "LootBagItem")) facts.add("utility_bag");
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        for (String tag : CompatMetaUtil.splitCsv(context.tags)) {
            if (tag.startsWith("eternal_starlight:accessories")) facts.add("accessory");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = CompatMetaUtil.basePath(context.path);
        if (path.contains("painting")) facts.add("painting");
        if (CompatMetaUtil.containsAny(path, "pendant", "amulet", "quiver", "spectator", "living_arm")) facts.add("accessory");
        if (CompatMetaUtil.containsAny(path, "eye", "orb", "starfire", "candlash", "coldsnap", "chain_of_souls", "doom")) {
            facts.add("artifact");
        }
        if (CompatMetaUtil.containsAny(path, "dew", "petal", "rag", "sac", "gel")) facts.add("reagent");
        if (path.contains("brick")) facts.add("material");
        if (path.contains("bag")) facts.add("utility_bag");
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("painting")) return "paintings";
        if (facts.contains("accessory")) return "accessories";
        if (facts.contains("artifact")) return "artifacts";
        if (facts.contains("reagent")) return "reagents";
        if (facts.contains("material")) return "materials";
        if (facts.contains("utility_bag")) return "utility_bag";
        return "";
    }

    private static void applyKindMetadata(Context context, String kind, Map<String, String> meta) {
        switch (kind) {
            case "paintings" -> {
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_FAMILY, MOD_ID + ":starlit_painting");
                meta.putIfAbsent(SearchNodeKeys.COLLAPSE_LABEL, "Starlit Paintings");
                meta.putIfAbsent(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
                route(meta, "paintings");
            }
            case "accessories" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.CURIO);
                CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
                route(meta, "accessories");
            }
            case "artifacts" -> {
                if (context.path.contains("starfire") || context.path.contains("tentacle_spike")) {
                    CompatMetaUtil.addFacet(meta, ItemFacet.PROJECTILE);
                }
                CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
                route(meta, "artifacts");
            }
            case "reagents" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_REAGENT);
                route(meta, "reagents");
            }
            case "materials" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_MINERAL);
                route(meta, "materials");
            }
            case "utility_bag" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_MISC);
                route(meta, "utility");
            }
            default -> {
            }
        }
    }

    private static void route(Map<String, String> meta, String subcategory) {
        meta.put(SearchNodeKeys.COMPAT_ROUTE_CATEGORY, MOD_ID);
        meta.put(SearchNodeKeys.COMPAT_ROUTE_SUBCATEGORY, subcategory);
    }

    private static final class Context {
        final String path;
        final String itemClass;
        final String tags;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT);
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
        }
    }
}
