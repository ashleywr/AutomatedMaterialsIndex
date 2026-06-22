package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class TideCompat {
    private static final String MOD_ID = "tide";

    private TideCompat() {
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
            meta.put("tideItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "tide_" + kind);
        }
        applyKindMetadata(kind, meta);
        if (!facts.isEmpty()) {
            meta.put("tideFacts", CompatMetaUtil.join(facts));
        }
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (CompatMetaUtil.containsAny(context.itemClass, "FishingHookItem", "FishingLineItem")) facts.add("fishing_tackle");
        if (CompatMetaUtil.containsAny(context.itemClass, "PocketWatchItem", "EnchantedPocketWatchItem", "LunarCalendarItem",
                "ClimateGaugeItem", "DepthMeterItem", "WeatherRadioItem", "VoidseekerItem")) facts.add("navigation_gadget");
        if (CompatMetaUtil.containsAny(context.itemClass, "FishingJournalItem", "FishyNoteItem")) facts.add("reference");
        if (CompatMetaUtil.containsAny(context.itemClass, "FishSatchelItem")) facts.add("fish_satchel");
        if (CompatMetaUtil.containsAny(context.itemClass, "FishingBobberItem")) facts.add("bobber");
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        for (String tag : CompatMetaUtil.splitCsv(context.tags)) {
            if (tag.startsWith("tide:hooks") || tag.startsWith("tide:lines")) facts.add("fishing_tackle");
            if (tag.startsWith("tide:bobbers")) facts.add("bobber");
            if (tag.startsWith("tide:fishing_rods")) facts.add("rod");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = CompatMetaUtil.basePath(context.path);
        if (CompatMetaUtil.containsAny(path, "hook", "line")) facts.add("fishing_tackle");
        if (CompatMetaUtil.containsAny(path, "journal", "note")) facts.add("reference");
        if (CompatMetaUtil.containsAny(path, "watch", "calendar", "gauge", "meter", "radio", "voidseeker")) {
            facts.add("navigation_gadget");
        }
        if (path.contains("satchel")) facts.add("fish_satchel");
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("reference")) return "reference";
        if (facts.contains("navigation_gadget")) return "navigation_gadget";
        if (facts.contains("fishing_tackle")) return "fishing_tackle";
        if (facts.contains("fish_satchel")) return "fish_satchel";
        if (facts.contains("bobber")) return "bobber";
        if (facts.contains("rod")) return "rod";
        return "";
    }

    private static void applyKindMetadata(String kind, Map<String, String> meta) {
        switch (kind) {
            case "reference" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.BOOK);
                CompatMetaUtil.addFacet(meta, ItemFacet.GUIDE_BOOK);
            }
            case "navigation_gadget" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_NAVIGATION);
            case "fishing_tackle", "rod" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_TOOL);
            case "fish_satchel", "bobber" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_MISC);
            default -> {
            }
        }
    }

    private static final class Context {
        final String path;
        final String itemClass;
        final String tags;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath();
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(java.util.Locale.ROOT);
        }
    }
}
