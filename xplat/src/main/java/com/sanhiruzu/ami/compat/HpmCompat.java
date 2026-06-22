package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Map;

public final class HpmCompat {
    private static final String MOD_ID = "hpm";

    private HpmCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null || !MOD_ID.equals(id.getNamespace())) {
            return;
        }
        String path = id.getPath().toLowerCase(Locale.ROOT);
        String itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT);
        String kind = "";
        if (path.contains("cannonball") || path.contains("mortar_ball")) kind = "ammo";
        else if (path.contains("mortar")) kind = "weapon";
        else if (path.contains("hull") || path.contains("mast")) kind = "ship_part";
        else if (path.contains("cutter") || path.contains("swashbuckler") || path.contains("corvette") || path.contains("pirate")) kind = "ship_token";
        if (!kind.isBlank()) {
            meta.put("hpmItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "hpm_" + kind);
        }
        switch (kind) {
            case "ammo" -> CompatMetaUtil.addFacet(meta, ItemFacet.PROJECTILE);
            case "weapon" -> CompatMetaUtil.addFacet(meta, ItemFacet.RANGED_WEAPON);
            case "ship_part" -> CompatMetaUtil.addFacet(meta, ItemFacet.TRANSPORT);
            case "ship_token" -> {
                meta.put(SearchNodeKeys.COMPAT_ROUTE_CATEGORY, MOD_ID);
                meta.put(SearchNodeKeys.COMPAT_ROUTE_SUBCATEGORY, "ships");
            }
            default -> {
            }
        }
    }
}
