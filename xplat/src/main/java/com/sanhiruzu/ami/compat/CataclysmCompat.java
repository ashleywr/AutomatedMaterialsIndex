package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CataclysmCompat {
    private static final String MOD_ID = "cataclysm";

    private CataclysmCompat() {
    }

    public static void enrichItem(Identifier id, Map<String, String> meta) {
        if (id == null || meta == null || !MOD_ID.equals(id.getNamespace())) {
            return;
        }
        meta.putIfAbsent(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, MOD_ID);
        meta.putIfAbsent(SearchNodeKeys.COMPAT_FAMILIES, MOD_ID);
        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addFacts(context, facts);

        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put("cataclysmItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "cataclysm_" + kind);
        }
        applyKindMetadata(kind, context, meta);
        if (!facts.isEmpty()) {
            meta.put("cataclysmFacts", CompatMetaUtil.join(facts));
        }
    }

    private static void addFacts(Context context, Set<String> facts) {
        String path = context.path;
        if (CompatMetaUtil.containsAny(path, "ingot", "nugget")) facts.add("metal_material");
        if (CompatMetaUtil.containsAny(path, "lacrima", "void_core", "dying_ember", "burning_ashes", "monstrous_horn")) facts.add("magic_reagent");
        if (CompatMetaUtil.containsAny(context.itemClass, "DungeonEyeItem") || path.endsWith("_eye")) facts.add("dungeon_eye");
        if (CompatMetaUtil.containsAny(context.itemClass, "Laser", "Shoulder_Weapon", "bow", "Bow")) facts.add("ranged_weapon");
        if (CompatMetaUtil.containsAny(context.itemClass, "Sword", "Spear", "Bardiche", "Athame", "Incinerator", "Annihilator", "Immolator")) facts.add("melee_weapon");
        if (CompatMetaUtil.containsAny(context.itemClass, "Shield", "Targe", "Bulwark", "Gauntlet")) facts.add("defensive_artifact");
        if (CompatMetaUtil.containsAny(context.itemClass, "ItemInventoryOnly")) facts.add("inventory_component");
        if (CompatMetaUtil.containsAny(context.itemClass, "CuriosItem")) facts.add("curio");
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("ranged_weapon")) return "ranged_weapons";
        if (facts.contains("melee_weapon")) return "weapons";
        if (facts.contains("defensive_artifact")) return "defensive_artifacts";
        if (facts.contains("dungeon_eye")) return "dungeon_eyes";
        if (facts.contains("magic_reagent")) return "reagents";
        if (facts.contains("metal_material")) return "materials";
        if (facts.contains("inventory_component")) return "components";
        if (facts.contains("curio")) return "curios";
        return "";
    }

    private static void applyKindMetadata(String kind, Context context, Map<String, String> meta) {
        switch (kind) {
            case "ranged_weapons" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.RANGED_WEAPON);
                route(meta, "weapons");
            }
            case "weapons" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.MELEE_WEAPON);
                route(meta, "weapons");
            }
            case "defensive_artifacts", "dungeon_eyes" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
                route(meta, kind.equals("dungeon_eyes") ? "dungeon_eyes" : "artifacts");
            }
            case "reagents" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_REAGENT);
            case "materials" -> {
                if (context.path.contains("nugget")) CompatMetaUtil.addFacet(meta, ItemFacet.NUGGET);
                else CompatMetaUtil.addFacet(meta, ItemFacet.INGOT);
                CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_MINERAL);
            }
            case "components" -> CompatMetaUtil.addFacet(meta, ItemFacet.TECH_COMPONENT);
            case "curios" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.CURIO);
                route(meta, "artifacts");
            }
            default -> {
            }
        }
    }

    private static void route(Map<String, String> meta, String subcategory) {
        meta.put(SearchNodeKeys.COMPAT_ROUTE_CATEGORY, MOD_ID);
        meta.put(SearchNodeKeys.COMPAT_ROUTE_SUBCATEGORY, subcategory);
    }

    private static final class Context {
        final String path;
        final String itemClass;

        Context(Identifier id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
        }
    }
}
