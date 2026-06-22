package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class McTradePostCompat {
    private static final String MOD_ID = "mctradepost";

    private McTradePostCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null || !MOD_ID.equals(id.getNamespace())) {
            return;
        }
        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        if (CompatMetaUtil.containsAny(context.itemClass, "AdvancedClipboardItem", "CurrencyExchangeItem", "OutpostClaimMarkerItem")) facts.add("utility_tool");
        if (CompatMetaUtil.containsAny(context.itemClass, "SouvenirItem")) facts.add("souvenir");
        if (CompatMetaUtil.containsAny(context.path, "wish_")) facts.add("wish");
        if (CompatMetaUtil.containsAny(context.path, "napkin", "mortar")) facts.add("material");
        if (context.path.contains("copper_nugget")) facts.add("currency");
        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put("mcTradePostItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "mctradepost_" + kind);
        }
        switch (kind) {
            case "utility_tool" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_TOOL);
            case "wish" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
            case "material" -> CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_MINERAL);
            case "currency" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_CURRENCY);
            case "souvenir" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_MISC);
            default -> {
            }
        }
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("utility_tool")) return "utility_tool";
        if (facts.contains("wish")) return "wish";
        if (facts.contains("currency")) return "currency";
        if (facts.contains("material")) return "material";
        if (facts.contains("souvenir")) return "souvenir";
        return "";
    }

    private static final class Context {
        final String path;
        final String itemClass;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT);
        }
    }
}
