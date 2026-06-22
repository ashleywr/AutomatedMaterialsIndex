package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class EnigmaticLegacyPlusCompat {
    private static final String MOD_ID = "enigmaticlegacyplus";

    private EnigmaticLegacyPlusCompat() {
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
        addPathFacts(context, facts);

        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put("enigmaticLegacyPlusItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "enigmaticlegacyplus_" + kind);
        }
        applyKindMetadata(kind, meta);
        if (!facts.isEmpty()) {
            meta.put("enigmaticLegacyPlusFacts", CompatMetaUtil.join(facts));
        }
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (CompatMetaUtil.containsAny(context.itemClass, "AntiqueBag")) facts.add("storage");
        if (CompatMetaUtil.containsAny(context.itemClass, "MendingMixture", "LoreInscriber", "BlessStone", "CursedStone",
                "EarthHeart", "ExtradimensionalEye", "TwistedHeart", "TwistedMirror", "VoidStone", "GuardianHeart", "PureHeart",
                "AbyssalHeart")) {
            facts.add("artifact");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = CompatMetaUtil.basePath(context.path);
        if (path.contains("ring")) facts.add("accessory");
        if (CompatMetaUtil.containsAny(path, "mixture", "droplet", "fragment", "glass")) facts.add("reagent");
        if (CompatMetaUtil.containsAny(path, "heart", "eye", "stone", "mirror", "inscriber", "bless")) facts.add("artifact");
        if (path.contains("bag")) facts.add("storage");
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("storage")) return "storage";
        if (facts.contains("accessory")) return "accessory";
        if (facts.contains("artifact")) return "artifact";
        if (facts.contains("reagent")) return "reagent";
        return "";
    }

    private static void applyKindMetadata(String kind, Map<String, String> meta) {
        switch (kind) {
            case "storage" -> CompatMetaUtil.addFacet(meta, ItemFacet.STORAGE);
            case "accessory" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.CURIO);
                CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
            }
            case "artifact" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
            case "reagent" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_REAGENT);
            default -> {
            }
        }
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
