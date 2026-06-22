package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class WitcheryCompat {
    private static final String MOD_ID = "witchery";

    private WitcheryCompat() {
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
        addTagFacts(context, facts);
        addPathFacts(context, facts);

        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put("witcheryItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "witchery_" + kind);
        }
        applyKindMetadata(kind, meta);
        if (!facts.isEmpty()) {
            meta.put("witcheryFacts", CompatMetaUtil.join(facts));
        }
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (CompatMetaUtil.containsAny(context.itemClass, "BroomItem")) facts.add("broom_tool");
        if (CompatMetaUtil.containsAny(context.itemClass, "WoodenStakeItem")) facts.add("melee_tool");
        if (CompatMetaUtil.containsAny(context.itemClass, "TarotDeckItem", "TornPageItem", "CovenContractItem")) facts.add("witch_text");
        if (CompatMetaUtil.containsAny(context.itemClass, "PoppetItem", "VoodooPoppetItem", "SeerStoneItem",
                "AttunedStoneItem", "LeonardsUrnItem", "DeathHandItem", "WitchesHandItem", "DebugWand")) {
            facts.add("magic_artifact");
        }
        if (CompatMetaUtil.containsAny(context.itemClass, "MutandisItem", "ParasiticLouseItem")) facts.add("magic_reagent");
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        for (String tag : CompatMetaUtil.splitCsv(context.tags)) {
            if (tag.startsWith("witchery:vines")) facts.add("organic_reagent");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = CompatMetaUtil.basePath(context.path);
        if (path.contains("broom")) facts.add("broom_tool");
        if (path.contains("stake")) facts.add("melee_tool");
        if (CompatMetaUtil.containsAny(path, "poppet", "seer_stone", "attuned_stone", "necromantic_stone",
                "eternal_catalyst", "hand", "urn", "tarot", "contract", "page", "ring")) {
            facts.add("magic_artifact");
        }
        if (CompatMetaUtil.containsAny(path, "dust", "oil", "ointment", "mutandis", "animus", "ghost", "soul",
                "spirit", "breath", "whiff", "fume", "tear", "exhale", "hint", "reek", "odor", "drop", "dew",
                "blood", "hunger", "fear", "will")) {
            facts.add("magic_reagent");
        }
        if (CompatMetaUtil.containsAny(path, "tongue", "toe", "wing", "fabric", "twine", "cruor", "louse")) {
            facts.add("organic_reagent");
        }
        if (path.contains("jar")) facts.add("vessel");
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("broom_tool")) return "broom_tool";
        if (facts.contains("melee_tool")) return "melee_tool";
        if (facts.contains("witch_text")) return "witch_text";
        if (facts.contains("magic_artifact")) return "magic_artifact";
        if (facts.contains("magic_reagent")) return "magic_reagent";
        if (facts.contains("organic_reagent")) return "organic_reagent";
        if (facts.contains("vessel")) return "vessel";
        return "";
    }

    private static void applyKindMetadata(String kind, Map<String, String> meta) {
        switch (kind) {
            case "broom_tool" -> CompatMetaUtil.addFacet(meta, ItemFacet.TRANSPORT);
            case "melee_tool" -> CompatMetaUtil.addFacet(meta, ItemFacet.MELEE_WEAPON);
            case "witch_text" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.BOOK);
                CompatMetaUtil.addFacet(meta, ItemFacet.GUIDE_BOOK);
            }
            case "magic_artifact" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
            case "magic_reagent" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_REAGENT);
            case "organic_reagent" -> CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_ORGANIC);
            case "vessel" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_MISC);
            default -> {
            }
        }
    }

    private static final class Context {
        final String path;
        final String itemClass;
        final String tags;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT);
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
        }
    }
}
