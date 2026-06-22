package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DoggyTalentsCompat {
    private static final String MOD_ID = "doggytalents";

    private DoggyTalentsCompat() {
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
            meta.put("doggyTalentsItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "doggytalents_" + kind);
        }
        applyKindMetadata(kind, meta);
        if (!facts.isEmpty()) {
            meta.put("doggyTalentsFacts", CompatMetaUtil.join(facts));
        }
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (CompatMetaUtil.containsAny(context.itemClass, "TreatItem", "ScentTreatItem", "DroolScentTreatItem")) facts.add("treat");
        if (CompatMetaUtil.containsAny(context.itemClass, "WhistleItem", "CanineTrackerItem", "LocatorOrbItem", "DyableOrbItem", "EmptyLocatorOrbItem")) {
            facts.add("tracker");
        }
        if (CompatMetaUtil.containsAny(context.itemClass, "ThrowableItem", "FrisbeeItem", "DroolBoneItem", "FrisbeeDroolItem", "EnergizerStickItem")) {
            facts.add("toy_tool");
        }
        if (CompatMetaUtil.containsAny(context.itemClass, "PianoItem", "DogPlushieItem", "SamoyedPlushieItem")) facts.add("decor");
        if (context.itemClass.contains("doggytalents.common.entity.accessory.")) facts.add("pet_accessory");
        if (CompatMetaUtil.containsAny(context.itemClass, "AccessoryItem", "DualAccessoryItem", "DyeableAccessoryItem", "DoggyContactsItem",
                "DemonHornsItem", "DeerAntlersItem", "BunnyEarsItem", "DogPresentCostumeItem")) {
            facts.add("pet_accessory");
        }
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        for (String tag : CompatMetaUtil.splitCsv(context.tags)) {
            if (tag.startsWith("doggytalents:treats")) facts.add("treat");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = CompatMetaUtil.basePath(context.path);
        if (CompatMetaUtil.containsAny(path, "treat", "tracker", "orb", "whistle", "frisbee", "plushie", "piano")) {
            if (path.contains("treat")) facts.add("treat");
            if (CompatMetaUtil.containsAny(path, "tracker", "orb", "whistle")) facts.add("tracker");
            if (CompatMetaUtil.containsAny(path, "frisbee", "stick")) facts.add("toy_tool");
            if (CompatMetaUtil.containsAny(path, "plushie", "piano")) facts.add("decor");
        }
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("pet_accessory")) return "pet_accessory";
        if (facts.contains("tracker")) return "tracker";
        if (facts.contains("toy_tool")) return "toy_tool";
        if (facts.contains("treat")) return "treat";
        if (facts.contains("decor")) return "decor";
        return "";
    }

    private static void applyKindMetadata(String kind, Map<String, String> meta) {
        switch (kind) {
            case "pet_accessory" -> CompatMetaUtil.addFacet(meta, ItemFacet.CURIO);
            case "tracker" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_NAVIGATION);
            case "toy_tool" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_TOOL);
            case "treat" -> route(meta, "nature", "snacks");
            case "decor" -> route(meta, "decoration", "furniture");
            default -> {
            }
        }
    }

    private static void route(Map<String, String> meta, String category, String subcategory) {
        meta.put(SearchNodeKeys.COMPAT_ROUTE_CATEGORY, category);
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
