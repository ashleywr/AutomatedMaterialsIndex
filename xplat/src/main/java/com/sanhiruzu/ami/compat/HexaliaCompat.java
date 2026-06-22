package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HexaliaCompat {
    private static final String MOD_ID = "hexalia";

    private HexaliaCompat() {
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
            meta.put("hexaliaItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "hexalia_" + kind);
        }
        applyKindMetadata(kind, meta);
        if (!facts.isEmpty()) {
            meta.put("hexaliaFacts", CompatMetaUtil.join(facts));
        }
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (CompatMetaUtil.containsAny(context.itemClass, "HexFocusItem", "WeatherIdolItem", "PurityIdolItem")) facts.add("magic_artifact");
        if (CompatMetaUtil.containsAny(context.itemClass, "AthameItem")) facts.add("melee_tool");
        if (CompatMetaUtil.containsAny(context.itemClass, "ThrownSacItem")) facts.add("projectile_sac");
        if (CompatMetaUtil.containsAny(context.itemClass, "PurifyingSacItem", "SpiritrootTetherItem")) facts.add("utility_tool");
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        for (String tag : CompatMetaUtil.splitCsv(context.tags)) {
            if (tag.startsWith("hexalia:crushed_herbs")) facts.add("magic_reagent");
            if (tag.startsWith("hexalia:herbs")) facts.add("organic_reagent");
            if (tag.startsWith("hexalia:offhand_equipment")) facts.add("magic_artifact");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = CompatMetaUtil.basePath(context.path);
        if (CompatMetaUtil.containsAny(path, "idol", "focus", "pendant")) facts.add("magic_artifact");
        if (CompatMetaUtil.containsAny(path, "powder", "paste", "node")) facts.add("magic_reagent");
        if (path.contains("resin")) facts.add("organic_reagent");
        if (path.contains("sac")) facts.add("projectile_sac");
        if (CompatMetaUtil.containsAny(path, "tether", "purifying")) facts.add("utility_tool");
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("magic_artifact")) return "magic_artifact";
        if (facts.contains("projectile_sac")) return "projectile_sac";
        if (facts.contains("melee_tool")) return "melee_tool";
        if (facts.contains("utility_tool")) return "utility_tool";
        if (facts.contains("magic_reagent")) return "magic_reagent";
        if (facts.contains("organic_reagent")) return "organic_reagent";
        return "";
    }

    private static void applyKindMetadata(String kind, Map<String, String> meta) {
        switch (kind) {
            case "magic_artifact" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
            case "projectile_sac" -> CompatMetaUtil.addFacet(meta, ItemFacet.PROJECTILE);
            case "melee_tool" -> CompatMetaUtil.addFacet(meta, ItemFacet.MELEE_WEAPON);
            case "utility_tool" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_TOOL);
            case "magic_reagent" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_REAGENT);
            case "organic_reagent" -> CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_ORGANIC);
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
