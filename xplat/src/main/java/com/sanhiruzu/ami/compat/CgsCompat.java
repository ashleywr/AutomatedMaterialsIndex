package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CgsCompat {
    private static final String MOD_ID = "cgs";

    private CgsCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null || !MOD_ID.equals(id.getNamespace())) {
            return;
        }
        meta.putIfAbsent(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, MOD_ID);
        meta.putIfAbsent(SearchNodeKeys.COMPAT_FAMILIES, MOD_ID);
        Context context = new Context(meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        if (CompatMetaUtil.containsAny(context.itemClass, "GatlingItem")) facts.add("weapon");
        if (CompatMetaUtil.containsAny(context.itemClass, "ScopeItem", "AttachmentItem", "BarrelItem")) facts.add("attachment");
        String kind = facts.contains("weapon") ? "weapon" : facts.contains("attachment") ? "attachment" : "";
        if (!kind.isBlank()) {
            meta.put("cgsItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "cgs_" + kind);
        }
        switch (kind) {
            case "weapon" -> CompatMetaUtil.addFacet(meta, ItemFacet.RANGED_WEAPON);
            case "attachment" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.TECH_COMPONENT);
                CompatMetaUtil.addFacet(meta, ItemFacet.UPGRADE);
            }
            default -> {
            }
        }
    }

    private static final class Context {
        final String itemClass;

        Context(Map<String, String> meta) {
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT);
        }
    }
}
