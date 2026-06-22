package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CncCompat {
    private static final String MOD_ID = "cnc";

    private CncCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null || !MOD_ID.equals(id.getNamespace())) {
            return;
        }
        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        if (CompatMetaUtil.containsAny(context.path, "buckskin", "raw_turkey", "mandible", "antler", "tusk", "wishbone")) facts.add("organic_material");
        if (CompatMetaUtil.containsAny(context.path, "potofmouse", "kill_stick")) facts.add("artifact");
        String kind = facts.contains("artifact") ? "artifact" : facts.contains("organic_material") ? "organic_material" : "";
        if (!kind.isBlank()) {
            meta.put("cncItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "cnc_" + kind);
        }
        switch (kind) {
            case "artifact" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
            case "organic_material" -> CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_ORGANIC);
            default -> {
            }
        }
    }

    private static final class Context {
        final String path;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
        }
    }
}
