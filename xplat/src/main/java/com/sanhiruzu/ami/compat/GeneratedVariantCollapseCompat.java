package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class GeneratedVariantCollapseCompat {
    private GeneratedVariantCollapseCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null) {
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

        Optional<String> generatedBase = generatedVariantBase(id, meta);
        if (generatedBase.isEmpty()) {
            return;
        }

        String base = generatedBase.get();
        String basePath = base.contains(":") ? base.substring(base.indexOf(':') + 1) : base;
        meta.put(SearchNodeKeys.COLLAPSE_FAMILY, base);
        meta.put(SearchNodeKeys.COLLAPSE_LABEL, CompatMetaUtil.title(CompatMetaUtil.basePath(basePath)));
        meta.put(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
        CompatMetaUtil.addSearchToken(meta, "generated_variant");
    }

    private static Optional<String> generatedVariantBase(ResourceLocation id, Map<String, String> meta) {
        String path = id.getPath();
        int variant = path.indexOf("/variant/");
        if (variant >= 0) {
            String base = meta.getOrDefault(SearchNodeKeys.SUBTYPE_OF, "");
            if (base.isBlank()) {
                base = id.getNamespace() + ":" + path.substring(0, variant);
            }
            return Optional.of(base);
        }

        String normalized = path.toLowerCase(Locale.ROOT);
        String basePath = stripVisualStateSuffix(normalized);
        if (basePath.equals(normalized) || basePath.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(id.getNamespace() + ":" + basePath);
    }

    private static String stripVisualStateSuffix(String path) {
        String result = path;
        result = result.replaceFirst("_pulling_[0-9]+_inventory$", "");
        result = result.replaceFirst("_charged_[0-9]+_inventory$", "");
        result = result.replaceFirst("_charge_[0-9]+_inventory$", "");
        result = result.replaceFirst("_inventory$", "");
        result = result.replaceFirst("_empty_hand$", "");
        result = result.replaceFirst("_empty_inventory$", "");
        result = result.replaceFirst("_hand$", "");
        return result;
    }
}
