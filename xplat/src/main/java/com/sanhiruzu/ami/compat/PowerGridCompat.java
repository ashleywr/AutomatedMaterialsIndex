package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PowerGridCompat {
    private static final String MOD_ID = "powergrid";

    private PowerGridCompat() {
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
            meta.put("powerGridItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "powergrid_" + kind);
        }
        applyKindMetadata(kind, meta);
        if (!facts.isEmpty()) {
            meta.put("powerGridFacts", CompatMetaUtil.join(facts));
        }
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (CompatMetaUtil.containsAny(context.itemClass, "CordItem", "WindingItem", "LvLightBulb", "LightBulb")) facts.add("component");
        if (CompatMetaUtil.containsAny(context.itemClass, "ElectroZapperItem", "MultimeterItem")) facts.add("tool");
        if (CompatMetaUtil.containsAny(context.itemClass, "PunchCardItem")) facts.add("programming");
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        for (String tag : CompatMetaUtil.splitCsv(context.tags)) {
            if (tag.startsWith("powergrid:circuit_component") || tag.startsWith("c:coils")) facts.add("component");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = CompatMetaUtil.basePath(context.path);
        if (CompatMetaUtil.containsAny(path, "cord", "coil", "magnet", "gizmo", "resistor", "diode", "vfet",
                "bjt", "capacitor", "potentiometer", "bulb", "varistor")) {
            facts.add("component");
        }
        if (path.contains("punch_card")) facts.add("programming");
        if (CompatMetaUtil.containsAny(path, "electrozapper", "multimeter")) facts.add("tool");
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("tool")) return "tools";
        if (facts.contains("programming")) return "programming";
        if (facts.contains("component")) return "components";
        return "";
    }

    private static void applyKindMetadata(String kind, Map<String, String> meta) {
        switch (kind) {
            case "tools" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_TOOL);
            case "programming" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.TECH_COMPONENT);
                CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_MISC);
            }
            case "components" -> CompatMetaUtil.addFacet(meta, ItemFacet.TECH_COMPONENT);
            default -> {
            }
        }
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
