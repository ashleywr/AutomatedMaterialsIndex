package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class NtglCompat {
    private static final String MOD_ID = "ntgl";

    private NtglCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null || !MOD_ID.equals(id.getNamespace())) {
            return;
        }
        meta.putIfAbsent(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, MOD_ID);
        meta.putIfAbsent(SearchNodeKeys.COMPAT_FAMILIES, MOD_ID);
        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        if (CompatMetaUtil.containsAny(context.itemClass, "WeaponItem")) facts.add("weapon");
        if (CompatMetaUtil.containsAny(context.itemClass, "ScopeItem", "StockItem", "GripItem", "MagazineItem", "BarrelItem", "ChassisItem")) {
            facts.add("attachment");
        }
        if (CompatMetaUtil.containsAny(context.itemClass, "ChassisArmor")) facts.add("power_armor");
        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put("ntglItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "ntgl_" + kind);
        }
        switch (kind) {
            case "weapon" -> CompatMetaUtil.addFacet(meta, ItemFacet.RANGED_WEAPON);
            case "attachment" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.TECH_COMPONENT);
                CompatMetaUtil.addFacet(meta, ItemFacet.UPGRADE);
            }
            case "power_armor" -> {
                meta.put(SearchNodeKeys.COMPAT_ROUTE_CATEGORY, MOD_ID);
                meta.put(SearchNodeKeys.COMPAT_ROUTE_SUBCATEGORY, "power_armor");
            }
            default -> {
            }
        }
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("power_armor")) return "power_armor";
        if (facts.contains("weapon")) return "weapon";
        if (facts.contains("attachment")) return "attachment";
        return "";
    }

    private static final class Context {
        final String itemClass;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT);
        }
    }
}
