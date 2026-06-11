package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class BornInChaosCompat {
    private static final String MOD_ID = "born_in_chaos_v1";

    private BornInChaosCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null || !MOD_ID.equals(id.getNamespace())) {
            return;
        }
        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addFacts(context, facts);

        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put("bornInChaosItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "born_in_chaos_" + kind);
        }
        applyKindMetadata(kind, context, meta);
        if (!facts.isEmpty()) {
            meta.put("bornInChaosFacts", CompatMetaUtil.join(facts));
        }
    }

    private static void addFacts(Context context, Set<String> facts) {
        String path = context.path;
        if (CompatMetaUtil.containsAny(path, "spawn_structure", "spawn_structures")) facts.add("structure_placer");
        if (CompatMetaUtil.containsAny(path, "charmof", "charm_", "totem", "icon", "orb")) facts.add("magic_artifact");
        if (CompatMetaUtil.containsAny(path, "ingot", "nugget", "dark_metal", "armor_plate")) facts.add("metal_material");
        if (CompatMetaUtil.containsAny(path, "claw", "skin", "flesh", "stomach", "fang", "bone", "horn")) facts.add("organic_drop");
        if (CompatMetaUtil.containsAny(path, "dust", "spirit", "soul", "seedof_chaos")) facts.add("magic_reagent");
        if (CompatMetaUtil.containsAny(path, "bomb", "dark_charge")) facts.add("projectile");
        if (CompatMetaUtil.containsAny(context.itemClass, "Sword", "Dagger", "Scythe", "Axe", "Saber", "Crusher")) facts.add("melee_weapon");
        if (CompatMetaUtil.containsAny(context.itemClass, "ArmorItem", "Helmet", "Chestplate", "Leggings", "Boots")) facts.add("armor");
        if (CompatMetaUtil.containsAny(context.itemClass, "Elixir", "Decoction", "Bottle")) facts.add("drink");
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("melee_weapon")) return "weapons";
        if (facts.contains("armor")) return "armor";
        if (facts.contains("projectile")) return "projectiles";
        if (facts.contains("structure_placer")) return "structure_placers";
        if (facts.contains("magic_artifact")) return "artifacts";
        if (facts.contains("magic_reagent")) return "reagents";
        if (facts.contains("metal_material")) return "materials";
        if (facts.contains("organic_drop")) return "organic_drops";
        if (facts.contains("drink")) return "drinks";
        return "";
    }

    private static void applyKindMetadata(String kind, Context context, Map<String, String> meta) {
        switch (kind) {
            case "weapons" -> CompatMetaUtil.addFacet(meta, ItemFacet.MELEE_WEAPON);
            case "armor" -> CompatMetaUtil.addFacet(meta, ItemFacet.EQUIPPABLE);
            case "projectiles" -> CompatMetaUtil.addFacet(meta, ItemFacet.PROJECTILE);
            case "structure_placers" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_MISC);
            case "artifacts" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_ARTIFACT);
            case "reagents" -> CompatMetaUtil.addFacet(meta, ItemFacet.MAGIC_REAGENT);
            case "materials" -> {
                if (context.path.contains("ingot")) CompatMetaUtil.addFacet(meta, ItemFacet.INGOT);
                else if (context.path.contains("nugget")) CompatMetaUtil.addFacet(meta, ItemFacet.NUGGET);
                CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_MINERAL);
            }
            case "organic_drops" -> CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_ORGANIC);
            case "drinks" -> {
                CompatMetaUtil.addFacet(meta, ItemFacet.EDIBLE);
                CompatMetaUtil.addFacet(meta, ItemFacet.FOOD_DRINK);
            }
            default -> {
            }
        }
    }

    private static final class Context {
        final String path;
        final String itemClass;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
        }
    }
}
