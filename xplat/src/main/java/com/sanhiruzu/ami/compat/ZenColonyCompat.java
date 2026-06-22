package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Map;

public final class ZenColonyCompat {
    private static final String MOD_ID = "zen_colony";
    private static final String HOST_CATEGORY = "minecolonies";

    private ZenColonyCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null || !MOD_ID.equals(id.getNamespace())) {
            return;
        }
        meta.putIfAbsent(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, MOD_ID);
        meta.putIfAbsent(SearchNodeKeys.COMPAT_FAMILIES, MOD_ID);
        String path = id.getPath().toLowerCase(Locale.ROOT);
        String tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
        String kind = "";
        if (tags.contains("zen_colony:supply_packs") || path.endsWith("_pack")) kind = "supply_pack";
        else if (path.contains("focus")) kind = "focus";
        else if (path.contains("material")) kind = "material";
        if (!kind.isBlank()) {
            meta.put("zenColonyItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "zen_colony_" + kind);
        }
        switch (kind) {
            case "supply_pack" -> {
                meta.put(SearchNodeKeys.COMPAT_ROUTE_CATEGORY, HOST_CATEGORY);
                meta.put(SearchNodeKeys.COMPAT_ROUTE_SUBCATEGORY, "supply_packs");
                CompatMetaUtil.addFacet(meta, ItemFacet.STORAGE);
            }
            case "focus" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
            case "material" -> CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_MINERAL);
            default -> {
            }
        }
    }
}
