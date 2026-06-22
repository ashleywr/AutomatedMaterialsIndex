package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MowziesMobsCompat {
    private static final String MOD_ID = "mowziesmobs";

    private MowziesMobsCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null || !MOD_ID.equals(id.getNamespace())) {
            return;
        }
        meta.putIfAbsent(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, MOD_ID);
        meta.putIfAbsent(SearchNodeKeys.COMPAT_FAMILIES, MOD_ID);
        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        if (CompatMetaUtil.containsAny(context.itemClass, "ItemDart")) facts.add("ammo");
        if (CompatMetaUtil.containsAny(context.itemClass, "ItemBluffRod", "ItemSandRake", "ItemMobRemover")) facts.add("tool");
        if (CompatMetaUtil.containsAny(context.itemClass, "ItemElokosaPaw", "ItemGrantSunsBlessing", "ItemCapturedGrottol")) facts.add("artifact");
        if (CompatMetaUtil.containsAny(context.itemClass, "ItemNagaFang")) facts.add("organic_material");
        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put("mowziesMobsItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "mowziesmobs_" + kind);
        }
        switch (kind) {
            case "ammo" -> CompatMetaUtil.addFacet(meta, ItemFacet.PROJECTILE);
            case "tool" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_TOOL);
            case "artifact" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
            case "organic_material" -> CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_ORGANIC);
            default -> {
            }
        }
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("ammo")) return "ammo";
        if (facts.contains("tool")) return "tool";
        if (facts.contains("artifact")) return "artifact";
        if (facts.contains("organic_material")) return "organic_material";
        return "";
    }

    private static final class Context {
        final String itemClass;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT);
        }
    }
}
