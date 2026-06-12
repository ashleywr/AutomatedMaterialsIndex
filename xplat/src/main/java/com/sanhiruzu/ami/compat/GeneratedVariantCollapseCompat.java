package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public final class GeneratedVariantCollapseCompat {
    private GeneratedVariantCollapseCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }
        String path = id.getPath();
        int variant = path.indexOf("/variant/");
        if (variant < 0) {
            return;
        }
        if (!meta.getOrDefault(SearchNodeKeys.COLLAPSE_FAMILY, "").isBlank()) {
            return;
        }
        if ("never".equals(meta.get(SearchNodeKeys.VARIANT_COLLAPSE_MODE))) {
            return;
        }
        String facets = meta.getOrDefault(SearchNodeKeys.FACETS, "");
        if ("true".equals(meta.get(SearchNodeKeys.GUIDE_BOOK_CANDIDATE))
                || CompatMetaUtil.hasToken(facets, "guide_book")) {
            return;
        }

        String base = meta.getOrDefault(SearchNodeKeys.SUBTYPE_OF, "");
        if (base.isBlank()) {
            base = id.getNamespace() + ":" + path.substring(0, variant);
        }
        String basePath = base.contains(":") ? base.substring(base.indexOf(':') + 1) : base;
        meta.put(SearchNodeKeys.COLLAPSE_FAMILY, base);
        meta.put(SearchNodeKeys.COLLAPSE_LABEL, CompatMetaUtil.title(CompatMetaUtil.basePath(basePath)));
        meta.put(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
        CompatMetaUtil.addSearchToken(meta, "generated_variant");
    }
}
